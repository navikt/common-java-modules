package no.nav.sbl.dialogarena.common.web.components.header;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

public class InnstillingerPanel extends Panel {

    public InnstillingerPanel(String id) {
        super(id);

        add(
                new Label("skriftstorrelse_content", new StringResourceModel("skriftstorrelse.content.label", this, null).getString()),
                new Label("skriftstorrelse", new StringResourceModel("skriftstorrelse.label", this, null)),
                new Label("hoykontrast", new StringResourceModel("hoykontrast.label", this, null)),
                new Label("innlogging", new StringResourceModel("innlogging.nav.label", this, null))
                );

    }

}
