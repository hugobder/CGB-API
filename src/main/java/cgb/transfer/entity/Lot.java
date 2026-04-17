package cgb.transfer.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Lot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String refLot;
    private String sourceAccount;
    private String descriptionLot;
    private LocalDate dateLot;
    private String state;

    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<TransferLot> virements = new ArrayList<>();

    public Lot() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRefLot() { return refLot; }
    public void setRefLot(String refLot) { this.refLot = refLot; }
    public String getSourceAccount() { return sourceAccount; }
    public void setSourceAccount(String sourceAccount) { this.sourceAccount = sourceAccount; }
    public String getDescriptionLot() { return descriptionLot; }
    public void setDescriptionLot(String descriptionLot) { this.descriptionLot = descriptionLot; }
    public LocalDate getDateLot() { return dateLot; }
    public void setDateLot(LocalDate dateLot) { this.dateLot = dateLot; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public List<TransferLot> getVirements() { return virements; }
    public void setVirements(List<TransferLot> virements) { this.virements = virements; }
}
