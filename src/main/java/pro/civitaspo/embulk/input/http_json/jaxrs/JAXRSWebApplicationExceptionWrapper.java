package pro.civitaspo.embulk.input.http_json.jaxrs;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class JAXRSWebApplicationExceptionWrapper extends WebApplicationException {

    private JAXRSWebApplicationExceptionWrapper(WebApplicationException e) {
        super(
                e.getMessage() + ", Body: " + e.getResponse().readEntity(String.class),
                e.getResponse());
    }

    public static JAXRSWebApplicationExceptionWrapper wrap(WebApplicationException e) {
        return new JAXRSWebApplicationExceptionWrapper(e);
    }

    public static JAXRSWebApplicationExceptionWrapper wrap(Response response) {
        return new JAXRSWebApplicationExceptionWrapper(new WebApplicationException(response));
    }
}
