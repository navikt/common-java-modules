package no.nav.sbl.rest.client;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class RestRequest {

    private final Function<Invocation.Builder, Invocation.Builder> decorator;
    private WebTarget webTarget;

    public RestRequest(Function<Invocation.Builder, Invocation.Builder> decorator, WebTarget webTarget) {
        this.decorator = decorator;
        this.webTarget = webTarget;
    }

    public RestRequest queryParam(String name, Object value) {
        webTarget = webTarget.queryParam(name, value);
        return this;
    }

    public <ELEMENT> List<ELEMENT> getList(Class<ELEMENT> responseClass) {
        return decorator.apply(webTarget.request()
                .header(ACCEPT, APPLICATION_JSON)
        ).get(new GenericType<>(new ListType(responseClass)));
    }

    private static class ListType implements ParameterizedType {
        private final Class<?> elementType;

        public ListType(Class<?> elementType) {
            this.elementType = elementType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{elementType};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return List.class;
        }
    }

}
