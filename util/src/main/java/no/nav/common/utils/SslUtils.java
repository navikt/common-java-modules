package no.nav.common.utils;

import static no.nav.common.utils.EnvironmentUtils.Type.PUBLIC;
import static no.nav.common.utils.EnvironmentUtils.Type.SECRET;
import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.common.utils.EnvironmentUtils.setProperty;

public class SslUtils {

    public static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";
    public static final String JAVAX_NET_SSL_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    public static final String NAV_TRUSTSTORE_PATH = "NAV_TRUSTSTORE_PATH";
    public static final String NAV_TRUSTSTORE_PASSWORD = "NAV_TRUSTSTORE_PASSWORD";

    public static void setupTruststore() {
        getOptionalProperty(NAV_TRUSTSTORE_PATH).ifPresent(path -> setProperty(JAVAX_NET_SSL_TRUST_STORE, path, PUBLIC));
        getOptionalProperty(NAV_TRUSTSTORE_PASSWORD).ifPresent(passwd -> setProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD, passwd, SECRET));
    }

}
