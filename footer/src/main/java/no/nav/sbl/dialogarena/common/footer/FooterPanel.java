package no.nav.sbl.dialogarena.common.footer;

import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.Date;
import java.util.Map;

/**
 * Felleskomponent som bl.a. brukes på forside i inngangsporten.
 *
 * Statiske public fields gjør det mulig å sikre at nøkkel er lik ved innstansiering
 *
 */
public class FooterPanel extends Panel {
    public static final String FOOTER_KONTAKT_URL = "footer.kontakt.url";
    public static final String FOOTER_PERSONVERN_URL = "footer.personvern.url";
    public static final String FOOTER_FEIL_OG_MANGLER_URL = "footer.feilOgMangler.url";
    public static final String FOOTER_TILGJENGELIGHET_URL = "footer.tilgjengelighet.url";
    public static final String FOOTER_NETTSTEDSKART_URL = "footer.nettstedskart.url";

    public FooterPanel(String id, Map<String, String> links) {
        super(id);
        add(
            new ExternalLink("kontakt", new Model<>(links.get(FOOTER_KONTAKT_URL)), new StringResourceModel("footer.link.kontakt_oss", null)),
            new ExternalLink("personvern", new Model<>(links.get(FOOTER_PERSONVERN_URL)), new StringResourceModel("footer.link.personvern_og_sikkerhet", null)),
            new ExternalLink("feil", new Model<>(links.get(FOOTER_FEIL_OG_MANGLER_URL)), new StringResourceModel("footer.link.feil_og_mangler", null)),
            new ExternalLink("tilgjengelighet", new Model<>(links.get(FOOTER_TILGJENGELIGHET_URL)), new StringResourceModel("footer.link.tilgjengelighet", null)),
            new ExternalLink("nettstedskart", new Model<>(links.get(FOOTER_NETTSTEDSKART_URL)), new StringResourceModel("footer.link.nettstedskart", null)),
            DateLabel.forDatePattern("year", Model.of(new Date()), "yyyy")
        );
    }

}
