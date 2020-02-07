package br.com.zapia.wppclone.exceptions;

public class DTORelationNotFound extends RuntimeException {
    public DTORelationNotFound() {
        super();
    }

    public DTORelationNotFound(String message) {
        super(message);
    }

    public DTORelationNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public DTORelationNotFound(Throwable cause) {
        super(cause);
    }

    protected DTORelationNotFound(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
