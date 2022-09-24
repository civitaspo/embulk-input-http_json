package pro.civitaspo.embulk.input.http_json.jq;

public class InvalidJQFilterException extends JQException {
    public InvalidJQFilterException() {
        super();
    }

    public InvalidJQFilterException(String message) {
        super(message);
    }

    public InvalidJQFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidJQFilterException(Throwable cause) {
        super(cause);
    }

    public InvalidJQFilterException(
            String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
