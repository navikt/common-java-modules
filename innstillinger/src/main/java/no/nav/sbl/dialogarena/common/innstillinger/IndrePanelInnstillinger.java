package no.nav.sbl.dialogarena.common.innstillinger;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

public class IndrePanelInnstillinger extends Panel {

    public IndrePanelInnstillinger() {
        super("innstillinger-panel-outer-div");
        Component skriftstorrelse = new Label("skriftstorrelse", new ResourceModel("skriftstorrelse.label"))
                .add(AttributeAppender.append("title", new ResourceModel("skriftstorrelse.content.label")));
        Component hoykontrast = new Label("hoykontrast", new ResourceModel("hoykontrast.label"));

        add(
                skriftstorrelse,
                hoykontrast);
    }
}
