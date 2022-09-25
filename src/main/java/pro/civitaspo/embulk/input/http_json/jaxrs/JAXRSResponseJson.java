package pro.civitaspo.embulk.input.http_json.jaxrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class JAXRSResponseJson {
    private JAXRSResponseJson() {}

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ObjectNode convertResponseToObjectNode(
            MultivaluedMap<String, Object> requestParams, Response response) throws IOException {
        ObjectNode responseJson = mapper.createObjectNode();
        responseJson.putArray("request_params").addAll(extractRequestParams(requestParams));
        responseJson.put("status_code", response.getStatus());
        responseJson.put("status_code_class", (response.getStatus() / 100) * 100);
        String json = response.readEntity(String.class);
        responseJson.set("response_body", mapper.readValue(json, JsonNode.class));
        return responseJson;
    }

    private static List<ObjectNode> extractRequestParams(
            MultivaluedMap<String, Object> requestParams) {
        List<ObjectNode> list = new ArrayList<>();
        for (Map.Entry<String, List<Object>> e : requestParams.entrySet()) {
            for (Object v : e.getValue()) {
                ObjectNode o = mapper.createObjectNode();
                o.put("name", e.getKey());
                o.set("value", mapper.convertValue(v, JsonNode.class));
                list.add(o);
            }
        }
        return list;
    }
}
