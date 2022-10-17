package pro.civitaspo.embulk.input.http_json.config;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.Task;
import pro.civitaspo.embulk.input.http_json.config.validation.annotations.ValueOfEnum;

public interface RequestOption extends Task {
    public enum URIScheme {
        http,
        https;

        public static URIScheme of(String value) {
            return URIScheme.valueOf(value.toLowerCase(Locale.ENGLISH));
        }
    }

    public static enum RequestMethod {
        CONNECT,
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
        HEAD,
        OPTIONS,
        TRACE;

        public static RequestMethod of(String value) {
            return RequestMethod.valueOf(value.toUpperCase(Locale.ENGLISH));
        }
    }

    public interface RetryOption extends Task {
        @Config("condition")
        @ConfigDefault("\"true\"")
        public String getCondition();

        @Config("max_retries")
        @ConfigDefault("7")
        public int getMaxRetries();

        @Config("initial_interval_millis")
        @ConfigDefault("1000")
        public int getInitialIntervalMillis();

        @Config("max_interval_millis")
        @ConfigDefault("60000")
        public int getMaxIntervalMillis();
    }

    @Config("scheme")
    @ConfigDefault("\"https\"")
    @ValueOfEnum(enumClass = URIScheme.class)
    public String getScheme();

    @Config("host")
    @NotBlank
    public String getHost();

    @Config("port")
    @ConfigDefault("null")
    public Optional<@Min(0) @Max(65535) Integer> getPort();

    @Config("path")
    @ConfigDefault("null")
    public Optional<@NotBlank String> getPath();

    @Config("headers")
    @ConfigDefault("[]")
    public List<@Size(min = 1, max = 1) Map<@NotBlank String, @NotBlank String>> getHeaders();

    @Config("method")
    @ConfigDefault("\"GET\"")
    @ValueOfEnum(enumClass = RequestMethod.class, caseSensitive = false)
    public String getMethod();

    @Config("params")
    @ConfigDefault("[]")
    public List<@Size(min = 1, max = 1) Map<@NotBlank String, @NotNull Object>> getParams();

    @Config("body")
    @ConfigDefault("null")
    public Optional<JsonNode> getBody();

    @Config("success_condition")
    @ConfigDefault("\".status_code_class == 200\"")
    public @NotBlank String getSuccessCondition();

    @Config("retry")
    @ConfigDefault("{}")
    public RetryOption getRetry();

    @Config("show_request_body_on_error")
    @ConfigDefault("true")
    @NotNull
    public Boolean getShowRequestBodyOnError();
}
