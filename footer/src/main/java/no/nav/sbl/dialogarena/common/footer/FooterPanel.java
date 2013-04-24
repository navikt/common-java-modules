package no.nav.sbl.dialogarena.common.footer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import no.nav.modig.wicket.conditional.ConditionalUtils;

import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;

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

    private AbstractReadOnlyModel<Boolean> isInnlogget;

    public FooterPanel(String id, Map<String, String> links, AbstractReadOnlyModel<Boolean> isInnlogget) {
        super(id);
        this.isInnlogget = isInnlogget;

        add(
                new LinkListView("links-footer", createFooterLenker(links)),
                DateLabel.forDatePattern("year", Model.of(new Date()), "yyyy")
        );
    }

    private List<? extends FooterLink> createFooterLenker(Map<String, String> urls) {
        List<FooterLink> links = new ArrayList<>();
        links.add(new FooterLink(urls.get(FOOTER_KONTAKT_URL), "footer.link.kontakt_oss", true));
        links.add(new FooterLink(urls.get(FOOTER_PERSONVERN_URL), "footer.link.personvern_og_sikkerhet", true));
        links.add(new FooterLink(urls.get(FOOTER_FEIL_OG_MANGLER_URL), "footer.link.feil_og_mangler", true));
        links.add(new FooterLink(urls.get(FOOTER_TILGJENGELIGHET_URL), "footer.link.tilgjengelighet", true));
        links.add(new FooterLink(urls.get(FOOTER_NETTSTEDSKART_URL), "footer.link.nettstedskart", false));
        return links;
    }

    private class LinkListView extends ListView<FooterLink> {

        public LinkListView(String id, List<? extends FooterLink> list) {
            super(id, list);
        }

        @Override
        protected void populateItem(ListItem<FooterLink> item) {
            item.add(item.getModelObject());
            item.add(ConditionalUtils.visibleIf(Model.of(item.getModelObject().isVisibleWhenInnlogget() || !isInnlogget.getObject())));
        }

    }

}
