package no.nav.apiapp.rest;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import javax.ws.rs.container.ContainerResponseFilter;

public class NavMetricsBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(NavMetricsProvider.class)
                .to(ResourceMethodInvocationHandlerProvider.class);

        bind(NavMetricsProvider.class)
                .to(ContainerResponseFilter.class);
    }

}
