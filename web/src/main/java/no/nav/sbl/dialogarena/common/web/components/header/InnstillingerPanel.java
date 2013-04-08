package no.nav.sbl.dialogarena.common.web.components.header;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

public class InnstillingerPanel extends Panel {

    public InnstillingerPanel(String id) {
        super(id);

        add(
                new Label("skriftstorrelse_content", new ResourceModel("skriftstorrelse.content.label")),
                new Label("skriftstorrelse", new ResourceModel("skriftstorrelse.label")),
                new Label("hoykontrast", new ResourceModel("hoykontrast.label"))
                );

    }

}
