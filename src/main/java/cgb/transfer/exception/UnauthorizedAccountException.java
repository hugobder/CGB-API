package cgb.transfer.exception;

public class UnauthorizedAccountException extends TransferException {
    public UnauthorizedAccountException(String message) {
        super(message);
    }
}
