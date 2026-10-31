package pe.edu.sgfa.common.exception;

public class RyeSgfaException extends RuntimeException {

    public RyeSgfaException(String messageYnq) {
        super(messageYnq);
    }

    public RyeSgfaException(String messageYnq, Throwable causeYnq) {
        super(messageYnq, causeYnq);
    }
}
