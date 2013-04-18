package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

class TilbakemeldingPanel extends WebMarkupContainer {

    private static final long serialVersionUID = 1L;

    private Label header;
    private Label content;

    public TilbakemeldingPanel(String id, final String applikasjonsnavn) {
        super(id);
        setOutputMarkupPlaceholderTag(true);

        header = new Label("label-tilbakemelding-header", new ResourceModel("label.tilbakemelding.header"));
        content = new Label("label-tilbakemelding-content", new StringResourceModel("label.tilbakemelding.content", this, null, (Object) applikasjonsnavn));

        AjaxLink<Void> close = new AjaxLink<Void>("link-close") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                reset(applikasjonsnavn);
                target.add(getParent());
                target.appendJavaScript("setupJqueryHandlers();");
            }
        };

        add(header, content, close);
    }

    public void success(boolean success) {
        if (success) {
            successs();
        } else {
            failure();
        }
    }

    private void reset(String applikasjonsnavn) {
        add(new AttributeModifier("class", ""));
        header.setDefaultModel(new ResourceModel("label.tilbakemelding.header"));
        content.setDefaultModel(new StringResourceModel("label.tilbakemelding.content", this, null, (Object) applikasjonsnavn));
    }

    private void successs() {
        add(new AttributeAppender("class", " success"));
        header.setDefaultModel(new ResourceModel("label.tilbakemelding.success.header"));
        content.setDefaultModel(Model.of(""));
    }

    private void failure() {
        add(new AttributeAppender("class", " unsuccess"));
        header.setDefaultModel(new ResourceModel("label.tilbakemelding.unsuccess.header"));
        content.setDefaultModel(new ResourceModel("label.tilbakemelding.unsuccess.content"));
    }

}
