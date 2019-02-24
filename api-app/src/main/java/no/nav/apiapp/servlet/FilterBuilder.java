package no.nav.apiapp.servlet;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.servlet.DispatcherType.REQUEST;

@Slf4j
public class FilterBuilder {

    private static AtomicInteger counter = new AtomicInteger();

    private final Class<? extends Filter> filterClass;
    private Filter filter;

    private EnumSet<DispatcherType> dispatcherTypes = EnumSet.of(REQUEST);
    private String[] urlPatterns = new String[]{"/*"};
    private Map<String, String> initParameters = new HashMap<>();

    public FilterBuilder(Class<? extends Filter> filterClass) {
        this.filterClass = filterClass;
    }

    public FilterBuilder(Filter filter) {
        this.filterClass = filter.getClass();
        this.filter = filter;
    }

    public FilterBuilder dispatcherTypes(DispatcherType... dispatcherType) {
        this.dispatcherTypes = EnumSet.copyOf(Arrays.asList(dispatcherType));
        return this;
    }

    public FilterBuilder urlPatterns(String... urlPatterns) {
        this.urlPatterns = urlPatterns;
        return this;
    }

    public FilterBuilder initParameter(String name, String value) {
        this.initParameters.put(name, value);
        return this;
    }

    public FilterRegistration.Dynamic register(ServletContextEvent servletContextEvent) {
        return register(servletContextEvent.getServletContext());
    }

    public FilterRegistration.Dynamic register(ServletContext servletContext) {
        String filterClassName = filterClass.getName();
        log.info("adding filter {} on {} for {}",
                filterClassName,
                urlPatterns,
                dispatcherTypes
        );
        String filterName = filterClassName + "_" + counter.incrementAndGet();
        FilterRegistration.Dynamic dynamic = filter != null ? servletContext.addFilter(filterName, filter) : servletContext.addFilter(filterName, filterClass);
        dynamic.addMappingForUrlPatterns(dispatcherTypes, false, urlPatterns);

        initParameters.forEach(dynamic::setInitParameter);

        return dynamic;
    }

}
