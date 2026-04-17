package cgb.transfer.exception;

public class NegativeAmountException extends TransferException {
    public NegativeAmountException(String message) {
        super(message);
    }
}
