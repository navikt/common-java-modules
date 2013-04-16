package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

class TilbakemeldingPanel extends WebMarkupContainer {

    private static final long serialVersionUID = 1L;

    public TilbakemeldingPanel(String id, String applikasjonsnavn) {
        super(id);

        add(
                new Label("label-tilbakemelding-header", new ResourceModel("label.tilbakemelding.header")),
                new Label("label-tilbakemelding-content", new StringResourceModel("label.tilbakemelding.content", this, null, (Object) applikasjonsnavn))
        );
    }

}
