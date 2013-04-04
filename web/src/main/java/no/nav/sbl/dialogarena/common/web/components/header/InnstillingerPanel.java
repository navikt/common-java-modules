package no.nav.sbl.dialogarena.common.web.components.header;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class InnstillingerPanel extends Panel {

    public InnstillingerPanel(String id) {
        super(id);
        add(new Label("label", "innstillinger"));
    }

}
