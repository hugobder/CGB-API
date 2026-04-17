package cgb.transfer.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class TransferLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String destAccount;
    private Double amount;
    private String description;
    private LocalDate completionDate;
    private String state;

    @ManyToOne
    @JoinColumn(name = "lot_id")
    @JsonIgnore
    private Lot lot;

    public TransferLot() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDestAccount() { return destAccount; }
    public void setDestAccount(String destAccount) { this.destAccount = destAccount; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Lot getLot() { return lot; }
    public void setLot(Lot lot) { this.lot = lot; }
}
