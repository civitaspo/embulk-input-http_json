package pro.civitaspo.embulk.input.http_json.jq;

public class JQException extends Exception {
    public JQException() {
        super();
    }

    public JQException(String message) {
        super(message);
    }

    public JQException(String message, Throwable cause) {
        super(message, cause);
    }

    public JQException(Throwable cause) {
        super(cause);
    }

    public JQException(
            String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
