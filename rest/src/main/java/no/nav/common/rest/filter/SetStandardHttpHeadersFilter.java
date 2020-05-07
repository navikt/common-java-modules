package no.nav.common.rest.filter;

import static no.nav.common.rest.filter.HttpFilterHeaders.STANDARD_HEADERS;

public class SetStandardHttpHeadersFilter extends SetHeaderFilter  {

    public SetStandardHttpHeadersFilter() {
        super(STANDARD_HEADERS);
    }

}
