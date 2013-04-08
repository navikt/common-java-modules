package no.nav.sbl.dialogarena.common.web.showcase;

import no.nav.modig.frontend.FrontendConfigurator;
import no.nav.modig.frontend.MetaTag;
import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

import static no.nav.modig.frontend.FrontendModules.*;

public class ShowcaseApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return ShowcasePage.class;
    }

    @Override
    protected void init() {
        super.init();

        new FrontendConfigurator()
                .withModules(UNDERSCORE, BOOTSTRAP_CORE, BOOTSTRAP_NAVIGATION, BOOTSTRAP_BUTTON, BOOTSTRAP_TOOLTIP, BOOTSTRAP_LABELS_AND_BADGES)
                .addMetas(MetaTag.CHARSET_UTF8, MetaTag.VIEWPORT_SCALE_1, MetaTag.XUA_IE_EDGE)
                .withResourcePacking(this.usesDeploymentConfig())
                .configure(this);
    }

}
