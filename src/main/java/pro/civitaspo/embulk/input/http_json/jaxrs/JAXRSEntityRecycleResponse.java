package pro.civitaspo.embulk.input.http_json.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

/**
 * This class is a wrapper of {@link Response} to recycle the response entity. The primitive {@link
 * Response} class does not allow to read the response entity twice.
 */
public class JAXRSEntityRecycleResponse extends Response {

    private final ObjectMapper mapper;
    private final Response delegate;
    private final Optional<String> body;

    private JAXRSEntityRecycleResponse(Response delegate) {
        this.mapper = new ObjectMapper();
        this.delegate = delegate;
        if (delegate.hasEntity()) {
            this.body = Optional.of(delegate.readEntity(String.class));
        } else {
            this.body = Optional.empty();
        }
    }

    public static JAXRSEntityRecycleResponse of(Response delegate) {
        return new JAXRSEntityRecycleResponse(delegate);
    }

    @Override
    public Object getEntity() {
        return body.get().isEmpty() ? null : body.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readEntity(Class<T> entityType) {
        if (entityType.equals(String.class)) {
            return (T) getEntity();
        }
        if (entityType.equals(ObjectNode.class)) {
            try {
                return (T) mapper.readTree((String) getEntity());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalArgumentException("Unsupported entity type: " + entityType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readEntity(GenericType<T> entityType) {
        return readEntity((Class<T>) entityType.getRawType());
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return readEntity(entityType);
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return readEntity(entityType);
    }

    @Override
    public boolean hasEntity() {
        return body.isPresent();
    }

    /* NOTE: The below methods are just delegated methods. */

    @Override
    public int getStatus() {
        return delegate.getStatus();
    }

    @Override
    public StatusType getStatusInfo() {
        return delegate.getStatusInfo();
    }

    @Override
    public boolean bufferEntity() {
        return delegate.bufferEntity();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public MediaType getMediaType() {
        return delegate.getMediaType();
    }

    @Override
    public Locale getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public int getLength() {
        return delegate.getLength();
    }

    @Override
    public Set<String> getAllowedMethods() {
        return delegate.getAllowedMethods();
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public EntityTag getEntityTag() {
        return delegate.getEntityTag();
    }

    @Override
    public Date getDate() {
        return delegate.getDate();
    }

    @Override
    public Date getLastModified() {
        return delegate.getLastModified();
    }

    @Override
    public URI getLocation() {
        return delegate.getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return delegate.getLinks();
    }

    @Override
    public boolean hasLink(String relation) {
        return delegate.hasLink(relation);
    }

    @Override
    public Link getLink(String relation) {
        return delegate.getLink(relation);
    }

    @Override
    public Builder getLinkBuilder(String relation) {
        return delegate.getLinkBuilder(relation);
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return delegate.getStringHeaders();
    }

    @Override
    public String getHeaderString(String name) {
        return delegate.getHeaderString(name);
    }
}
