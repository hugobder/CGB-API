package cgb.transfer.dto;

public class LotItemRequest {

    private Double amount;
    private String destAccount;
    private String description;

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDestAccount() { return destAccount; }
    public void setDestAccount(String destAccount) { this.destAccount = destAccount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
