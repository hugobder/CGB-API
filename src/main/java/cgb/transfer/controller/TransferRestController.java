package cgb.transfer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cgb.transfer.dto.TransferRequest;
import cgb.transfer.entity.Transfer;
import cgb.transfer.service.TransferService;
import cgb.transfer.exception.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/transfers")
public class TransferRestController {

    @Autowired
    private TransferService transferService;


    

    @PostMapping
    public ResponseEntity<?> createTransfer(@RequestBody TransferRequest transferRequest) {
    //public ResponseEntity<Transfer> createTransfer(@RequestBody TransferRequest transferRequest) {
        try {
    	Transfer transfer = transferService.createTransfer(
                transferRequest.getSourceAccountNumber(),
                transferRequest.getDestinationAccountNumber(),
                transferRequest.getAmount(),
                transferRequest.getTransferDate(),
                transferRequest.getDescription()
        );
    	return ResponseEntity.ok(transfer);
        }catch (RuntimeException e) {
            TransferResponse errorResponse = new TransferResponse("FAILURE", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }        
    }  
    
    @DeleteMapping
    public ResponseEntity<?> deleteTransfer(@RequestBody Long id) {
    //public ResponseEntity<Transfer> createTransfer(@RequestBody TransferRequest transferRequest) {
        try {
    	Transfer t=transferService.deleteTransfer(id);
        System.out.println(t);
        TransferResponse succesResponse = new TransferResponse("SUCCESS", t.toString());
    	return ResponseEntity.ok(succesResponse);
    	
        }catch (RuntimeException | DeleteTransferException e) {
            TransferResponse errorResponse = new TransferResponse("FAILURE", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }        
    }  
    

    /*
    @PostMapping
    public ResponseEntity<String> testTransfer(@RequestBody String s) {
    	System.out.println("Post reçu");
        return ResponseEntity.ok("Post bien traité: "+ s);
    } 
    */
    
}


class TransferResponse {
    private String status;
    private String message;

    // Constructeur
    public TransferResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    // Getters et Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
