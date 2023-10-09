package fiftyone.pipeline.core.exceptions;

/**
 * Thrown to indicate that properties are not available yet but MAY(!) be re-requested later.
 */
public class PropertyNotLoadedException extends RuntimeException {

    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = -8356317224796833123L;

    public PropertyNotLoadedException() {
        super();
    }

    public PropertyNotLoadedException(String message) {
        super(message);
    }

    public PropertyNotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyNotLoadedException(Throwable cause) {
        super(cause);
    }

    protected PropertyNotLoadedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
