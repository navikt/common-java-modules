package no.nav.common.client.nom;

import no.nav.common.types.identer.NavIdent;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CachedNomClientTest {

    @Test
    public void skal_cache_navn() {
        VeilederNavn veilederNavn1 = new VeilederNavn()
                .setNavIdent(NavIdent.of("Z1234"));

        VeilederNavn veilederNavn2 = new VeilederNavn()
                .setNavIdent(NavIdent.of("Z5678"));

        NomClient nomClient = mock(NomClient.class);
        when(nomClient.finnNavn(any(NavIdent.class))).thenReturn(veilederNavn1);

        CachedNomClient cachedNomClient = new CachedNomClient(nomClient);

        VeilederNavn funnetNavn = cachedNomClient.finnNavn(veilederNavn1.navIdent);
        assertEquals(veilederNavn1, funnetNavn);


        ArgumentCaptor<List<NavIdent>> captor = ArgumentCaptor.forClass(List.class);

        // Create mutable list instead of using List.of()
        List<VeilederNavn> navn = new ArrayList<>() {{
            add(veilederNavn1);
            add(veilederNavn2);
        }};

        when(nomClient.finnNavn(captor.capture())).thenReturn(navn);

        cachedNomClient.finnNavn(List.of(veilederNavn1.navIdent, veilederNavn2.navIdent));

        assertEquals(1, captor.getValue().size());
        assertEquals(veilederNavn2.navIdent, captor.getValue().get(0));
    }

    @Test
    public void skal_ikke_gjore_request_hvis_alt_er_cachet() {
        VeilederNavn veilederNavn1 = new VeilederNavn()
                .setNavIdent(NavIdent.of("Z1234"));

        VeilederNavn veilederNavn2 = new VeilederNavn()
                .setNavIdent(NavIdent.of("Z5678"));

        List<VeilederNavn> navn = new ArrayList<>() {{
            add(veilederNavn1);
            add(veilederNavn2);
        }};

        NomClient nomClient = mock(NomClient.class);
        when(nomClient.finnNavn(anyList())).thenReturn(navn);

        CachedNomClient cachedNomClient = new CachedNomClient(nomClient);

        List<NavIdent> navIdenter = List.of(veilederNavn1.navIdent, veilederNavn2.navIdent);
        cachedNomClient.finnNavn(navIdenter);
        cachedNomClient.finnNavn(navIdenter);

        verify(nomClient, times(1)).finnNavn(anyList());
    }


}
