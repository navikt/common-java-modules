package no.nav.apiapp.security;

import no.nav.apiapp.util.SubjectUtils;
import no.nav.brukerdialog.security.domain.IdentType;

import java.util.Optional;

// Denne kan brukes som en dependency for å gjøre det lettere å teste
public class SubjectService {

    public Optional<IdentType> getIdentType() {
        return SubjectUtils.getIdentType();
    }

    public Optional<String> getUserId() {
        return SubjectUtils.getUserId();
    }

}
