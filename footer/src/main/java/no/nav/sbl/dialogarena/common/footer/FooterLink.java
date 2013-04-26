package no.nav.sbl.dialogarena.common.footer;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

/**
 * Enkel linkklasse som kan skjules
 */
class FooterLink extends ExternalLink {

    private IModel<Boolean> visibleWhenInnlogget;

    public FooterLink(String linkLocation, String resourceKey, boolean visibleWhenInnlogget) {
        super("link-footer", Model.of(linkLocation), new ResourceModel(resourceKey));
        this.visibleWhenInnlogget = Model.of(visibleWhenInnlogget);
    }

    public IModel<Boolean> isVisibleWhenInnlogget() {
        return visibleWhenInnlogget;
    }
}
