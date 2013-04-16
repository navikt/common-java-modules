package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;

import org.apache.wicket.markup.html.panel.Panel;

public class TilbakemeldingContainer extends Panel {

    public TilbakemeldingContainer(String id, TilbakemeldingService service) {
        super(id);
        setOutputMarkupPlaceholderTag(true);

        TilbakemeldingPanel tilbakemeldingPanel = new TilbakemeldingPanel("panel-tilbakemelding");
        TilbakemeldingExpandedPanel tilbakemeldingExpandedPanel = new TilbakemeldingExpandedPanel("panel-tilbakemelding-expanded", service);
        tilbakemeldingPanel.add(tilbakemeldingExpandedPanel);

        add(tilbakemeldingPanel);
    }

}
