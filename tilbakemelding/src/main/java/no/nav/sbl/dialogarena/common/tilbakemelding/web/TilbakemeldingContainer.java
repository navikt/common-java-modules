package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class TilbakemeldingContainer extends Panel {

    public TilbakemeldingContainer(String id, TilbakemeldingService service) {
        super(id);
        setOutputMarkupPlaceholderTag(true);

        final Component tilbakemelding = new TilbakemeldingPanel("panel-tilbakemelding");
        final Component tilbakemeldingExtended = new TilbakemeldingExpandedPanel("panel-tilbakemelding-expanded", Model.of(false), service);

        add(tilbakemelding, tilbakemeldingExtended);
    }

}
