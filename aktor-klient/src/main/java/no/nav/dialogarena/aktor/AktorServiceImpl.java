package no.nav.dialogarena.aktor;

import lombok.val;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOR_ID_FROM_FNR;
import static no.nav.dialogarena.aktor.AktorConfig.FNR_FROM_AKTOR_ID;

@Component
public class AktorServiceImpl implements AktorService {

    @Inject
    private AktoerV2 aktoerV2;

    @Override
    @Cacheable(FNR_FROM_AKTOR_ID)
    public Optional<String> getFnr(String aktorId) {
        try {
            val req = new HentIdentForAktoerIdRequest();
            req.setAktoerId(aktorId);
            val res = aktoerV2.hentIdentForAktoerId(req);
            return ofNullable(res.getIdent());
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
            return empty();
        }
    }

    @Cacheable(AKTOR_ID_FROM_FNR)
    public Optional<String> getAktorId(String fnr) {
        try {
            val req = new HentAktoerIdForIdentRequest();
            req.setIdent(fnr);
            val res = aktoerV2.hentAktoerIdForIdent(req);
            return ofNullable(res.getAktoerId());
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            return empty();
        }
    }
}
