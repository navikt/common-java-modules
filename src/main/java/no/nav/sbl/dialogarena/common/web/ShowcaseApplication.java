package no.nav.sbl.dialogarena.common.web;

import no.nav.sbl.dialogarena.common.web.pages.ShowcasePage;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class ShowcaseApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return ShowcasePage.class;
    }

}
