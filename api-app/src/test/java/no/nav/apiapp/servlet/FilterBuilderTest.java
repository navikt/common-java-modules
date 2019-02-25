package no.nav.apiapp.servlet;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.*;
import java.util.EnumSet;

import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;
import static org.mockito.Mockito.*;

public class FilterBuilderTest {

    private Filter filter = mock(Filter.class);
    private ServletContext servletContext = mock(ServletContext.class);
    private FilterRegistration.Dynamic dynamic = mock(FilterRegistration.Dynamic.class);
    private ServletContextEvent servletContextEvent = mock(ServletContextEvent.class);
    private Class<? extends Filter> filterClass = filter.getClass();

    @Before
    public void setup() {
        when(servletContext.addFilter(anyString(), eq(filter))).thenReturn(dynamic);
        when(servletContext.addFilter(anyString(), any(Class.class))).thenReturn(dynamic);
        when(servletContextEvent.getServletContext()).thenReturn(servletContext);
    }

    @Test
    public void defaultRegistration() {
        new FilterBuilder(filter).register(servletContext);
        new FilterBuilder(filter).register(servletContextEvent);
        new FilterBuilder(filterClass).register(servletContext);
        new FilterBuilder(filterClass).register(servletContextEvent);

        verify(servletContext, times(2)).addFilter(anyString(), eq(filter));
        verify(servletContext, times(2)).addFilter(anyString(), eq(filterClass));

        verify(dynamic, times(4)).addMappingForUrlPatterns(EnumSet.of(REQUEST), false, "/*");
    }

    @Test
    public void customRegistration() {
        FilterBuilder filterBuilder = new FilterBuilder(filter)
                .initParameter("paramA", "valueA")
                .initParameter("paramB", "valueB")
                .urlPatterns("/a", "/b", "/c")
                .dispatcherTypes(INCLUDE, FORWARD);

        filterBuilder.register(servletContext);
        filterBuilder.register(servletContextEvent);

        verify(servletContext, times(2)).addFilter(anyString(), eq(filter));

        verify(dynamic, times(2)).addMappingForUrlPatterns(EnumSet.of(INCLUDE, FORWARD), false, "/a", "/b", "/c");
        verify(dynamic, times(2)).setInitParameter("paramA", "valueA");
        verify(dynamic, times(2)).setInitParameter("paramB", "valueB");
    }


}