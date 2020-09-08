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

        AuthContextHolder.withContext(new AuthContext(UserRole.EKSTERN, new PlainJWT(claimsSet)), () -> {
            assertTrue(AuthContextHolder.getIdTokenString().isPresent());
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

        AuthContextHolder.withContext(new AuthContext(UserRole.EKSTERN, jwt), () -> {
            assertTrue(AuthContextHolder.getIdTokenString().isPresent());
        });
    }

    @Test
    public void withSubjectProvider() {
        AuthContextHolder.withContext(null, this::assertNoContext);
    }

    @Test
    public void withSubject__no_leakage_to_otherThreads_or_contexts() {
        AuthContext authContext = newAuthContext("uid");

        assertNoContext();

        AuthContextHolder.withContext(authContext, () -> {
            assertEquals(authContext, AuthContextHolder.requireContext());

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
                            assertEquals(authContext, AuthContextHolder.requireContext());
                        } else {
                            assertNoContext();
                        }
                    });
            assertThat(usedThreads.size()).isGreaterThan(1);

            // child thread
            AtomicReference<AuthContext> contextWrapper = new AtomicReference<>();
            Thread thread = new Thread(() -> AuthContextHolder.getContext().ifPresent(contextWrapper::set));
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assertThat(contextWrapper.get()).isNull();

            // empty child context
            AuthContextHolder.withContext(null, this::assertNoContext);

            // populated child context
            AuthContext otherContext = newAuthContext("other");
            AuthContextHolder.withContext(otherContext, () -> {
                assertEquals(otherContext, AuthContextHolder.requireContext());
            });

            // failing child context
            assertThatThrownBy(() -> {
                AuthContextHolder.withContext(otherContext, () -> {
                    throw new Error();
                });
            }).isInstanceOf(Error.class);

            assertEquals(authContext, AuthContextHolder.requireContext());
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
        assertTrue(AuthContextHolder.getContext().isEmpty());
    }

}
