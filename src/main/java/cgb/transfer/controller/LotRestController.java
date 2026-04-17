package cgb.transfer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cgb.transfer.dto.LotRequest;
import cgb.transfer.dto.LotResponse;
import cgb.transfer.entity.Lot;
import cgb.transfer.entity.TransferLot;
import cgb.transfer.service.LotService;
import cgb.transfer.service.ReportService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lots")
public class LotRestController {

    @Autowired
    private LotService lotService;

    @Autowired
    private ReportService reportService;

    @PostMapping
    public ResponseEntity<LotResponse> submitLot(@RequestBody LotRequest lotRequest) {
        LotResponse response = lotService.submitLot(lotRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLot(@PathVariable Long id) {
        Lot lot = lotService.getLotById(id);
        if (lot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lot introuvable: " + id);
        }
        return ResponseEntity.ok(lot);
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<?> getLotReport(@PathVariable Long id) {
        Map<String, Object> report = reportService.generateLotReport(id);
        if (report == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lot introuvable: " + id);
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping("/{id}/failures")
    public ResponseEntity<List<TransferLot>> getFailedTransfersByLot(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getFailedTransfersByLot(id));
    }

    @GetMapping("/failures")
    public ResponseEntity<List<TransferLot>> getFailedTransfersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getFailedTransfersByDateRange(from, to));
    }

    @GetMapping("/failures/account/{destAccount}")
    public ResponseEntity<List<TransferLot>> getFailedTransfersByDestAccount(@PathVariable String destAccount) {
        return ResponseEntity.ok(reportService.getFailedTransfersByDestAccount(destAccount));
    }
}
