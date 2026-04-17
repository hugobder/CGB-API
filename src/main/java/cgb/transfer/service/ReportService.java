package cgb.transfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cgb.transfer.entity.Lot;
import cgb.transfer.entity.TransferLot;
import cgb.transfer.repository.LotRepository;
import cgb.transfer.repository.TransferLotRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private TransferLotRepository transferLotRepository;

    private static final List<String> FAILURE_STATES = List.of("failure", "delayed", "canceled");

    public Map<String, Object> generateLotReport(Long lotId) {
        Lot lot = lotRepository.findById(lotId).orElse(null);
        if (lot == null) return null;

        List<TransferLot> virements = lot.getVirements();
        long successCount = virements.stream().filter(v -> "success".equals(v.getState())).count();
        long failureCount = virements.stream().filter(v -> "failure".equals(v.getState())).count();
        long delayedCount = virements.stream().filter(v -> "delayed".equals(v.getState())).count();
        long canceledCount = virements.stream().filter(v -> "canceled".equals(v.getState())).count();
        long waitingCount = virements.stream().filter(v -> "waiting".equals(v.getState())).count();

        Map<String, Object> report = new HashMap<>();
        report.put("lotId", lot.getId());
        report.put("refLot", lot.getRefLot());
        report.put("dateLot", lot.getDateLot());
        report.put("state", lot.getState());
        report.put("totalVirements", virements.size());
        report.put("success", successCount);
        report.put("failure", failureCount);
        report.put("delayed", delayedCount);
        report.put("canceled", canceledCount);
        report.put("waiting", waitingCount);
        report.put("virements", virements);
        return report;
    }

    public List<TransferLot> getFailedTransfersByLot(Long lotId) {
        return transferLotRepository.findByLotIdAndStateIn(lotId, FAILURE_STATES);
    }

    public List<TransferLot> getFailedTransfersByDateRange(LocalDate from, LocalDate to) {
        return transferLotRepository.findByCompletionDateBetweenAndStateIn(from, to, FAILURE_STATES);
    }

    public List<TransferLot> getFailedTransfersByDestAccount(String destAccount) {
        return transferLotRepository.findByDestAccountAndStateIn(destAccount, FAILURE_STATES);
    }
}
