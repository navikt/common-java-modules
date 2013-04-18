package no.nav.sbl.dialogarena.common.innstillinger;

import org.apache.wicket.markup.html.panel.Panel;

public class InnstillingerPanel extends Panel {

    private IndrePanelInnstillinger outerDiv;

    public InnstillingerPanel(String id) {
        super(id);
        outerDiv = new IndrePanelInnstillinger();
        add(outerDiv);
    }

    public IndrePanelInnstillinger getOuterDiv() {
        return outerDiv;
    }
}
