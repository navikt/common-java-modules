package no.nav.sbl.dialogarena.common.navigasjon;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;

public class NavigasjonPanel extends Panel {

    public NavigasjonPanel(String id, String url) {
        super(id);
        add(
                new ExternalLink("anchor-logo", url));
    }

}
