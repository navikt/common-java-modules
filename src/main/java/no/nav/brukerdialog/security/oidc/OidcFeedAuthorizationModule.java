package no.nav.brukerdialog.security.oidc;

import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.feed.common.FeedAuthorizationModule;

import java.util.List;

import static no.nav.brukerdialog.tools.Utils.getSystemProperty;
import static no.nav.brukerdialog.tools.Utils.getCommaSeparatedUsers;

public class OidcFeedAuthorizationModule implements FeedAuthorizationModule {

    @Override
    public boolean isRequestAuthorized(String feedname) {
        String username = SubjectHandler.getSubjectHandler().getUid().toLowerCase();
        String allowedUsersString = getSystemProperty(feedname+ ".feed.brukertilgang");
        List<String> allowedUsers = getCommaSeparatedUsers(allowedUsersString);
        return allowedUsers.contains(username);
    }
}
