package no.nav.sbl.dialogarena.common.web.components.header;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

public class InnstillingerPanel extends Panel {

    public InnstillingerPanel(String id) {
        super(id);

        Component skriftstorrelse = new Label("skriftstorrelse", new ResourceModel("skriftstorrelse.label"))
                .add(AttributeAppender.append("title", new ResourceModel("skriftstorrelse.content.label")));
        Component hoykontrast = new Label("hoykontrast", new ResourceModel("hoykontrast.label"));

        add(skriftstorrelse, hoykontrast);
    }

}
