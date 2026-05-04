package cgb.transfer.controller;

import cgb.transfer.dto.LotRequest;
import cgb.transfer.dto.LotResponse;
import cgb.transfer.entity.Lot;
import cgb.transfer.service.LotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lots")
public class LotRestController {

    @Autowired
    private LotService lotService;

    @PostMapping("/lot")
    public ResponseEntity<?> createTransferLots(@RequestBody LotRequest lotRequest) {
        Lot lot = lotService.createLot(lotRequest.getRefLot(), lotRequest.getDescriptionLot());
        lotService.processLotAsync(lot.getId(), lotRequest.getSourceAccount(), lotRequest.getVirements());
        LotResponse response = new LotResponse(lot.getId(), lot.getDateLancement(), "Traitement Lancé", lot.getEtat().name().toLowerCase());
        return ResponseEntity.ok(response);
    }
}
