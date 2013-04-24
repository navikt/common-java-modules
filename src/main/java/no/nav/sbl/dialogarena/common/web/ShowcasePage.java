package no.nav.sbl.dialogarena.common.web;

import java.util.HashMap;

import no.nav.sbl.dialogarena.common.footer.FooterPanel;
import no.nav.sbl.dialogarena.common.innstillinger.InnstillingerPanel;
import no.nav.sbl.dialogarena.common.navigasjon.NavigasjonPanel;
import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;
import no.nav.sbl.dialogarena.common.tilbakemelding.web.TilbakemeldingContainer;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.InlineFrame;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.HashMap;

public class ShowcasePage extends WebPage {

    @SpringBean
    private TilbakemeldingService tilbakemeldingService;

    private ContentPage contentPage = new ContentPage();
    private InlineFrame inlineFrame = new InlineFrame("iframe", contentPage);

    public static boolean isInnlogget = false;

    public ShowcasePage(PageParameters parameters) {
        super(parameters);

        inlineFrame.setOutputMarkupPlaceholderTag(true);

        add(inlineFrame);

        add(new AjaxLink<Void>("trigger-innlogging") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                isInnlogget = isInnlogget ? false : true;
                target.add(inlineFrame, this);
            }
        });

        addShowcaseComponent(
                new InnstillingerPanel("innstillinger", new IsInnlogget()),
                new NavigasjonPanel("navigasjon", "www.nav.no"),
                new FooterPanel("footer", new HashMap<String, String>(), new IsInnlogget()),
                new TilbakemeldingContainer("tilbakemelding", "Din profil", tilbakemeldingService));
    }

    private class IsInnlogget extends AbstractReadOnlyModel<Boolean> {

        @Override
        public Boolean getObject() {
            return isInnlogget;
        }

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
