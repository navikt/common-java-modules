package no.nav.dialogarena.config.fasit;

import org.junit.Test;

import static no.nav.dialogarena.config.fasit.FasitUtils.DEFAULT_ENVIRONMENT_VARIABLE_NAME;
import static no.nav.dialogarena.config.fasit.FasitUtils.OERA_Q_LOCAL;
import static no.nav.dialogarena.config.fasit.FasitUtils.OERA_T_LOCAL;
import static no.nav.dialogarena.config.fasit.FasitUtils.PREPROD_LOCAL;
import static no.nav.dialogarena.config.fasit.FasitUtils.TEST_LOCAL;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.SBS;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDefaultDomain;
import static no.nav.dialogarena.config.fasit.TestEnvironment.Q6;
import static no.nav.dialogarena.config.fasit.TestEnvironment.T6;
import static no.nav.sbl.dialogarena.test.SystemProperties.setTemporaryProperty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;

public class FasitUtilsTest {

    @Test
    public void getEnvironmentClass_() {
        assertThat(FasitUtils.getEnvironmentClass("t6"), equalTo("t"));
        assertThat(FasitUtils.getEnvironmentClass("t"), equalTo("t"));
        assertThat(FasitUtils.getEnvironmentClass("q6"), equalTo("q"));
    }

    @Test
    public void getOeraLocal_() {
        assertThat(FasitUtils.getOeraLocal("t6"), equalTo("oera-t.local"));
        assertThat(FasitUtils.getOeraLocal("t"), equalTo("oera-t.local"));
        assertThat(FasitUtils.getOeraLocal("q6"), equalTo("oera-q.local"));
    }

    @Test
    public void getFSSLocal() {
        assertThat(FasitUtils.getFSSLocal("t6"), equalTo("test.local"));
        assertThat(FasitUtils.getFSSLocal("t"), equalTo("test.local"));
        assertThat(FasitUtils.getFSSLocal("q6"), equalTo("preprod.local"));
    }

    @Test
    public void erEksterntDomene_() {
        assertThat(FasitUtils.erEksterntDomene("adeo.no"), is(false));
        assertThat(FasitUtils.erEksterntDomene("test.local"), is(false));
        assertThat(FasitUtils.erEksterntDomene("devillo.no"), is(false));
        assertThat(FasitUtils.erEksterntDomene("preprod.local"), is(false));

        assertThat(FasitUtils.erEksterntDomene("oera.no"), is(true));
        assertThat(FasitUtils.erEksterntDomene("oera-q.local"), is(true));
        assertThat(FasitUtils.erEksterntDomene("oera-t.local"), is(true));
    }

    public static void testServiceUserCertificate(ServiceUserCertificate certificate) {
        assertThat(certificate.getKeystorealias(), isOneOf("host-key", "app-key"));

        assertThat(certificate.getKeystorepassword(), notNullValue());
        assertThat(certificate.getKeystorepassword().length(), greaterThan(0));

        assertThat(certificate.getKeystore(), notNullValue());
        assertThat(certificate.getKeystore().length, greaterThan(1000));
    }

    @Test
    public void getDefaultDomain_() throws Exception {
        setTemporaryProperty(DEFAULT_ENVIRONMENT_VARIABLE_NAME, T6.toString(),()->{
            assertThat(getDefaultDomain(SBS),equalTo(OERA_T_LOCAL));
            assertThat(getDefaultDomain(FSS),equalTo(TEST_LOCAL));
        });

        setTemporaryProperty(DEFAULT_ENVIRONMENT_VARIABLE_NAME, Q6.toString(),()->{
            assertThat(getDefaultDomain(SBS),equalTo(OERA_Q_LOCAL));
            assertThat(getDefaultDomain(FSS),equalTo(PREPROD_LOCAL));
        });
    }
}