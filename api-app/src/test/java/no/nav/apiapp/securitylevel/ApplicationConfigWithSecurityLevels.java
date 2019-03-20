package no.nav.apiapp.securitylevel;

import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.fo.apiapp.ApplicationConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.common.auth.SecurityLevel.*;

@Configuration
@Import({
        SecurityLevelsService.class
})
public class ApplicationConfigWithSecurityLevels extends ApplicationConfig {

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {

        apiAppConfigurator
                .validateAzureAdExternalUserTokens(Level3)
                .customSecurityLevelForExternalUsers(Level4, "level4")
                .customSecurityLevelForExternalUsers(Level2, "level2");
    }
}
