package cgb.transfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import cgb.transfer.dto.LotRequest;
import cgb.transfer.dto.LotResponse;
import cgb.transfer.dto.VirementRequest;
import cgb.transfer.entity.Account;
import cgb.transfer.entity.Lot;
import cgb.transfer.entity.TransferLot;
import cgb.transfer.repository.AccountRepository;
import cgb.transfer.repository.LotRepository;
import cgb.transfer.repository.TransferLotRepository;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LotService {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private TransferLotRepository transferLotRepository;

    @Autowired
    private AccountRepository accountRepository;

    public LotResponse submitLot(LotRequest request) {
        Lot lot = new Lot();
        lot.setRefLot(request.getRefLot());
        lot.setSourceAccount(request.getSourceAccount());
        lot.setDescriptionLot(request.getDescriptionLot());
        lot.setDateLot(LocalDate.now());
        lot.setState("received");

        lot = lotRepository.save(lot);

        for (VirementRequest vr : request.getVirements()) {
            TransferLot tl = new TransferLot();
            tl.setDestAccount(vr.getDestAccount());
            tl.setAmount(vr.getAmount());
            tl.setDescription(vr.getDescription());
            tl.setState("waiting");
            tl.setLot(lot);
            transferLotRepository.save(tl);
        }

        processLot(lot.getId());

        return new LotResponse(lot.getId(), lot.getDateLot(), "Traitement Lance", "received");
    }

    @Async
    public void processLot(Long lotId) {
        Lot lot = lotRepository.findById(lotId).orElse(null);
        if (lot == null || !"received".equals(lot.getState())) return;

        List<TransferLot> virements = transferLotRepository.findByLotIdAndStateIn(lotId, List.of("waiting"));

        Optional<Account> sourceOpt = accountRepository.findById(lot.getSourceAccount());
        if (sourceOpt.isEmpty()) {
            for (TransferLot vl : virements) {
                vl.setState("failure");
                vl.setCompletionDate(LocalDate.now());
                transferLotRepository.save(vl);
            }
            lot.setState("closed");
            lotRepository.save(lot);
            return;
        }

        Account sourceAccount = sourceOpt.get();

        for (TransferLot vl : virements) {
            processVirement(vl, sourceAccount);
        }

        lot.setState("closed");
        lotRepository.save(lot);
    }

    @Transactional
    private void processVirement(TransferLot vl, Account sourceAccount) {
        Optional<Account> destOpt = accountRepository.findById(vl.getDestAccount());
        if (destOpt.isEmpty()) {
            vl.setState("failure");
            vl.setCompletionDate(LocalDate.now());
            transferLotRepository.save(vl);
            return;
        }

        if (sourceAccount.getSolde().compareTo(vl.getAmount()) < 0) {
            vl.setState("delayed");
            vl.setCompletionDate(LocalDate.now());
            transferLotRepository.save(vl);
            return;
        }

        Account destAccount = destOpt.get();
        sourceAccount.setSolde(sourceAccount.getSolde() - vl.getAmount());
        destAccount.setSolde(destAccount.getSolde() + vl.getAmount());
        accountRepository.save(sourceAccount);
        accountRepository.save(destAccount);

        vl.setState("success");
        vl.setCompletionDate(LocalDate.now());
        transferLotRepository.save(vl);
    }

    public Lot getLotById(Long id) {
        return lotRepository.findById(id).orElse(null);
    }
}
