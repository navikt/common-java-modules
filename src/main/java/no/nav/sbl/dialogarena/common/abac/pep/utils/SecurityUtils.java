package no.nav.sbl.dialogarena.common.abac.pep.utils;

import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.modig.core.context.SAMLAssertionCredential;

import javax.security.auth.Subject;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class SecurityUtils {

    public static Optional<String> getIdent() {
        return tokenFromSubject(SecurityUtils::getIdent);
    }

    private static Optional<String> getIdent(Subject subject) {
        return subject.getPrincipals()
                .stream()
                .filter(p -> p instanceof no.nav.brukerdialog.security.domain.SluttBruker || p instanceof no.nav.modig.core.domain.SluttBruker)
                .map(Principal::getName)
                .findAny();
    }

    public static Optional<String> getSamlToken() {
        return tokenFromSubject(SecurityUtils::getSamlToken);
    }

    private static Optional<String> getSamlToken(Subject subject) {
        return subject.getPublicCredentials(SAMLAssertionCredential.class)
                .stream()
                .map(SecurityUtils::getTokenFromAssertion)
                .findAny();
    }

    public static Optional<String> getOidcToken() {
        return tokenFromSubject(SecurityUtils::getOidcToken);
    }

    private static Optional<String> getOidcToken(Subject subject) {
        return subject.getPublicCredentials()
                .stream()
                .filter(o -> o instanceof OidcCredential)
                .map(o -> (OidcCredential) o)
                .findFirst()
                .map(OidcCredential::getToken)
                .map(SecurityUtils::extractOidcTokenBody)
                ;
    }

    private static Optional<String> tokenFromSubject(Function<Subject, Optional<String>> getString) {
        return ofNullable(ofNullable(SubjectHandler.getSubjectHandler().getSubject()).orElse(
                no.nav.modig.core.context.SubjectHandler.getSubjectHandler().getSubject())
        ).flatMap(getString);
    }

    public static String extractOidcTokenBody(String oidcTokenBody) {
        final String[] tokenParts = oidcTokenBody.split("\\.");
        return tokenParts.length == 1 ? tokenParts[0] : tokenParts[1];
    }

    @SneakyThrows
    private static String getTokenFromAssertion(SAMLAssertionCredential samlAssertionCredential) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(samlAssertionCredential.getElement()), new StreamResult(outputStream));
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

}
