package no.nav.common.auth.context;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import org.junit.Test;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AuthContextHolderTest {

    @Test
    public void should_return_parsed_token_string_when_built_from_scratch() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("subject")
                .audience("audience")
                .issuer("issuer")
                .build();

        AuthContextHolderThreadLocal.instance().withContext(new AuthContext(UserRole.EKSTERN, new PlainJWT(claimsSet)), () -> {
            assertTrue(AuthContextHolderThreadLocal.instance().getIdTokenString().isPresent());
        });
    }

    @Test
    public void should_return_parsed_token_string_when_parsed_from_string() throws ParseException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("subject")
                .audience("audience")
                .issuer("issuer")
                .build();

        JWT jwt = JWTParser.parse(new PlainJWT(claimsSet).serialize());

        AuthContextHolderThreadLocal.instance().withContext(new AuthContext(UserRole.EKSTERN, jwt), () -> {
            assertTrue(AuthContextHolderThreadLocal.instance().getIdTokenString().isPresent());
        });
    }

    @Test
    public void withSubjectProvider() {
        AuthContextHolderThreadLocal.instance().withContext(null, this::assertNoContext);
    }

    @Test
    public void withSubject__no_leakage_to_otherThreads_or_contexts() {
        AuthContext authContext = newAuthContext("uid");

        assertNoContext();

        AuthContextHolderThreadLocal.instance().withContext(authContext, () -> {
            assertEquals(authContext, AuthContextHolderThreadLocal.instance().requireContext());

            // fork-join thread pool
            Set<Thread> usedThreads = new HashSet<>();
            Thread baseThread = Thread.currentThread();
            IntStream.range(0, 50)
                    .parallel()
                    .forEach((i) -> {
                        try {
                            // prevent reuse of main thread
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Thread thread = Thread.currentThread();
                        usedThreads.add(thread);
                        if (thread == baseThread) {
                            assertEquals(authContext, AuthContextHolderThreadLocal.instance().requireContext());
                        } else {
                            assertNoContext();
                        }
                    });
            assertThat(usedThreads.size()).isGreaterThan(1);

            // child thread
            AtomicReference<AuthContext> contextWrapper = new AtomicReference<>();
            Thread thread = new Thread(() -> AuthContextHolderThreadLocal.instance().getContext().ifPresent(contextWrapper::set));
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assertThat(contextWrapper.get()).isNull();

            // empty child context
            AuthContextHolderThreadLocal.instance().withContext(null, this::assertNoContext);

            // populated child context
            AuthContext otherContext = newAuthContext("other");
            AuthContextHolderThreadLocal.instance().withContext(otherContext, () -> {
                assertEquals(otherContext, AuthContextHolderThreadLocal.instance().requireContext());
            });

            // failing child context
            assertThatThrownBy(() -> {
                AuthContextHolderThreadLocal.instance().withContext(otherContext, () -> {
                    throw new Error();
                });
            }).isInstanceOf(Error.class);

            assertEquals(authContext, AuthContextHolderThreadLocal.instance().requireContext());
        });

        assertNoContext();
    }

    private AuthContext newAuthContext(String subject) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .build();

        JWT jwt = new PlainJWT(claimsSet);

        return new AuthContext(UserRole.EKSTERN, jwt);
    }

    private void assertNoContext() {
        assertTrue(AuthContextHolderThreadLocal.instance().getContext().isEmpty());
    }

}
