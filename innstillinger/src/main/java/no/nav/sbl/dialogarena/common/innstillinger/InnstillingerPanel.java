package no.nav.sbl.dialogarena.common.innstillinger;

import static no.nav.modig.wicket.conditional.ConditionalUtils.hasCssClassIf;
import no.nav.modig.core.context.SecurityContext;

import org.apache.wicket.markup.head.CssContentHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptContentHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class InnstillingerPanel extends Panel {

    private static final JavaScriptResourceReference INNSTILLINGER_PANEL_JS = new JavaScriptResourceReference(
            InnstillingerPanel.class, "InnstillingerPanel.js");

    private static final CssResourceReference INNSTILLINGER_PANEL_CSS = new CssResourceReference(
            InnstillingerPanel.class, "InnstillingerPanel.css");

    private static final String INNLOGGET_CSS_CLASS = "innlogget";

    public InnstillingerPanel(String id) {
        super(id);
        setOutputMarkupId(true);

        add(hasCssClassIf(INNLOGGET_CSS_CLASS, new IsInnlogget()));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(JavaScriptContentHeaderItem.forReference(INNSTILLINGER_PANEL_JS));
        response.render(CssContentHeaderItem.forReference(INNSTILLINGER_PANEL_CSS));
    }

    private class IsInnlogget extends Model<Boolean> {

        public IsInnlogget() {
            super(SecurityContext.getCurrent().getPrincipal() == null ? false : true);
        }

    }

}
