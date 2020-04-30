package no.nav.common.cxf.saml;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.IdentType;
import org.apache.wss4j.common.saml.OpenSAMLBootstrap;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.GlobalParserPoolInitializer;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;

import static org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport.getMarshallerFactory;

// Simplified version of http://stash.devillo.no/projects/UTSTOTT/repos/saml-gen/browse/src/main/java/no/nav/modig/samlgen/AssertionBuilder.java
@Slf4j
public class AssertionBuilder {

    public static final String NAME_QUALIFIER = "www.nav.no";
    public static final String ID = "1231231231231231321";

    private static final XMLObjectBuilderFactory builderFactory;

    static {
        builderFactory = setup();
    }

    @SneakyThrows
    private static XMLObjectBuilderFactory setup() {
        OpenSAMLBootstrap.bootstrap();
        new GlobalParserPoolInitializer().init();
        return XMLObjectProviderRegistrySupport.getBuilderFactory();
    }

    public static Assertion getSamlAssertionForUsername(Parameters parameters) {
        log.info("creating saml assertion with paramters: {}", parameters);

        NameID nameId = getNameIdentifier(parameters.username);

        SubjectConfirmation subjectConfirmation = createSubjectConfirmation();

        Subject subject = createSubject(nameId, subjectConfirmation);

        // Builder Attributes
        SAMLObjectBuilder attrStatementBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);
        AttributeStatement attrStatement = (AttributeStatement) attrStatementBuilder.buildObject();

        attrStatement.getAttributes().add(buildStringAttribute(SamlUtils.AUTHENTICATION_LEVEL, Integer.toString(parameters.authenticationLevel)));
        attrStatement.getAttributes().add(buildStringAttribute(SamlUtils.IDENT_TYPE, parameters.identType.name()));
        attrStatement.getAttributes().add(buildStringAttribute(SamlUtils.CONSUMER_ID, parameters.consumerId));

        // Create not before or after condition
        SAMLObjectBuilder conditionsBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(Conditions.DEFAULT_ELEMENT_NAME);
        Conditions conditions = (Conditions) conditionsBuilder.buildObject();

        Issuer issuer = createIssuer(parameters);

        return createAssertion(subject, attrStatement, conditions, issuer);

    }

    @SneakyThrows
    private static Assertion createAssertion(Subject subject, AttributeStatement attrStatement, Conditions conditions, Issuer issuer) {
        SAMLObjectBuilder assertionBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
        Assertion assertion = (Assertion) assertionBuilder.buildObject();
        assertion.setIssuer(issuer);
        assertion.setVersion(SAMLVersion.VERSION_20);

        assertion.setSubject(subject);
        assertion.getAttributeStatements().add(attrStatement);
        assertion.setConditions(conditions);
        assertion.setID(ID);

        assertion.setDOM(getMarshallerFactory().getMarshaller(assertion).marshall(assertion));
        return assertion;
    }

    private static Issuer createIssuer(Parameters parameters) {
        SAMLObjectBuilder issuerBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
        Issuer issuer = (Issuer) issuerBuilder.buildObject();
        issuer.setValue(parameters.issuer);
        return issuer;
    }

    private static Subject createSubject(NameID nameId, SubjectConfirmation subjectConfirmation) {
        SAMLObjectBuilder subjectBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
        Subject subject = (Subject) subjectBuilder.buildObject();
        subject.setNameID(nameId);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        return subject;
    }

    private static SubjectConfirmation createSubjectConfirmation() {
        SAMLObjectBuilder subjectConfirmationBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        SubjectConfirmation subjectConfirmation = (SubjectConfirmation) subjectConfirmationBuilder.buildObject();
        subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");
        return subjectConfirmation;
    }

    private static NameID getNameIdentifier(String username) {
        SAMLObjectBuilder nameIdBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
        NameID nameId = (NameID) nameIdBuilder.buildObject();
        nameId.setValue(username);
        nameId.setNameQualifier(NAME_QUALIFIER);
        nameId.setFormat(NameID.UNSPECIFIED);
        return nameId;
    }

    private static Attribute buildStringAttribute(String name, String value) {
        SAMLObjectBuilder attrBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(Attribute.DEFAULT_ELEMENT_NAME);
        Attribute attribute = (Attribute) attrBuilder.buildObject();
        attribute.setName(name);
        attribute.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:uri");

        // Set custom Attributes
        XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
        XSString attrValue = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrValue.setValue(value);

        attribute.getAttributeValues().add(attrValue);
        return attribute;
    }

    @Builder
    @Value
    public static class Parameters {

        public String username;
        public String issuer;
        public int authenticationLevel;
        public IdentType identType;
        public String consumerId;

    }

}