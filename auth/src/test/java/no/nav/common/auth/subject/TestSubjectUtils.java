package no.nav.common.auth.subject;

import java.util.HashMap;
import java.util.Map;

public class TestSubjectUtils {

    public static Subject buildDefault() {
        return builder().build();
    }

    public static SubjectBuilder builder() {
        return new SubjectBuilder();
    }

    public static class SubjectBuilder {

        private String uid = "uid";
        private IdentType identType = IdentType.EksternBruker;
        private SsoToken.Type tokenType = SsoToken.Type.OIDC;
        private String token = "test-sso-token";
        private Map<String, Object> attributes = new HashMap<>();

        public Subject build() {
            return new Subject(uid, identType, new SsoToken(tokenType, token, attributes));
        }

        public SubjectBuilder uid(String uid) {
            this.uid = uid;
            return this;
        }

        public SubjectBuilder identType(IdentType identType) {
            this.identType = identType;
            return this;
        }

        public SubjectBuilder tokenType(SsoToken.Type tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public SubjectBuilder token(String token) {
            this.token = token;
            return this;
        }

        public SubjectBuilder attributes(Map<String, Object> attributes) {
            this.attributes = new HashMap<>(attributes);
            return this;
        }

        public SubjectBuilder token(SsoToken ssoToken) {
            this.token = ssoToken.getToken();
            this.tokenType = ssoToken.getType();
            this.attributes = new HashMap<>(ssoToken.getAttributes());
            return this
                    .token(ssoToken.getToken())
                    .tokenType(ssoToken.getType())
                    .attributes(ssoToken.getAttributes());
        }
    }

}
