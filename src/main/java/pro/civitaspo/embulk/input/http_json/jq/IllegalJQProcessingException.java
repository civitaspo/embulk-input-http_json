package pro.civitaspo.embulk.input.http_json.jq;

public class IllegalJQProcessingException extends JQException {
    public IllegalJQProcessingException(String message) {
        super(message);
    }

    public IllegalJQProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalJQProcessingException(Throwable cause) {
        super(cause);
    }

    public IllegalJQProcessingException() {
        super();
    }

    public IllegalJQProcessingException(
            String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
