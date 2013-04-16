package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

class TilbakemeldingExpandedPanel extends WebMarkupContainer {

    private static final long serialVersionUID = 1L;

    private TilbakemeldingService service;

    public TilbakemeldingExpandedPanel(String id, TilbakemeldingService service) {
        super(id);
        this.service = service;

        add(
                new Label("label-tilbakemelding-expanded-header", new ResourceModel("label.tilbakemelding.expanded.header")),
                new Label("label-tilbakemelding-expanded-content", new ResourceModel("label.tilbakemelding.expanded.content")),
                new TilbakemeldingForm("form-tilbakemelding"));
    }

    private class TilbakemeldingForm extends Form<Void> {

        private IModel<String> tilbakemelding = new Model<>();

        public TilbakemeldingForm(String id) {
            super(id);

            add(
                    new Label("form-tilbakemelding-header", new ResourceModel("form.tilbakemelding.header")),
                    new TextField<>("form-tilbakemelding-input", tilbakemelding),
                    new Button("form-tilbakemelding-submit", new ResourceModel("form.tilbakemelding.submit")),
                    new Label("form-tilbakemelding-abort", new ResourceModel("form.tilbakemelding.abort")));
        }

        @Override
        protected void onSubmit() {
            service.sendTilbakemelding(tilbakemelding.getObject());
        }

    }

}
