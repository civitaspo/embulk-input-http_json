package pro.civitaspo.embulk.input.http_json.jaxrs;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Optional;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import pro.civitaspo.embulk.input.http_json.jq.IllegalJQProcessingException;
import pro.civitaspo.embulk.input.http_json.jq.InvalidJQFilterException;
import pro.civitaspo.embulk.input.http_json.jq.JQ;

public class JAXRSResponseJqCondition {

    private final JQ jq;
    private final String jqFilter;

    public JAXRSResponseJqCondition(JQ jq, String jqFilter) throws InvalidJQFilterException {
        this.jq = jq;
        this.jqFilter = jqFilter;
        validateFilter();
    }

    private void validateFilter() throws InvalidJQFilterException {
        jq.validateFilter(jqFilter);
    }

    public boolean isSatisfied(
            MultivaluedMap<String, Object> requestParams,
            Optional<JsonNode> requestBody,
            Response response)
            throws InvalidJQFilterException, IOException, IllegalJQProcessingException {
        return jq.jqBoolean(
                jqFilter,
                JAXRSResponseJson.convertResponseToObjectNode(
                        requestParams, requestBody, response));
    }

    public String getJqFilter() {
        return jqFilter;
    }
}
