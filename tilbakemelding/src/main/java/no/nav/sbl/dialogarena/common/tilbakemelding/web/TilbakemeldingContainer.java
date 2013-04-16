package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;

public class TilbakemeldingContainer extends Panel {

    public TilbakemeldingContainer(String id, String applikasjonsnavn, TilbakemeldingService service) {
        super(id);

        final Component tilbakemelding = new TilbakemeldingPanel("panel-tilbakemelding", applikasjonsnavn);
        final Component tilbakemeldingExpanded = new TilbakemeldingExpandedPanel("panel-tilbakemelding-expanded", service);

        add(tilbakemelding, tilbakemeldingExpanded);
    }

}
