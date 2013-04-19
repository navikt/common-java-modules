package no.nav.sbl.dialogarena.common.innstillinger;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;

public class IndrePanelInnstillinger extends WebMarkupContainer {

    public IndrePanelInnstillinger() {
        super("innstillinger-panel-outer-div");
        Component hoykontrast = new Label("hoykontrast", new ResourceModel("hoykontrast.label"));
        Component skriftstorrelse = new Label("skriftstorrelse", new ResourceModel("skriftstorrelse.label"))
                .add(AttributeAppender.append("title", new ResourceModel("skriftstorrelse.content.label")));

        add(
                hoykontrast,
                skriftstorrelse);
    }
}
