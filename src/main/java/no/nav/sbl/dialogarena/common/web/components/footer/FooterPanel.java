package no.nav.sbl.dialogarena.common.web.components.footer;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class FooterPanel extends Panel {
    public FooterPanel(String id) {
        super(id);
        add(
            new ExternalLink("kontakt", new Model<String>("#"), new Model<String>("Kontakt oss")),
            new ExternalLink("personvern", new Model<String>("#"), new Model<String>("Personvern og sikkerhet")),
            new ExternalLink("feil", new Model<String>("#"), new Model<String>("Feil og mangler i nav.no")),
            new ExternalLink("tilgjengelighet", new Model<String>("#"), new Model<String>("Tilgjengelighet")),
            new ExternalLink("nyheter", new Model<String>("#"), new Model<String>("Nyheter som RSS")),
            new ExternalLink("nettstedskart", new Model<String>("#"), new Model<String>("Nettstedskart"))
        );
    }
}
