package no.nav.sbl.dialogarena.common.web;

import no.nav.sbl.dialogarena.common.footer.FooterPanel;
import no.nav.sbl.dialogarena.common.innstillinger.InnstillingerPanel;
import no.nav.sbl.dialogarena.common.navigasjon.NavigasjonPanel;
import no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding.TilbakemeldingPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;

public class ShowcasePage extends WebPage {

    public ShowcasePage(PageParameters parameters) {
        super(parameters);
        add(new InnstillingerPanel("innstillinger"));
        add(new NavigasjonPanel("navigasjon"));
        add(new FooterPanel("footer"));
        add(new TilbakemeldingPanel("tilbakemelding"));

    }

}
