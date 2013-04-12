package no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;


public class TilbakemeldingPanel extends Panel {

    @SpringBean
    private Epostsender epostsender;

    public TilbakemeldingPanel(String id) {
        super(id);

        final Link<?> actionLink = new Link<Void>("actionLink") {
            @Override
            public void onClick() {
                epostsender.sendEpost("avsender", "mottaker", "emne", "innhold");
            }
        };

        add(actionLink);


    }

}
