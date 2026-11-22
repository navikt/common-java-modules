package no.nav.common.auth.context;

import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.Test;

public class AuthContextHolderTest {

    @Test
    public void should_return_parsed_token_string_when_built_from_scratch() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("subject")
                .audience("audience")
                .issuer("issuer")
                .build();

        AuthContextHolderThreadLocal.instance()
                .withContext(
                        new AuthContext(UserRole.EKSTERN, new PlainJWT(claimsSet)),
                        () -> assertTrue(AuthContextHolderThreadLocal.instance()
                                .getIdTokenString()
                                .isPresent()));
    }

    @Test
    public void should_return_parsed_token_string_when_parsed_from_string() throws ParseException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("subject")
                .audience("audience")
                .issuer("issuer")
                .build();

        JWT jwt = JWTParser.parse(new PlainJWT(claimsSet).serialize());

        AuthContextHolderThreadLocal.instance()
                .withContext(
                        new AuthContext(UserRole.EKSTERN, jwt),
                        () -> assertTrue(AuthContextHolderThreadLocal.instance()
                                .getIdTokenString()
                                .isPresent()));
    }

    @Test
    public void withSubjectProvider() {
        AuthContextHolderThreadLocal.instance().withContext(null, this::assertNoContext);
    }

    @Test
    public void lekkerIkkeKontekstTilAndreTraderEllerNestedeKontekster() {
        AuthContext authContext = newAuthContext("uid");

        assertNoContext();

        AuthContextHolderThreadLocal.instance().withContext(authContext, () -> {
            assertEquals(authContext, AuthContextHolderThreadLocal.instance().requireContext());

            Set<Thread> usedThreads = ConcurrentHashMap.newKeySet();
            Thread baseThread = Thread.currentThread();
            @SuppressWarnings("resource")
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                List<Future<?>> oppgaver = new ArrayList<>();
                for (int i = 0; i < 50; i++) {
                    oppgaver.add(executor.submit(() -> {
                        usedThreads.add(Thread.currentThread());
                        assertNoContext();
                    }));
                }
                executor.shutdown();
                assertTrue(
                        "Arbeidertradene avsluttet ikke innen fem sekunder",
                        executor.awaitTermination(5, TimeUnit.SECONDS));
                for (Future<?> oppgave : oppgaver) {
                    oppgave.get(5, TimeUnit.SECONDS);
                }
            } finally {
                executor.shutdownNow();
                assertTrue("Test-executor er ikke avsluttet", executor.awaitTermination(5, TimeUnit.SECONDS));
            }
            assertThat(usedThreads).hasSize(2).doesNotContain(baseThread);
            assertEquals(authContext, AuthContextHolderThreadLocal.instance().requireContext());

            FutureTask<Optional<AuthContext>> childContext = new FutureTask<>(
                    () -> AuthContextHolderThreadLocal.instance().getContext());
            Thread thread = new Thread(childContext);
            thread.start();
            try {
                assertThat(childContext.get(5, TimeUnit.SECONDS)).isEmpty();
            } finally {
                thread.interrupt();
                thread.join(5000);
                assertThat(thread.isAlive()).isFalse();
            }

            // empty child context
            AuthContextHolderThreadLocal.instance().withContext(null, this::assertNoContext);

            // populated child context
            AuthContext otherContext = newAuthContext("other");
            AuthContextHolderThreadLocal.instance()
                    .withContext(
                            otherContext,
                            () -> assertEquals(
                                    otherContext,
                                    AuthContextHolderThreadLocal.instance().requireContext()));

            // failing child context
            assertThatThrownBy(() -> AuthContextHolderThreadLocal.instance().withContext(otherContext, () -> {
                        throw new Error();
                    }))
                    .isInstanceOf(Error.class);

            assertEquals(authContext, AuthContextHolderThreadLocal.instance().requireContext());
        });

        assertNoContext();
    }

    @Test
    public void getUid__returns_sub_claim() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("subject")
                .audience("audience")
                .issuer("issuer")
                .build();

        Arrays.stream(UserRole.values())
                .forEach(userRole -> assertInContext(
                        claimsSet, userRole, cxt -> assertEquals(Optional.of("subject"), cxt.getUid())));
    }

    @Test
    public void getUid__returns_null_if_sub_claim_is_missing() {
        JWTClaimsSet claimsSet =
                new JWTClaimsSet.Builder().audience("audience").issuer("issuer").build();

        Arrays.stream(UserRole.values())
                .forEach(userRole ->
                        assertInContext(claimsSet, userRole, cxt -> assertEquals(Optional.empty(), cxt.getUid())));
    }

    @Test
    public void getUid__returns_pid_claim_for_external_users() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("subject")
                .audience("audience")
                .issuer("issuer")
                .claim("pid", "pid")
                .build();

        Arrays.stream(UserRole.values())
                .filter(userRole -> !UserRole.EKSTERN.equals(userRole))
                .forEach(userRole -> assertInContext(
                        claimsSet, userRole, ctx -> assertEquals(Optional.of("subject"), ctx.getUid())));

        assertInContext(claimsSet, UserRole.EKSTERN, ctx -> assertEquals(Optional.of("pid"), ctx.getUid()));
    }

    @Test
    public void getUid__returns_NAVident_claim_for_internal_users() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("subject")
                .audience("audience")
                .issuer("issuer")
                .claim(AAD_NAV_IDENT_CLAIM, "A123456")
                .build();

        assertInContext(claimsSet, UserRole.EKSTERN, ctx -> assertEquals(Optional.of("subject"), ctx.getUid()));
        assertInContext(claimsSet, UserRole.INTERN, ctx -> assertEquals(Optional.of("A123456"), ctx.getUid()));
        assertInContext(claimsSet, UserRole.SYSTEM, ctx -> assertEquals(Optional.of("subject"), ctx.getUid()));
    }

    @Test
    public void getUid__returns_pid_claim_for_external_users_and_NAVident_claim_for_internal_users() {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("subject")
                .audience("audience")
                .issuer("issuer")
                .claim("pid", "pid")
                .claim(AAD_NAV_IDENT_CLAIM, "A123456")
                .build();

        assertInContext(claimsSet, UserRole.EKSTERN, ctx -> assertEquals(Optional.of("pid"), ctx.getUid()));
        assertInContext(claimsSet, UserRole.INTERN, ctx -> assertEquals(Optional.of("A123456"), ctx.getUid()));
        assertInContext(claimsSet, UserRole.SYSTEM, ctx -> assertEquals(Optional.of("subject"), ctx.getUid()));
    }

    private void assertInContext(JWTClaimsSet claims, UserRole userRole, Consumer<AuthContextHolder> assertion) {
        AuthContextHolderThreadLocal.instance()
                .withContext(
                        new AuthContext(userRole, new PlainJWT(claims)),
                        () -> assertion.accept(AuthContextHolderThreadLocal.instance()));
    }

    private AuthContext newAuthContext(String subject) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(subject).build();

        JWT jwt = new PlainJWT(claimsSet);

        return new AuthContext(UserRole.EKSTERN, jwt);
    }

    private void assertNoContext() {
        assertTrue(AuthContextHolderThreadLocal.instance().getContext().isEmpty());
    }
}
