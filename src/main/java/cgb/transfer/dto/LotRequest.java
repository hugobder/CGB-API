package cgb.transfer.dto;

import java.util.List;

public class LotRequest {
    private String refLot;
    private String sourceAccount;
    private String descriptionLot;
    private List<VirementRequest> virements;

    public String getRefLot() { return refLot; }
    public void setRefLot(String refLot) { this.refLot = refLot; }
    public String getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; }
    public String getDescriptionLot() { return descriptionLot; }
    public void setDescriptionLot(String descriptionLot) { this.descriptionLot = descriptionLot; }
    public List<VirementRequest> getVirements() { return virements; }
    public void setVirements(List<VirementRequest> virements) { this.virements = virements; }
}
