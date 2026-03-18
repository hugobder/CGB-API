package cgb.transfer.dto;

import java.time.LocalDate;

public class LotResponse {

    private final Long numLot;
    private final LocalDate dateLancement;
    private final String message;
    private final String etat;

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