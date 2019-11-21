package no.nav.sbl.rest;

import no.nav.common.utils.IdUtils;
import no.nav.sbl.util.EnvironmentUtils;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import static no.nav.sbl.rest.RestUtils.CALL_ID_HEADER_NAME;
import static no.nav.sbl.rest.RestUtils.CONSUMER_ID_HEADER_NAME;

public class DefaultHeadersFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
            requestContext.getHeaders().add(CALL_ID_HEADER_NAME, IdUtils.generateId());
            requestContext.getHeaders().add(CONSUMER_ID_HEADER_NAME, EnvironmentUtils.requireApplicationName());
    }
}
