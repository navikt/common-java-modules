package no.nav.common.client.aktorregister;

import com.fasterxml.jackson.databind.type.MapType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static no.nav.common.rest.client.RestUtils.getBodyStr;
import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
public class AktorregisterHttpClient implements AktorregisterClient {

    private final static MapType mapType = JsonUtils.getMapper().getTypeFactory().constructMapType(HashMap.class, String.class, IdentData.class);

    private final String aktorregisterUrl;
    private final String aktorregisterIsAliveUrl;

    private final String consumingApplication;

    private final Supplier<String> tokenSupplier;

    private final OkHttpClient client;

    public AktorregisterHttpClient(String aktorregisterUrl, String consumingApplication, Supplier<String> tokenSupplier) {
        this.aktorregisterUrl = aktorregisterUrl;
        this.aktorregisterIsAliveUrl = resolveIsAliveUrl(aktorregisterUrl);
        this.consumingApplication = consumingApplication;
        this.tokenSupplier = tokenSupplier;
        this.client = RestClient.baseClient();
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        return Fnr.of(hentEnkeltIdent(aktorId, Identgruppe.NorskIdent));
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        return AktorId.of(hentEnkeltIdent(fnr, Identgruppe.AktoerId));
    }

    @SneakyThrows
    @Override
    public Map<AktorId, Fnr> hentFnrBolk(List<AktorId> aktorIdListe) {
        Map<String, IdentData> oppslagMap = hentIdenter(aktorIdListe, Identgruppe.AktoerId, true);
        Map<AktorId, Fnr> aktorIdToFnrMap = new HashMap<>();

        oppslagMap
                .entrySet()
                .stream()
                .map(this::tilIdentOppslag)
                .forEach(oppslag -> {
                    AktorId aktorId = AktorId.of(oppslag.getIdentTilRegister());
                    Optional<Fnr> maybeFnr = oppslag.getIdentFraRegister().map(Fnr::of);

                    maybeFnr.ifPresent(fnr -> aktorIdToFnrMap.put(aktorId, fnr));
                });

        return aktorIdToFnrMap;
    }

    @SneakyThrows
    @Override
    public Map<Fnr, AktorId> hentAktorIdBolk(List<Fnr> fnrListe) {
        Map<String, IdentData> oppslagMap = hentIdenter(fnrListe, Identgruppe.AktoerId, true);
        Map<Fnr, AktorId> fnrToAktorIdMap = new HashMap<>();

        oppslagMap
                .entrySet()
                .stream()
                .map(this::tilIdentOppslag)
                .forEach(oppslag -> {
                    Fnr fnr = Fnr.of(oppslag.getIdentTilRegister());
                    Optional<AktorId> maybeAktorId = oppslag.getIdentFraRegister().map(AktorId::of);

                    maybeAktorId.ifPresent(aktorId -> fnrToAktorIdMap.put(fnr, aktorId));
                });

        return fnrToAktorIdMap;
    }

