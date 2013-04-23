package no.nav.sbl.dialogarena.common.innstillinger;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssContentHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptContentHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class InnstillingerPanel extends Panel {

    private static final JavaScriptResourceReference INNSTILLINGER_PANEL_JS = new JavaScriptResourceReference(
            InnstillingerPanel.class, "InnstillingerPanel.js");

    private static final CssResourceReference INNSTILLINGER_PANEL_CSS = new CssResourceReference(
            InnstillingerPanel.class, "InnstillingerPanel.css");

    private static final String INNLOGGET_STYLE = "innlogget";
    private static final String IKKE_INNLOGGET_STYLE = "";

    private IModel<String> isInnloggetModel = Model.of(IKKE_INNLOGGET_STYLE);

    public InnstillingerPanel(String id) {
        super(id);
        setOutputMarkupPlaceholderTag(true);
        add(new AttributeAppender("class", isInnloggetModel));
    }

    public final InnstillingerPanel setInnlogget(boolean isInnlogget) {
        isInnloggetModel.setObject(isInnlogget ? INNLOGGET_STYLE : IKKE_INNLOGGET_STYLE);
        return this;
    }

    @Override
    public final void renderHead(IHeaderResponse response) {
        response.render(JavaScriptContentHeaderItem.forReference(INNSTILLINGER_PANEL_JS));
        response.render(CssContentHeaderItem.forReference(INNSTILLINGER_PANEL_CSS));
    }
}