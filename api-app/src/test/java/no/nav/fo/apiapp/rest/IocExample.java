package no.nav.fo.apiapp.rest;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import static no.nav.sbl.util.AssertUtils.assertTrue;

@Slf4j
@Path("/ioc")
public class IocExample {

    @Inject
    private Provider<HttpServletRequest> httpServletRequestProviderInjected;
    private Provider<HttpServletRequest> httpServletRequestProvider;

    @Inject
    private Provider<UriInfo> uriInfoProviderInjected;
    private Provider<UriInfo> uriInfoProvider;

    @Inject
    private SpringComponent springComponent;

    @Inject
    public IocExample(Provider<HttpServletRequest> httpServletRequestProvider, Provider<UriInfo> uriInfoProvider) {
        this.httpServletRequestProvider = httpServletRequestProvider;
        this.uriInfoProvider = uriInfoProvider;
    }

    @Path("/")
    @GET
    public String testIoc(
            @Context HttpServletRequest httpServletRequest,
            @Context UriInfo uriInfo
    ) {
        List<String> requestUrls = new ArrayList<>();

        addRequest(requestUrls, () -> httpServletRequest);
        addRequest(requestUrls, () -> httpServletRequestProvider.get());
        addRequest(requestUrls, () -> httpServletRequestProviderInjected.get());
        addRequest(requestUrls, () -> springComponent.httpServletRequestProvider.get());

        addUriInfo(requestUrls, () -> uriInfo);
        addUriInfo(requestUrls, () -> uriInfoProviderInjected.get());

        try {
            addUriInfo(requestUrls, () -> uriInfoProvider.get());
        } catch (Exception e) { // no such bean
            log.warn(e.getMessage(), e);
        }
        try {
            addUriInfo(requestUrls, () -> springComponent.uriInfoProvider.get());
        } catch (Exception e) { // no such bean
            log.warn(e.getMessage(), e);
        }

        return selectUnique(requestUrls);
    }

    private String selectUnique(List<String> requestUrls) {
        HashSet<String> set = new HashSet<>(requestUrls);
        assertTrue(set.size() == 1);
        return set.iterator().next();
    }

    private void addUriInfo(List<String> requestUrls, Supplier<UriInfo> uriInfoSupplier) {
        requestUrls.add(uriInfoSupplier.get().getRequestUri().toString());
    }

    private void addRequest(List<String> requestUrls, Supplier<HttpServletRequest> httpServletRequestSupplier) {
        requestUrls.add(httpServletRequestSupplier.get().getRequestURL().toString());
    }


    @Component
    public static class SpringComponent {

        @Inject
        private Provider<HttpServletRequest> httpServletRequestProvider;

        @Inject
        private Provider<UriInfo> uriInfoProvider;

    }


}
