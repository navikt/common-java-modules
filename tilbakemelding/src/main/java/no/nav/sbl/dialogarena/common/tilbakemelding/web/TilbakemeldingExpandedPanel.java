package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import no.nav.modig.wicket.conditional.ConditionalUtils;
import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;

import org.apache.wicket.event.IEvent;
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

    public TilbakemeldingExpandedPanel(String id, IModel<Boolean> isVisible, TilbakemeldingService service) {
        super(id, isVisible);
        this.service = service;

        add(ConditionalUtils.visibleIf(isVisible));

        add(
                new Label("label-tilbakemelding-expanded-header", new ResourceModel("label.tilbakemelding.expanded.header")),
                new Label("label-tilbakemelding-expanded-content", new ResourceModel("label.tilbakemelding.expanded.content")),
                new TilbakemeldingForm("form-tilbakemelding"));
    }

    @Override
    public void onEvent(IEvent<?> event) {
        if (event.getSource() instanceof TilbakemeldingPanel) {
            changeVisibility();
        }
    }

    private void changeVisibility() {
        Boolean isVisible = (Boolean) getDefaultModelObject();
        setDefaultModelObject(isVisible ? false : true);
    }

    private class TilbakemeldingForm extends Form<Void> {

        private IModel<String> tilbakemelding = new Model<>();

        public TilbakemeldingForm(String id) {
            super(id);

            add(
                    new Label("form-tilbakemelding-header", new ResourceModel("form.tilbakemelding.header")),
                    new TextField<>("form-tilbakemelding-input", tilbakemelding),
                    new Button("form-tilbakemelding-submit", new ResourceModel("form.tilbakemelding.submit")));
        }

        @Override
        protected void onSubmit() {
            service.sendTilbakemelding(tilbakemelding.getObject());
        }

    }

}
