package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import no.nav.sbl.dialogarena.common.tilbakemelding.service.TilbakemeldingService;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TilbakemeldingExpandedPanel extends WebMarkupContainer {

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TilbakemeldingService service;
    private TilbakemeldingPanel tilbakemeldingPanel;

    public TilbakemeldingExpandedPanel(String id, TilbakemeldingService service, TilbakemeldingPanel tilbakemeldingPanel) {
        super(id);
        this.service = service;
        this.tilbakemeldingPanel = tilbakemeldingPanel;

        add(
                new Label("label-tilbakemelding-expanded-header", new ResourceModel("label.tilbakemelding.expanded.header")),
                new Label("label-tilbakemelding-expanded-content", new ResourceModel("label.tilbakemelding.expanded.content")),
                new TilbakemeldingForm("form-tilbakemelding", Model.of("")));
    }

    private class TilbakemeldingForm extends Form<String> {

        public TilbakemeldingForm(String id, IModel<String> tilbakemelding) {
            super(id, tilbakemelding);

            AjaxButton ajaxButton = new AjaxButton("form-tilbakemelding-submit", new ResourceModel("form.tilbakemelding.submit"), this) {

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    boolean success = true;
                    try {
                        service.sendTilbakemelding((String) form.getDefaultModelObject());
                    } catch (Exception e) {
                        logger.error("Error while sending tilbakemelding", e);
                        success = false;
                    }
                    target.add(tilbakemeldingPanel);
                    tilbakemeldingPanel.success(success);
                }

            };

            add(
                    ajaxButton,
                    new Label("form-tilbakemelding-header", new ResourceModel("form.tilbakemelding.header")),
                    new TextArea<>("form-tilbakemelding-input", tilbakemelding),
                    new Label("form-tilbakemelding-abort", new ResourceModel("form.tilbakemelding.abort")));

        }

    }

}
