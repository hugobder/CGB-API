package cgb.transfer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cgb.transfer.dto.LotRequest;
import cgb.transfer.dto.LotResponse;
import cgb.transfer.entity.Lot;
import cgb.transfer.service.LotService;

@RestController
@RequestMapping("/api/lots")
public class LotRestController {

    @Autowired
    private LotService lotService;

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
}
