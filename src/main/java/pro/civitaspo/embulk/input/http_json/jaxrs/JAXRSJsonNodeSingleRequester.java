package pro.civitaspo.embulk.input.http_json.jaxrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
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
import pro.civitaspo.embulk.input.http_json.jq.IllegalJQProcessingException;
import pro.civitaspo.embulk.input.http_json.jq.InvalidJQFilterException;
import pro.civitaspo.embulk.input.http_json.jq.JQ;

public class JAXRSJsonNodeSingleRequester extends JAXRSSingleRequester {

    private static final Logger logger =
            LoggerFactory.getLogger(JAXRSJsonNodeSingleRequester.class);

    public static class Builder {
        private String scheme;
        private String host;
        private Optional<Integer> port = Optional.empty();
        private Optional<String> path = Optional.empty();
        private String method;
        private List<Map<String, Object>> headers;
        private List<Map<String, Object>> params;
        private Optional<JsonNode> body;
        private String successCondition;
        private String retryCondition;
        private JQ jq;
        private JAXRSRetryHelper retryHelper;
        private boolean showRequestBodyOnError;

        private Builder() {}

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(Optional<Integer> port) {
            this.port = port;
            return this;
        }

        public Builder port(Integer port) {
            if (port == null) this.port = Optional.empty();
            else this.port = Optional.of(port);
            return this;
        }

        public Builder path(Optional<String> path) {
            this.path = path;
            return this;
        }

        public Builder path(String path) {
            if (path == null) this.path = Optional.empty();
            else this.path = Optional.of(path);
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder headers(List<Map<String, Object>> headers) {
            if (this.headers == null) this.headers = new ArrayList<>();
            this.headers.addAll(headers);
            return this;
        }

        public Builder params(List<Map<String, Object>> params) {
            if (this.params == null) this.params = new ArrayList<>();
            this.params.addAll(params);
            return this;
        }

        public Builder body(Optional<JsonNode> body) {
            this.body = body;
            return this;
        }

        public Builder successCondition(String successCondition) {
            this.successCondition = successCondition;
            return this;
        }

        public Builder retryCondition(String retryCondition) {
            this.retryCondition = retryCondition;
            return this;
        }

        public Builder showRequestBodyOnError(boolean showRequestBodyOnError) {
            this.showRequestBodyOnError = showRequestBodyOnError;
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
            return new JAXRSJsonNodeSingleRequester(this);
        }

        private Optional<JsonNode> buildBody() {
            if (body == null) throw new IllegalStateException("body is not set");
            return body;
        }

        private MultivaluedMap<String, Object> buildParams() {
            if (params == null) throw new IllegalStateException("params is not set");
            MultivaluedMap<String, Object> paramsMap = new MultivaluedHashMap<>();
            params.forEach(p -> p.forEach((k, v) -> paramsMap.add(k, v)));
            return paramsMap;
        }

        private String buildEndpoint() {
            if (scheme == null) throw new IllegalStateException("scheme is not set");
            if (host == null) throw new IllegalStateException("host is not set");
            StringBuilder endpointBuilder = new StringBuilder();
            endpointBuilder.append(scheme).append("://").append(host);
            port.ifPresent(p -> endpointBuilder.append(":").append(p));
            path.ifPresent(p -> endpointBuilder.append(p));
            return endpointBuilder.toString();
        }

        private String buildMethod() {
            if (method == null) throw new IllegalStateException("method is not set");
            return method;
        }

        private MultivaluedMap<String, Object> buildHeaders() {
            if (headers == null) throw new IllegalStateException("headers is not set");
            MultivaluedMap<String, Object> headersMap = new MultivaluedHashMap<>();
            headers.forEach(h -> h.forEach((k, v) -> headersMap.add(k, v)));
            headersMap.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return headersMap;
        }

        private JAXRSResponseJqCondition buildSuccessCondition() {
            if (jq == null) throw new IllegalStateException("jq is not set");
            if (successCondition == null)
                throw new IllegalStateException("successCondition is not set");
            try {
                return new JAXRSResponseJqCondition(jq, successCondition);
            } catch (InvalidJQFilterException e) {
                throw new ConfigException(
                        String.format("Cannot compile the condition: '%s'", successCondition), e);
            }
        }

        private JAXRSResponseJqCondition buildRetryCondition() {
            if (jq == null) throw new IllegalStateException("jq is not set");
            if (retryCondition == null)
                throw new IllegalStateException("retryCondition is not set");
            try {
                return new JAXRSResponseJqCondition(jq, retryCondition);
            } catch (InvalidJQFilterException e) {
                throw new ConfigException(
                        String.format("Cannot compile the condition: '%s'", retryCondition), e);
            }
        }

        private JAXRSRetryHelper buildRetryHelper() {
            if (retryHelper == null) throw new IllegalStateException("retryHelper is not set");
            return retryHelper;
        }

        private boolean buildShowRequestBodyOnError() {
            return showRequestBodyOnError;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Optional<JsonNode> body;
    private final MultivaluedMap<String, Object> params;
    private final String endpoint;
    private final String method;
    private final MultivaluedMap<String, Object> headers;
    private final JAXRSResponseJqCondition successCondition;
    private final JAXRSResponseJqCondition retryableCondition;
    private final JAXRSRetryHelper retryHelper;
    private final boolean showRequestBodyOnError;

    private JAXRSJsonNodeSingleRequester(Builder builder) {
        this.body = builder.buildBody();
        this.params = builder.buildParams();
        this.endpoint = builder.buildEndpoint();
        this.method = builder.buildMethod();
        this.headers = builder.buildHeaders();
        this.successCondition = builder.buildSuccessCondition();
        this.retryableCondition = builder.buildRetryCondition();
        this.retryHelper = builder.buildRetryHelper();
        this.showRequestBodyOnError = builder.buildShowRequestBodyOnError();
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
                Entity<String> entity =
                        Entity.entity(body.get().toString(), MediaType.APPLICATION_JSON_TYPE);
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
            if (!successCondition.isSatisfied(params, body, response)) {
                logger.warn(
                        "Success condition is not satisfied. Condition jq:'{}', Request body: '{}'",
                        successCondition.getJqFilter(),
                        (showRequestBodyOnError && body.isPresent())
                                ? body.get().toString()
                                : "xxxxx(not shown)xxxxx");
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
            return retryableCondition.isSatisfied(params, body, response);
        } catch (InvalidJQFilterException | IllegalJQProcessingException | IOException e) {
            // TODO: Use a suitable exception class.
            throw new DataException(e);
        }
    }

    public ObjectNode requestWithRetry() {
        return retryHelper.requestWithRetry(
                new JAXRSObjectNodeResponseEntityReader(params, body), this);
    }
}
