package cgb.transfer.dto;

import java.time.LocalDate;

public class LotResponse {

    private Long numLot;
    private LocalDate dateLancement;
    private String message;
    private String etat;

    public LotResponse(Long numLot, LocalDate dateLancement, String message, String etat) {
        this.numLot = numLot;
        this.dateLancement = dateLancement;
        this.message = message;
        this.etat = etat;
    }

    public Long getNumLot() { return numLot; }
    public LocalDate getDateLancement() { return dateLancement; }
    public String getMessage() { return message; }
    public String getEtat() { return etat; }
}
