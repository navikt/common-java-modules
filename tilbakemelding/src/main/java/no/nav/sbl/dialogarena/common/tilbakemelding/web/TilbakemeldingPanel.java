package no.nav.sbl.dialogarena.common.tilbakemelding.web;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;

class TilbakemeldingPanel extends WebMarkupContainer {

    private static final long serialVersionUID = 1L;

    public TilbakemeldingPanel(String id) {
        super(id);

        add(
                new Label("label-tilbakemelding-header", new ResourceModel("label.tilbakemelding.header")),
                new Label("label-tilbakemelding-content", new ResourceModel("label.tilbakemelding.content"))
        );

        add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                target.add(getParent());
                send(getParent(), Broadcast.DEPTH, null);
            }
        });
    }

}
