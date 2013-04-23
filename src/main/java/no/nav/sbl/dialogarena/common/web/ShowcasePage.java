package no.nav.sbl.dialogarena.common.web;

import no.nav.sbl.dialogarena.common.footer.FooterPanel;
import no.nav.sbl.dialogarena.common.innstillinger.InnstillingerPanel;
import no.nav.sbl.dialogarena.common.navigasjon.NavigasjonPanel;
import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;
import no.nav.sbl.dialogarena.common.tilbakemelding.web.TilbakemeldingContainer;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.InlineFrame;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ShowcasePage extends WebPage {

    @SpringBean
    private TilbakemeldingService tilbakemeldingService;

    private ContentPage contentPage = new ContentPage();
    private InlineFrame inlineFrame = new InlineFrame("iframe", contentPage);

    public ShowcasePage(PageParameters parameters) {
        super(parameters);

        inlineFrame.setOutputMarkupPlaceholderTag(true);

        add(inlineFrame);

        addShowcaseComponent(
                new InnstillingerPanel("innstillinger"),
                new NavigasjonPanel("navigasjon", "www.nav.no"),
                new FooterPanel("footer"),
                new TilbakemeldingContainer("tilbakemelding", "Din profil", tilbakemeldingService));
    }

    private void addShowcaseComponent(Component... components) {
        for (Component component : components) {
            addShowcaseComponent(component);
        }
    }

    private void addShowcaseComponent(Component component) {
        contentPage.add(component);
        component.setVisible(false);
        component.setOutputMarkupPlaceholderTag(true);
        add(new SimpleAjaxLink(component.getId()));
    }

    private class SimpleAjaxLink extends AjaxLink<Void> {

        public SimpleAjaxLink(String id) {
            super(id);
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            contentPage.changeVisibility(getId());
            target.add(inlineFrame);
        }

    }

}
