package no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.context.ApplicationContext;


import javax.inject.Inject;


public class TilbakemeldingPanel extends Panel {

    @SpringBean
    private Epostsender epostsender;

    public TilbakemeldingPanel(String id) {
        super(id);

        final Link actionLink = new Link("actionLink")
        {
            @Override
            public void onClick()
            {
                epostsender.sendEpost("avsender", "mottaker", "emne", "innhold");
            }
        };

        add(actionLink);


    }

}
