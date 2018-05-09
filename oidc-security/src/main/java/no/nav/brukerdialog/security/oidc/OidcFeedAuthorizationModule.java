package no.nav.brukerdialog.security.oidc;

import no.nav.common.auth.Subject;
import no.nav.fo.feed.common.FeedAuthorizationModule;

import java.util.List;

import static no.nav.brukerdialog.tools.Utils.getCommaSeparatedUsers;
import static no.nav.brukerdialog.tools.Utils.getSystemProperty;

public class OidcFeedAuthorizationModule implements FeedAuthorizationModule {

    @Override
    public boolean isRequestAuthorized(String feedname) {
        return no.nav.common.auth.SubjectHandler.getSubject().map(Subject::getUid).map(String::toLowerCase).map(username -> {
            String allowedUsersString = getSystemProperty(feedname + ".feed.brukertilgang");
            List<String> allowedUsers = getCommaSeparatedUsers(allowedUsersString);
            return allowedUsers.contains(username);
        }).orElse(false);
    }
}
