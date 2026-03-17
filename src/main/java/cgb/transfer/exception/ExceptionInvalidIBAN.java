package cgb.transfer.exception;

public abstract class ExceptionInvalidIBAN extends RuntimeException {
    public ExceptionInvalidIBAN(String message) {
        super(message);
    }
}
