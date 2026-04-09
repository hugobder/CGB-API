package cgb.transfer.service;

import cgb.transfer.dto.LotItemRequest;
import cgb.transfer.entity.Lot;
import cgb.transfer.entity.Transfer;
import cgb.transfer.entity.TransferStatus;
import cgb.transfer.exception.DeleteTransferException;
import cgb.transfer.exception.DeleteTransferException.FailureTransfert;
import cgb.transfer.exception.TransferException;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.LotRepository;
import cgb.transfer.repository.TransferRepository;
import cgb.transfer.entity.Account;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LotRepository lotRepository;


    /*
     * Rappel du cours sur les transactions... Tout ou rien
     */
    @Transactional
    public Transfer createTransfer(String sourceAccountNumber, String destinationAccountNumber,
                                   Double amount, LocalDate transferDate, String description) throws TransferException {
        Account sourceAccount = accountRepository.findById(sourceAccountNumber)
                				.orElseThrow(() -> new TransferException("Source account not found"));
        Account destinationAccount = accountRepository.findById(destinationAccountNumber)
                				.orElseThrow(() -> new TransferException("Destination account not found"));

        /*Pas de découvert autorisé*/
        if (sourceAccount.getSolde().compareTo(amount) < 0)  throw new TransferException("Insufficient funds");

        /*Pas de virement négatif autorisé*/
        if (amount < 0 ) throw new TransferException("Negative transfer forbidden");

        /*Pas de virement antidaté*/
        if (transferDate.isBefore(LocalDate.now())) throw new TransferException("Backdated transfer forbidden");

        sourceAccount.setSolde(sourceAccount.getSolde()-(amount));
        destinationAccount.setSolde(destinationAccount.getSolde()+(amount));

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        Transfer transfer = new Transfer();
        transfer.setSourceAccountNumber(sourceAccountNumber);
        transfer.setDestinationAccountNumber(destinationAccountNumber);
        transfer.setAmount(amount);
        transfer.setTransferDate(transferDate);
        transfer.setDescription(description);
        transfer.setStatus(TransferStatus.SUCCESS);

        return transferRepository.save(transfer);

    }

    /**
     * Crée un lot en base avec le statut WAITING et le retourne immédiatement.
     */
    @Transactional
    public Lot  createLot(String refLot, String descriptionLot) {
        Lot lot = new Lot();
        lot.setDateLancement(LocalDate.now());
        lot.setRefLot(refLot);
        lot.setDescriptionLot(descriptionLot);
        lot.setEtat(TransferStatus.RECEIVED);
        return lotRepository.save(lot);
    }

    /**
     * Traitement asynchrone du lot : chaque virement est indépendant.
     * Les virements en échec sont sauvegardés avec le statut FAILURE pour rejeu ultérieur.
     */
    @Async
    public void processLotAsync(Long lotId, String sourceAccountNumber, List<LotItemRequest> items) {
        int successCount = 0;
        int failureCount = 0;

        accountRepository.findById(sourceAccountNumber).orElseThrow();
        // TODO: replace the call of the transactional method
        for (LotItemRequest item : items) {
            Transfer transfer = createTransferForLot(
                    sourceAccountNumber,
                    item.getDestAccount(),
                    item.getAmount(),
                    item.getDescription(),
                    lotId
            );
            if (transfer.getStatus() == TransferStatus.SUCCESS) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        // Mise à jour du statut du lot
        Lot lot = lotRepository.findById(lotId).orElseThrow();
        lot.setEtat(TransferStatus.CLOSED);
        lotRepository.save(lot);
    }

    /**
     * Traite un virement dans sa propre transaction (REQUIRES_NEW).
     * En cas d'échec, sauvegarde le virement avec le statut FAILURE sans modifier les soldes.
     */
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transfer createTransferForLot(String sourceAccountNumber, String destinationAccountNumber,
                                         Double amount, String description, Long lotId) {
        Transfer transfer = new Transfer();
        transfer.setSourceAccountNumber(sourceAccountNumber);
        transfer.setDestinationAccountNumber(destinationAccountNumber);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        transfer.setTransferDate(LocalDate.now());
        transfer.setLotId(lotId);
        transfer.setStatus(TransferStatus.FAILURE); // défaut : echec

        try {
            if (amount == null || amount < 0) throw new TransferException("Negative or null amount forbidden");

            Account sourceAccount = accountRepository.findById(sourceAccountNumber)
                    .orElseThrow(() -> new TransferException("Source account not found"));
            Account destinationAccount = accountRepository.findById(destinationAccountNumber)
                    .orElseThrow(() -> new TransferException("Destination account not found"));

            if (sourceAccount.getSolde().compareTo(amount) < 0)
                throw new TransferException("Insufficient funds");

            sourceAccount.setSolde(sourceAccount.getSolde() - amount);
            destinationAccount.setSolde(destinationAccount.getSolde() + amount);
            accountRepository.save(sourceAccount);
            accountRepository.save(destinationAccount);

            transfer.setStatus(TransferStatus.SUCCESS);
        } catch (TransferException e) {
            // Statut reste FAILURE, aucune modification des soldes
        }

        return transferRepository.save(transfer);
    }

    @Transactional
    public Transfer deleteTransfer(Long id) throws DeleteTransferException {
    	Optional<Transfer> otranfer=transferRepository.findById(id);
    	transferRepository.deleteById(id);
    	if (otranfer.isEmpty())throw new DeleteTransferException(FailureTransfert.OBJECT_NOT_FOUND);
    	return otranfer.orElse(null);
    }
}
