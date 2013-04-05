package no.nav.sbl.dialogarena.common.web.components.header;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public class NavigasjonPanel extends Panel {

    public NavigasjonPanel(String id) {
        super(id);
        add(new Label("label", "{navigasjon}"));
    }

}
