package no.nav.sbl.dialogarena.common.web.showcase;

import no.nav.sbl.dialogarena.common.footer.FooterPanel;
import no.nav.sbl.dialogarena.common.web.components.header.InnstillingerPanel;
import no.nav.sbl.dialogarena.common.web.components.header.NavigasjonPanel;
import org.apache.wicket.markup.html.WebPage;

public class ShowcasePage extends WebPage {

    public ShowcasePage() {
        add(new InnstillingerPanel("innstillinger"));
        add(new NavigasjonPanel("navigasjon"));
        add(new FooterPanel("footer"));
    }

}
