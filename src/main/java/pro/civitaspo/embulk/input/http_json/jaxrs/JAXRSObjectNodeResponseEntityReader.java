package pro.civitaspo.embulk.input.http_json.jaxrs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.embulk.util.retryhelper.jaxrs.JAXRSResponseReader;

public class JAXRSObjectNodeResponseEntityReader implements JAXRSResponseReader<ObjectNode> {
    private final MultivaluedMap<String, Object> requestParams;

    public JAXRSObjectNodeResponseEntityReader(MultivaluedMap<String, Object> requestParams) {
        this.requestParams = requestParams;
    }

    @Override
    public ObjectNode readResponse(Response response) throws Exception {
        return JAXRSResponseJson.convertResponseToObjectNode(requestParams, response);
    }
}
