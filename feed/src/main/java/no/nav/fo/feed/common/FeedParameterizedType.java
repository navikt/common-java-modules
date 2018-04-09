package no.nav.fo.feed.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class FeedParameterizedType implements ParameterizedType {
    private Class cls;

    public FeedParameterizedType(Class cls) {
        this.cls = cls;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[]{cls};
    }

    @Override
    public Type getRawType() {
        return FeedResponse.class;
    }

    @Override
    public Type getOwnerType() {
        return FeedResponse.class;
    }
}
