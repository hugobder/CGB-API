package cgb.transfer.exception;

public class AccountNotFoundException extends TransferException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
