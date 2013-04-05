package no.nav.sbl.dialogarena.common.web;

import static no.nav.modig.frontend.FrontendModules.ALL;
import no.nav.modig.frontend.FrontendConfigurator;
import no.nav.modig.frontend.MetaTag;
import no.nav.sbl.dialogarena.common.web.pages.ShowcasePage;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class ShowcaseApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return ShowcasePage.class;
    }

    @Override
    protected void init() {
        super.init();

        new FrontendConfigurator()
                .withModules(ALL)
                .addMetas(MetaTag.CHARSET_UTF8, MetaTag.VIEWPORT_SCALE_1, MetaTag.XUA_IE_EDGE)
                .withResourcePacking(this.usesDeploymentConfig())
                .configure(this);
    }

}
