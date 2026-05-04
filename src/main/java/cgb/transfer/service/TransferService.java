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

    @Transactional
    public Transfer deleteTransfer(Long id) throws DeleteTransferException {
    	Optional<Transfer> otranfer=transferRepository.findById(id);
    	transferRepository.deleteById(id);
    	if (otranfer.isEmpty())throw new DeleteTransferException(FailureTransfert.OBJECT_NOT_FOUND);
    	return otranfer.orElse(null);
    }

    /**
     * Traite un virement dans sa propre transaction.
     * En cas d'échec, sauvegarde le virement avec le statut FAILURE/DELAYED sans modifier les soldes.
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
        transfer.setStatus(TransferStatus.WAITING);

        try {
            if (amount == null || amount < 0) throw new TransferException("Negative or null amount forbidden");

            Account sourceAccount = accountRepository.findById(sourceAccountNumber).orElseThrow(
                    () -> {
                        transfer.setStatus(TransferStatus.FAILURE);
                        return new TransferException("Source account not found");
                    }
            );

            Account destinationAccount = accountRepository.findById(destinationAccountNumber).orElseThrow(
                    () -> {
                        transfer.setStatus(TransferStatus.FAILURE);
                        return new TransferException("Destination account not found");
                    }
            );

            if (sourceAccount.getSolde().compareTo(amount) < 0) {
                transfer.setStatus(TransferStatus.DELAYED); // fonds insuffisants, à rejouer ultérieurement
                throw new TransferException("Insufficient funds");
            }

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
}
