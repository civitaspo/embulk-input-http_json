package pro.civitaspo.embulk.input.http_json.jaxrs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.embulk.config.ConfigException;
import org.embulk.spi.DataException;
import org.embulk.util.retryhelper.jaxrs.JAXRSRetryHelper;
import org.embulk.util.retryhelper.jaxrs.JAXRSSingleRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.civitaspo.embulk.input.http_json.config.PluginTask;
import pro.civitaspo.embulk.input.http_json.jq.IllegalJQProcessingException;
import pro.civitaspo.embulk.input.http_json.jq.InvalidJQFilterException;
import pro.civitaspo.embulk.input.http_json.jq.JQ;

public class JAXRSJsonNodeSingleRequester extends JAXRSSingleRequester {

    private static final Logger logger =
            LoggerFactory.getLogger(JAXRSJsonNodeSingleRequester.class);

    public static class Builder {
        private PluginTask task;
        private List<Map<String, Object>> additionalParams;
        private JQ jq;
        private JAXRSRetryHelper retryHelper;

        private Builder() {}

        public Builder task(PluginTask task) {
            this.task = task;
            return this;
        }

        public Builder additionalParams(List<Map<String, Object>> additionalParams) {
            this.additionalParams = additionalParams;
            return this;
        }

        public Builder jq(JQ jq) {
            this.jq = jq;
            return this;
        }

        public Builder retryHelper(JAXRSRetryHelper retryHelper) {
            this.retryHelper = retryHelper;
            return this;
        }

        public JAXRSJsonNodeSingleRequester build() {
            if (task == null) throw new IllegalStateException("task is not set");
            if (jq == null) throw new IllegalStateException("jq is not set");
            if (additionalParams == null)
                throw new IllegalStateException("additionalParams is not set");
            if (retryHelper == null) throw new IllegalStateException("retryHelper is not set");
            return new JAXRSJsonNodeSingleRequester(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Optional<String> body;
    private final String contentType;
    private final MultivaluedMap<String, Object> params;
    private final String endpoint;
    private final String method;
    private final MultivaluedMap<String, Object> headers;
    private final JAXRSResponseJqCondition successCondition;
    private final JAXRSResponseJqCondition retryableCondition;
    private final JAXRSRetryHelper retryHelper;
    private final boolean showRequestBodyOnError;

    private JAXRSJsonNodeSingleRequester(Builder builder) {
        this.body = builder.task.getBody();
        this.contentType = builder.task.getContentType();
        this.params = buildParams(builder.task, builder.additionalParams);
        this.endpoint = buildEndpoint(builder.task);
        this.method = builder.task.getMethod();
        this.headers = buildHeaders(builder.task);
        try {
            this.successCondition =
                    new JAXRSResponseJqCondition(builder.jq, builder.task.getSuccessCondition());
            this.retryableCondition =
                    new JAXRSResponseJqCondition(
                            builder.jq, builder.task.getRetry().getCondition());
        } catch (InvalidJQFilterException e) {
            throw new ConfigException(e);
        }
        this.retryHelper = builder.retryHelper;
        this.showRequestBodyOnError = builder.task.getShowRequestBodyOnError();
    }

    private String buildEndpoint(PluginTask task) {
        StringBuilder endpointBuilder = new StringBuilder();
        endpointBuilder.append(task.getScheme().toString());
        endpointBuilder.append("://");
        endpointBuilder.append(task.getHost());
        task.getPort().ifPresent(port -> endpointBuilder.append(":").append(port));
        task.getPath().ifPresent(path -> endpointBuilder.append(path));
        return endpointBuilder.toString();
    }

    private MultivaluedMap<String, Object> buildHeaders(PluginTask task) {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        task.getHeaders().forEach(h -> h.forEach((k, v) -> headers.add(k, v)));
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.valueOf(task.getContentType()));
        return headers;
    }

    private MultivaluedMap<String, Object> buildParams(
            PluginTask task, List<Map<String, Object>> additionalParams) {
        MultivaluedMap<String, Object> paramsMap = new MultivaluedHashMap<>();
        task.getParams().forEach(p -> p.forEach((k, v) -> paramsMap.add(k, v)));
        additionalParams.forEach(p -> p.forEach((k, v) -> paramsMap.add(k, v)));
        return paramsMap;
    }

    private Response doRequestOnce(Client client) {
        WebTarget target = client.target(endpoint);
        for (Map.Entry<String, List<Object>> e : params.entrySet()) {
            target = target.queryParam(e.getKey(), e.getValue().toArray());
        }
        long latency = -1L; // -1L means "not measured yet".
        try {
            long start = System.currentTimeMillis();
            Response delegate;
            if (body.isPresent()) {
                Entity<String> entity = Entity.entity(body.get(), contentType);
                delegate = target.request().headers(headers).method(method, entity);
            } else {
                delegate = target.request().headers(headers).method(method);
            }
            latency = System.currentTimeMillis() - start;
            return JAXRSEntityRecycleResponse.of(delegate);
        } finally {
            logger.info("Request: {} {} (Latency: {} ms)", method, target.getUri(), latency);
        }
    }

    @Override
    public Response requestOnce(Client client) {
        Response response = doRequestOnce(client);
        // NOTE: If an exception is thrown by the exception handling in the link below, the
        //       error message will be poor, so to avoid this, put the exception handling
        //       here.
        // https://github.com/embulk/embulk-util-retryhelper/blob/402412d/embulk-util-retryhelper-jaxrs/src/main/java/org/embulk/util/retryhelper/jaxrs/JAXRSRetryHelper.java#L107-L109
        try {
            if (!successCondition.isSatisfied(params, response)) {
                if (showRequestBodyOnError) {
                    logger.warn(
                            "Success condition is not satisfied. Condition jq:'{}', Request body: '{}'",
                            successCondition.getJqFilter(),
                            body.toString());
                }
                throw JAXRSWebApplicationExceptionWrapper.wrap(response);
            }
        } catch (InvalidJQFilterException | IllegalJQProcessingException | IOException e) {
            try {
                String body = response.readEntity(String.class);
                throw new DataException("response_body: " + body, e);
            } catch (Exception e2) {
                logger.debug(
                        "Exception '{}' is thrown when reading the response.", e.getMessage(), e2);
                throw new DataException(e);
            }
        }
        return response;
    }

    @Override
    protected boolean isResponseStatusToRetry(Response response) {
        try {
            return retryableCondition.isSatisfied(params, response);
        } catch (InvalidJQFilterException | IllegalJQProcessingException | IOException e) {
            // TODO: Use a suitable exception class.
            throw new DataException(e);
        }
    }

    public ObjectNode requestWithRetry() {
        return retryHelper.requestWithRetry(new JAXRSObjectNodeResponseEntityReader(params), this);
    }
}
