package pro.civitaspo.embulk.input.http_json.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.embulk.base.restclient.RestClientInputTaskBase;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.Task;

public interface PluginTask extends RestClientInputTaskBase, RequestOption {

    @Config("transformer")
    @ConfigDefault("\"[.response_body]\"")
    public @NotBlank String getTransformer();

    @Config("transformed_json_column_name")
    @ConfigDefault("\"payload\"")
    public @NotBlank String getTransformedJsonColumnName();

    @Config("extract_transformed_json_array")
    @ConfigDefault("true")
    public boolean getExtractTransformedJsonArray();

    public interface PagerOption extends Task {
        @Config("initial_params")
        @ConfigDefault("[]")
        public List<@Size(min = 1, max = 1) Map<@NotBlank String, @NotNull Object>>
                getInitialParams();

        @Config("next_params")
        @ConfigDefault("[]")
        public List<@Size(min = 1, max = 1) Map<@NotBlank String, @NotBlank Object>>
                getNextParams();

        @Config("next_body_transformer")
        @ConfigDefault("null")
        public Optional<String> getNextBodyTransformer();

        @Config("while")
        @ConfigDefault("\"false\"")
        public @NotBlank String getWhile();

        @Config("interval_millis")
        @ConfigDefault("100")
        public @Min(0) long getIntervalMillis();
    }

    @Config("pager")
    @ConfigDefault("{}")
    public PagerOption getPager();

    @Config("show_request_body_on_error")
    @ConfigDefault("true")
    @NotNull
    public Boolean getShowRequestBodyOnError();

    public interface PrepareOption extends Task {
        public interface NamedRequestOption extends RequestOption {
            @Config("name")
            @NotBlank
            public String getName();
        }

        @Config("requests")
        @ConfigDefault("[]")
        public List<NamedRequestOption> getRequests();

        @Config("prepared_params")
        @ConfigDefault("[]")
        public List<@Size(min = 1, max = 1) Map<@NotBlank String, @NotBlank Object>>
                getPreparedParams();

        @Config("prepared_body_transformer")
        @ConfigDefault("null")
        public Optional<String> getPreparedBodyTransformer();
    }

    @Config("prepare")
    @ConfigDefault("null")
    public Optional<PrepareOption> getPrepare();

    @Config("default_timezone")
    @ConfigDefault("\"UTC\"")
    @NotBlank
    public String getDefaultTimeZoneId();

    @Config("default_timestamp_format")
    @ConfigDefault("\"%Y-%m-%d %H:%M:%S.%N %z\"")
    @NotNull
    public String getDefaultTimestampFormat();

    @Config("default_date")
    @ConfigDefault("\"1970-01-01\"")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    public String getDefaultDate();
}
