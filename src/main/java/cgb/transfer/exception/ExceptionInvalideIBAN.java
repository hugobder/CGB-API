package cgb.transfer.exception;

public abstract class ExceptionInvalideIBAN extends RuntimeException {
    public ExceptionInvalideIBAN(String message) {
        super(message);
    }
}
