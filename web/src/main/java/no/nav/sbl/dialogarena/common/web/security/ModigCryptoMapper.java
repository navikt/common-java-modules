package no.nav.sbl.dialogarena.common.web.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.core.request.handler.RequestSettingRequestHandler;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Url;
import org.apache.wicket.util.IProvider;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class ModigCryptoMapper implements IRequestMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModigCryptoMapper.class);

    private final IRequestMapper wrappedMapper;
    private final IProvider<ICrypt> cryptProvider;


    /**
     * Construct.
     *
     * @param wrappedMapper the non-crypted request mapper
     * @param cryptProvider the custom crypt provider
     */
    public ModigCryptoMapper(final IRequestMapper wrappedMapper, final IProvider<ICrypt> cryptProvider) {
        this.wrappedMapper = Args.notNull(wrappedMapper, "wrappedMapper");
        this.cryptProvider = Args.notNull(cryptProvider, "cryptProvider");
    }

    @Override
    public final int getCompatibilityScore(final Request request) {
        return wrappedMapper.getCompatibilityScore(request);
    }

    @Override
    public final Url mapHandler(final IRequestHandler requestHandler) {
        final Url url = wrappedMapper.mapHandler(requestHandler);

        if (url == null) {
            return null;
        }

        if (isExcludePath(url)) {
            return url;
        } else {
            return encryptUrl(url);
        }

    }

    @Override
    public final IRequestHandler mapRequest(final Request request) {

        Url url = request.getUrl();
        if (!isExcludePath(url)) {
            url = decryptUrl(request, url);
        }

        if (url == null) {
            return wrappedMapper.mapRequest(request);
        }

        Request decryptedRequest = request.cloneWithUrl(url);

        IRequestHandler handler = wrappedMapper.mapRequest(decryptedRequest);

        if (handler != null) {
            handler = new RequestSettingRequestHandler(decryptedRequest, handler);
        }

        return handler;
    }

    private boolean isExcludePath(Url url) {
        String urlAsString = url.toString();
        return StringUtils.endsWithAny(urlAsString, new String[] {".js", ".css", ".gif", ".png", ".svg", "selftest"});
    }

    /**
     * @return the {@link ICrypt} implementation that may be used to encrypt/decrypt {@link Url}'s
     *         segments and/or query string
     */
    protected final ICrypt getCrypt() {
        return cryptProvider.get();
    }

    /**
     * @return the wrapped root request mapper
     */
    protected final IRequestMapper getWrappedMapper() {
        return wrappedMapper;
    }

    private Url encryptUrl(final Url url) {
        if (url.getSegments().isEmpty()) {
            return url;
        }
        String encryptedUrlString = getCrypt().encryptUrlSafe(url.toString());

        Url encryptedUrl = new Url(url.getCharset());
        encryptedUrl.getSegments().add(encryptedUrlString);

        int numberOfSegments = url.getSegments().size();
        HashedSegmentGenerator generator = new HashedSegmentGenerator(encryptedUrlString);
        for (int segNo = 0; segNo < numberOfSegments; segNo++) {
            encryptedUrl.getSegments().add(generator.next());
        }
        return encryptedUrl;
    }

    private Url decryptUrl(final Request request, final Url encryptedUrl) {        /*
		 * If the encrypted URL has no segments it is the home page URL,
		 * and does not need decrypting.
		 */
        if (encryptedUrl.getSegments().isEmpty()) {
            return encryptedUrl;
        }

        List<String> encryptedSegments = encryptedUrl.getSegments();

        Url url = new Url(request.getCharset());
        try {
			/*
			 * The first encrypted segment contains an encrypted version of the
			 * entire plain text url.
			 */
            String encryptedUrlString = encryptedSegments.get(0);
            if (Strings.isEmpty(encryptedUrlString)) {
                return null;
            }

            String decryptedUrl = getCrypt().decryptUrlSafe(encryptedUrlString);
            if (decryptedUrl == null) {
                return null;
            }
            Url originalUrl = Url.parse(decryptedUrl, request.getCharset());

            int originalNumberOfSegments = originalUrl.getSegments().size();
            int encryptedNumberOfSegments = encryptedUrl.getSegments().size();

            HashedSegmentGenerator generator = new HashedSegmentGenerator(encryptedUrlString);
            int segNo = 1;
            for (; segNo < encryptedNumberOfSegments; segNo++) {
                if (segNo > originalNumberOfSegments) {
                    break;
                }

                String next = generator.next();
                String encryptedSegment = encryptedSegments.get(segNo);
                if (!next.equals(encryptedSegment)) {
					/*
					 * This segment received from the browser is not the same as the
					 * expected segment generated by the HashSegmentGenerator. Hence it,
					 * and all subsequent segments are considered plain text siblings of the
					 * original encrypted url.
					 */
                    break;
                }

				/*
				 * This segments matches the expected checksum, so we add the corresponding
				 * segment from the original URL.
				 */
                url.getSegments().add(originalUrl.getSegments().get(segNo - 1));
            }
			/*
			 * Add all remaining segments from the encrypted url as plain text segments.
			 */
            for (; segNo < encryptedNumberOfSegments; segNo++) {
                // modified or additional segment
                url.getSegments().add(encryptedUrl.getSegments().get(segNo));
            }

            url.getQueryParameters().addAll(originalUrl.getQueryParameters());
            // WICKET-4923 additional parameters
            url.getQueryParameters().addAll(encryptedUrl.getQueryParameters());
        } catch (Exception e) {
            LOGGER.error("Error decrypting URL", e);
            url = null;
        }

        return url;
    }

    private static class ApplicationCryptProvider implements IProvider<ICrypt> {
        private final Application application;

        public ApplicationCryptProvider(final Application application) {
            this.application = application;
        }

        @Override

        public ICrypt get() {
            return application.getSecuritySettings().getCryptFactory().newCrypt();
        }
    }

    /**
     * A generator of hashed segments.
     */
    private static class HashedSegmentGenerator {
        private char[] characters;

        private int hash = 0;

        public HashedSegmentGenerator(String string) {
            characters = string.toCharArray();
        }

        /**
         * Generate the next segment
         *
         * @return segment
         */
        public String next() {
            char a = characters[Math.abs(hash % characters.length)];
            hash++;
            char b = characters[Math.abs(hash % characters.length)];
            hash++;
            char c = characters[Math.abs(hash % characters.length)];

            String segment = "" + a + b + c;
            hash = hashString(segment);

            segment += String.format("%02x", Math.abs(hash % 256));
            hash = hashString(segment);

            return segment;
        }

        private int hashString(final String str) {
            int lokalHash = 97;

            for (char c : str.toCharArray()) {
                lokalHash = 47 * lokalHash + c;
            }

            return lokalHash;
        }
    }
}