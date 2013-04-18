package no.nav.sbl.dialogarena.common.footer;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class FooterPanel extends Panel {

    public FooterPanel(String id) {
        super(id);
        add(
            new ExternalLink("kontakt", new Model<>("#"), new StringResourceModel("footer.link.kontakt_oss", null)),
            new ExternalLink("personvern", new Model<>("#"), new StringResourceModel("footer.link.personvern_og_sikkerhet", null)),
            new ExternalLink("feil", new Model<>("#"), new StringResourceModel("footer.link.feil_og_mangler", null)),
            new ExternalLink("tilgjengelighet", new Model<>("#"), new StringResourceModel("footer.link.tilgjengelighet", null)),
            new ExternalLink("nettstedskart", new Model<>("#"), new StringResourceModel("footer.link.nettstedskart", null))
        );
    }

}
