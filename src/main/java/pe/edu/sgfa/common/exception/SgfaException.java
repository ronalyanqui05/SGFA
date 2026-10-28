package pe.edu.sgfa.common.exception;

public class SgfaException extends RuntimeException {

    public SgfaException(String mssg) {
        super(mssg);
    }

    public SgfaException(String mssg, Throwable cs) {
        super(mssg, cs);
    }
}
