package no.nav.sbl.dialogarena.tilbakemelding;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;


public class TilbakemeldingPanel extends Panel {

    public TilbakemeldingPanel(String id) {
        super(id);

        add(
                new Label("tilbakemelding_label", "tilbakemelding")

        );

    }

}