    @SneakyThrows
    @Override
    public List<AktorId> hentAktorIder(Fnr fnr) {
        return hentIdenter(Collections.singletonList(fnr), Identgruppe.AktoerId, false)
                .entrySet()
                .stream()
                .flatMap(e -> Optional.ofNullable(e.getValue().identer)
                        .orElseThrow(() -> new RuntimeException("Aktør registeret feilet og fant ikke identer på bruker"))
                        .stream()
                        .filter(i -> i.identgruppe == Identgruppe.AktoerId && i.ident != null)
                        .map(i -> AktorId.of(i.ident)))
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private String hentEnkeltIdent(EksternBrukerId eksternBrukerId, Identgruppe identgruppe) {
        return hentIdenter(Collections.singletonList(eksternBrukerId), identgruppe, true)
                .entrySet()
                .stream()
                .filter(this::filtrerIkkeGjeldendeIdent)
                .findFirst()
                .flatMap(e -> finnGjeldendeIdent(e.getValue().identer))
                .map(i -> i.ident)
                .orElseThrow(() -> new RuntimeException("Fant ikke gjeldende ident"));
    }

    private String createRequestUrl(String aktorregisterUrl, Identgruppe identgruppe, boolean gjeldende) {
        if (gjeldende) {
            return String.format("%s/identer?gjeldende=true&identgruppe=%s", aktorregisterUrl, valueOf(identgruppe));
        }
        return String.format("%s/identer?identgruppe=%s", aktorregisterUrl, valueOf(identgruppe));
    }

    private boolean filtrerIkkeGjeldendeIdent(Map.Entry<String, IdentData> identEntry) {
        List<Ident> identer = identEntry.getValue().identer;
        return identer != null && finnGjeldendeIdent(identer).isPresent();
    }

    private Optional<Ident> finnGjeldendeIdent(List<Ident> identer) {
        return identer.stream().filter(ident -> ident.gjeldende).findFirst();
    }

    private IdentOppslag tilIdentOppslag(Map.Entry<String, IdentData> identEntry) {
        Optional<Ident> gjeldendeIdent = finnGjeldendeIdent(identEntry.getValue().identer);
        return new IdentOppslag(identEntry.getKey(), gjeldendeIdent.map(i -> i.ident).orElse(null));
    }


    private <T extends EksternBrukerId> Map<String, IdentData> hentIdenter(List<T> eksternBrukerIdList, Identgruppe identgruppe, boolean gjeldende) throws IOException {
        String personidenter = eksternBrukerIdList.stream().map(Id::get).collect(Collectors.joining(","));
        String requestUrl = createRequestUrl(aktorregisterUrl, identgruppe, gjeldende);

        Request request = new Request.Builder()
                .url(requestUrl)
                .addHeader("Nav-Call-Id", UUID.randomUUID().toString())
                .addHeader("Nav-Consumer-Id", consumingApplication)
                .addHeader("Nav-Personidenter", personidenter)
                .addHeader("Authorization", "Bearer " + tokenSupplier.get())
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (response.code() >= 300) {
                String responseStr = getBodyStr(response).orElse("");
                throw new RuntimeException(
                        String.format("Fikk uventet status %d fra %s. Respons: %s",
                                response.code(), request, responseStr)
                );
            }

            Optional<String> jsonStr = getBodyStr(response);

            if (jsonStr.isEmpty()) {
                throw new IllegalStateException("Respons mangler body");
            }

            return JsonUtils.getMapper().readValue(jsonStr.get(), mapType);
        } catch (Exception e) {
            log.error("Klarte ikke å gjore oppslag mot aktorregister", e);
            throw e;
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(aktorregisterIsAliveUrl, client);
    }

    enum Identgruppe {
        NorskIdent, AktoerId
    }

    private static class IdentData {

        public List<Ident> identer;

        public String feilmelding;

    }

    private static class Ident {

        public String ident; // fnr eller aktorid

        public Identgruppe identgruppe;

        public boolean gjeldende;

    }

    private static class IdentOppslag {

        /**
         * Identen(fnr/aktør id) som det ble gjort oppslag på i aktørregisteret
         */
        private String identTilRegister;

        /**
         * Identen(fnr/aktør id) som det ble returnert fra aktørregisteret
         */
        private String identFraRegister;

        public IdentOppslag(String identTilRegister, String identFraRegister) {
            this.identTilRegister = identTilRegister;
            this.identFraRegister = identFraRegister;
        }

        public String getIdentTilRegister() {
            return identTilRegister;
        }

        public Optional<String> getIdentFraRegister() {
            return Optional.ofNullable(identFraRegister);
        }

    }

    private String resolveIsAliveUrl(String apiUrl) {
        String baseUrl = apiUrl;
        if (apiUrl.endsWith("api/v1")) {
            baseUrl = apiUrl.replace("api/v1", "");
        }
        return joinPaths(baseUrl, "/internal/isAlive");
    }
}
