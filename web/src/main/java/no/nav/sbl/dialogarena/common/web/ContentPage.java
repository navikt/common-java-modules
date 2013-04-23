package no.nav.sbl.dialogarena.common.web;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.WebPage;

import java.util.HashMap;
import java.util.Map;

public class ContentPage extends WebPage {

    private Map<String, Component> components = new HashMap<String, Component>();

    public ContentPage() {
        setOutputMarkupPlaceholderTag(true);
    }

    @Override
    public MarkupContainer add(Component... childs) {
        for(Component component : childs) {
            components.put(component.getId(), component);
        }
        return super.add(childs);
    }

    public final void changeVisibility(String id) {
        Component component = components.get(id);
        if (component.isVisible()) {
            component.setVisible(false);
        } else {
            component.setVisible(true);
        }
    }

    public final Component getComponent(String id) {
        return components.get(id);
    }

}
