package cgb.transfer.service;

import cgb.transfer.dto.LotItemRequest;
import cgb.transfer.entity.Account;
import cgb.transfer.entity.Lot;
import cgb.transfer.entity.Transfer;
import cgb.transfer.entity.TransferStatus;
import cgb.transfer.exception.TransferException;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.LotRepository;
import cgb.transfer.repository.TransferRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.util.List;

@Service
public class LotService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private LotRepository lotRepository;

    /**
     * Crée un lot en base avec le statut RECEIVED et le retourne immédiatement.
     */
    @Transactional
    public Lot createLot(String refLot, String descriptionLot) {
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
        accountRepository.findById(sourceAccountNumber).orElseThrow();
        for (LotItemRequest item : items) {
            Transfer transfer = transferService.createTransferForLot(
                    sourceAccountNumber,
                    item.getDestAccount(),
                    item.getAmount(),
                    item.getDescription(),
                    lotId
            );
        }

        // Mise à jour du statut du lot
        Lot lot = lotRepository.findById(lotId).orElseThrow();
        lot.setEtat(TransferStatus.CLOSED);
        lotRepository.save(lot);
    }
}
