package no.nav.sbl.dialogarena.common.web;

import no.nav.modig.frontend.FrontendConfigurator;
import no.nav.modig.frontend.MetaTag;
import org.apache.wicket.application.ComponentInstantiationListenerCollection;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static no.nav.modig.frontend.FrontendModules.BOOTSTRAP_BUTTON;
import static no.nav.modig.frontend.FrontendModules.BOOTSTRAP_CORE;
import static no.nav.modig.frontend.FrontendModules.BOOTSTRAP_LABELS_AND_BADGES;
import static no.nav.modig.frontend.FrontendModules.BOOTSTRAP_NAVIGATION;
import static no.nav.modig.frontend.FrontendModules.BOOTSTRAP_TOOLTIP;
import static no.nav.modig.frontend.FrontendModules.UNDERSCORE;

public class ShowcaseApplication extends WebApplication {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Class<? extends WebPage> getHomePage() {
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

        setSpringComponentInjector();
    }


    protected void setSpringComponentInjector() {
        ComponentInstantiationListenerCollection listeners = getComponentInstantiationListeners();
        listeners.add(new SpringComponentInjector(this, applicationContext));
    }
}
