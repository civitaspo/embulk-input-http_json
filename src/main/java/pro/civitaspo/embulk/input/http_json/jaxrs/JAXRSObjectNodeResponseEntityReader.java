package pro.civitaspo.embulk.input.http_json.jaxrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.embulk.util.retryhelper.jaxrs.JAXRSResponseReader;

public class JAXRSObjectNodeResponseEntityReader implements JAXRSResponseReader<ObjectNode> {
    private final MultivaluedMap<String, Object> requestParams;
    private final Optional<JsonNode> requestBody;

    public JAXRSObjectNodeResponseEntityReader(
            MultivaluedMap<String, Object> requestParams, Optional<JsonNode> requestBody) {
        this.requestParams = requestParams;
        this.requestBody = requestBody;
    }

    @Override
    public ObjectNode readResponse(Response response) throws Exception {
        return JAXRSResponseJson.convertResponseToObjectNode(requestParams, requestBody, response);
    }
}
