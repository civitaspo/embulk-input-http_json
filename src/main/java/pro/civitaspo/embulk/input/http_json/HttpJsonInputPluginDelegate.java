package pro.civitaspo.embulk.input.http_json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.client.ClientBuilder;
import org.embulk.base.restclient.DefaultServiceDataSplitter;
import org.embulk.base.restclient.RestClientInputPluginDelegate;
import org.embulk.base.restclient.ServiceDataSplitter;
import org.embulk.base.restclient.ServiceResponseMapper;
import org.embulk.base.restclient.jackson.JacksonServiceRecord;
import org.embulk.base.restclient.jackson.JacksonServiceResponseMapper;
import org.embulk.base.restclient.record.RecordImporter;
import org.embulk.base.restclient.record.ValueLocator;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.TaskReport;
import org.embulk.spi.DataException;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.retryhelper.jaxrs.JAXRSRetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.civitaspo.embulk.input.http_json.config.PluginTask;
import pro.civitaspo.embulk.input.http_json.config.PluginTask.PagerOption;
import pro.civitaspo.embulk.input.http_json.config.RequestOption;
import pro.civitaspo.embulk.input.http_json.config.validation.BeanValidator;
import pro.civitaspo.embulk.input.http_json.jaxrs.JAXRSJsonNodeSingleRequester;
import pro.civitaspo.embulk.input.http_json.jq.IllegalJQProcessingException;
import pro.civitaspo.embulk.input.http_json.jq.InvalidJQFilterException;
import pro.civitaspo.embulk.input.http_json.jq.JQ;

public class HttpJsonInputPluginDelegate implements RestClientInputPluginDelegate<PluginTask> {
    private static final Logger logger = LoggerFactory.getLogger(HttpJsonInputPluginDelegate.class);
    private static final JQ jq = new JQ();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ConfigMapperFactory configMapperFactory;

    public HttpJsonInputPluginDelegate(ConfigMapperFactory configMapperFactory) {
        this.configMapperFactory = configMapperFactory;
    }

    @Override
    public void validateInputTask(PluginTask task) {
        BeanValidator.validate(task);
        validateJsonQuery("success_condition", task.getSuccessCondition());
        validateJsonQuery("transformer", task.getTransformer());
        validateJsonQuery("retry.condition", task.getRetry().getCondition());
    }

    @Override
    public TaskReport ingestServiceData(
            PluginTask task,
            RecordImporter recordImporter,
            int taskIndex,
            PageBuilder pageBuilder) {
        return tryWithJAXRSRetryHelper(
                task, retryHelper -> mainTask(task, recordImporter, pageBuilder, retryHelper));
    }

    private TaskReport mainTask(
            PluginTask task,
            RecordImporter recordImporter,
            PageBuilder pageBuilder,
            JAXRSRetryHelper retryHelper) {
        TaskReport report =
                configMapperFactory
                        .newTaskReport(); // todo: numRecords, avgLatency, maxLatency, minLatency,
        ObjectNode response =
                fetch(task, retryHelper, task.getPager().getInitialParams(), task.getBody());
        ingestTransformedJsonRecord(
                task, recordImporter, pageBuilder, transformResponse(task, response));
        while (pagenationRequired(response, task.getPager())) {
            sleep(task.getPager().getIntervalMillis());
            response =
                    fetch(
                            task,
                            retryHelper,
                            nextParams(response, task.getPager()),
                            nextBody(response, task.getPager()).or(() -> task.getBody()));
            ingestTransformedJsonRecord(
                    task, recordImporter, pageBuilder, transformResponse(task, response));
        }
        return report;
    }

