package no.nav.sbl.dialogarena.common.footer;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

class FooterLink extends ExternalLink {

    private boolean visibleWhenInnlogget;

    public FooterLink(String linkLocation, String resourceKey, boolean visibleWhenInnlogget) {
        super("link-footer", Model.of(linkLocation), new ResourceModel(resourceKey));
        this.visibleWhenInnlogget = visibleWhenInnlogget;
    }

    public boolean isVisibleWhenInnlogget() {
        return visibleWhenInnlogget;
    }

}
