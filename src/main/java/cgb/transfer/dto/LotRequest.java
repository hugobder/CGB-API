package cgb.transfer.dto;

import java.util.List;

public class LotRequest {

    private String sourceAccountNumber;
    private List<LotItemRequest> transfers;

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public List<LotItemRequest> getTransfers() { return transfers; }
    public void setTransfers(List<LotItemRequest> transfers) { this.transfers = transfers; }
}