    private ObjectNode fetch(
            RequestOption requestOption,
            JAXRSRetryHelper retryHelper,
            List<Map<String, Object>> additionalParams,
            Optional<JsonNode> body) {

        return JAXRSJsonNodeSingleRequester.builder()
                .jq(jq)
                .retryHelper(retryHelper)
                .scheme(requestOption.getScheme())
                .host(requestOption.getHost())
                .port(requestOption.getPort())
                .path(requestOption.getPath())
                .method(requestOption.getMethod())
                .headers(convertHeadersType(requestOption.getHeaders()))
                .params(requestOption.getParams())
                .params(additionalParams)
                .body(body)
                .successCondition(requestOption.getSuccessCondition())
                .retryCondition(requestOption.getRetry().getCondition())
                .showRequestBodyOnError(requestOption.getShowRequestBodyOnError())
                .build()
                .requestWithRetry();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertHeadersType(List<Map<String, String>> headers) {
        return headers.stream()
                .map(h -> (Map<String, Object>) (Map<String, ?>) h)
                .collect(Collectors.toList());
    }

    @Override
    public ConfigDiff buildConfigDiff(
            PluginTask task, Schema schema, int taskCount, List<TaskReport> taskReports) {
        taskReports.forEach(report -> logger.info(report.toString()));
        return configMapperFactory.newConfigDiff();
    }

    @Override
    public ServiceResponseMapper<? extends ValueLocator> buildServiceResponseMapper(
            PluginTask task) {
        return JacksonServiceResponseMapper.builder()
                .add(
                        task.getTransformedJsonColumnName(),
                        Types.JSON,
                        task.getDefaultTimestampFormat())
                .build();
    }

    @Override
    public ServiceDataSplitter<PluginTask> buildServiceDataSplitter(PluginTask task) {
        // note: This plugin doesn't support parallel processing.
        return new DefaultServiceDataSplitter<>();
    }

    private void validateJsonQuery(String name, String jqFilter) {
        try {
            jq.validateFilter(jqFilter);
        } catch (InvalidJQFilterException e) {
            throw new ConfigException(String.format("'%s' filter is invalid.", name), e);
        }
    }

    private <T> T tryWithJAXRSRetryHelper(PluginTask task, Function<JAXRSRetryHelper, T> f) {
        try (JAXRSRetryHelper retryHelper =
                new JAXRSRetryHelper(
                        task.getRetry().getMaxRetries(),
                        task.getRetry().getInitialIntervalMillis(),
                        task.getRetry().getMaxIntervalMillis(),
                        () -> ClientBuilder.newBuilder().build())) {
            return f.apply(retryHelper);
        }
    }

    private JsonNode transformResponse(PluginTask task, ObjectNode response) {
        try {
            return jq.jqSingle(task.getTransformer(), response);
        } catch (IllegalJQProcessingException e) {
            throw new DataException("Failed to apply 'transformer'.", e);
        }
    }

    private boolean pagenationRequired(ObjectNode response, PagerOption pagerOption) {
        try {
            return jq.jqBoolean(pagerOption.getWhile(), response);
        } catch (IllegalJQProcessingException e) {
            throw new DataException("Failed to apply 'until_condition'.", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while sleeping.", e);
        }
    }

    private List<Map<String, Object>> nextParams(ObjectNode response, PagerOption pagerOption) {
        List<Map<String, Object>> nextParams = new ArrayList<>();
        for (Map<String, Object> p : pagerOption.getNextParams()) {
            for (Map.Entry<String, Object> e : p.entrySet()) {
                Map<String, Object> np = new HashMap<>();
                Object v;
                try {
                    JsonNode jn = jq.jqSingle(e.getValue().toString(), response);
                    if (jn.isBoolean()) {
                        v = jn.asBoolean();
                    } else if (jn.isDouble()) {
                        v = jn.asDouble();
                    } else if (jn.isLong()) {
                        v = jn.asLong();
                    } else if (jn.isInt()) {
                        v = jn.asInt();
                    } else if (jn.isLong()) {
                        v = jn.asLong();
                    } else if (jn.isTextual()) {
                        v = jn.asText();
                    } else {
                        throw new ConfigException(
                                String.format(
                                        "Unsupported value: %s",
                                        objectMapper.writeValueAsString(jn)));
                    }
                } catch (IllegalJQProcessingException | JsonProcessingException ex) {
                    throw new DataException("Failed to apply 'next_params'.", ex);
                }
                np.put(e.getKey(), v);
                nextParams.add(np);
            }
        }
        return nextParams;
    }

    private Optional<JsonNode> nextBody(ObjectNode response, PagerOption pagerOption) {
        if (!pagerOption.getNextBodyTransformer().isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(jq.jqSingle(pagerOption.getNextBodyTransformer().get(), response));
        } catch (IllegalJQProcessingException ex) {
            throw new DataException("Failed to apply 'next_body_transformer'.", ex);
        }
    }

    private void ingestTransformedJsonRecord(
            PluginTask task,
            RecordImporter recordImporter,
            PageBuilder pageBuilder,
            JsonNode transformedJsonRecord) {

        if (task.getExtractTransformedJsonArray()) {
            if (!transformedJsonRecord.isArray())
                throw new DataException("Expected array node: " + transformedJsonRecord.toString());
            for (JsonNode record : ((ArrayNode) transformedJsonRecord)) {
                ObjectNode on = objectMapper.createObjectNode();
                on.set(task.getTransformedJsonColumnName(), record);
                recordImporter.importRecord(new JacksonServiceRecord(on), pageBuilder);
            }
        } else {
            ObjectNode on = objectMapper.createObjectNode();
            on.set(task.getTransformedJsonColumnName(), transformedJsonRecord);
            recordImporter.importRecord(new JacksonServiceRecord(on), pageBuilder);
        }
    }
}
