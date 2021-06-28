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
        VeilederVisningsnavn veilederVisningsnavn1 = new VeilederVisningsnavn()
                .setNavIdent(NavIdent.of("Z1234"));

        VeilederVisningsnavn veilederVisningsnavn2 = new VeilederVisningsnavn()
                .setNavIdent(NavIdent.of("Z5678"));

        NomClient nomClient = mock(NomClient.class);
        when(nomClient.finnVisningsnavn(any(NavIdent.class))).thenReturn(veilederVisningsnavn1);

        CachedNomClient cachedNomClient = new CachedNomClient(nomClient);

        VeilederVisningsnavn funnetVisningsnavn = cachedNomClient.finnVisningsnavn(veilederVisningsnavn1.navIdent);
        assertEquals(veilederVisningsnavn1, funnetVisningsnavn);


        ArgumentCaptor<List<NavIdent>> captor = ArgumentCaptor.forClass(List.class);

        // Create mutable list instead of using List.of()
        List<VeilederVisningsnavn> visningsnavn = new ArrayList<>() {{
            add(veilederVisningsnavn1);
            add(veilederVisningsnavn2);
        }};

        when(nomClient.finnVisningsnavn(captor.capture())).thenReturn(visningsnavn);

        cachedNomClient.finnVisningsnavn(List.of(veilederVisningsnavn1.navIdent, veilederVisningsnavn2.navIdent));

        assertEquals(1, captor.getValue().size());
        assertEquals(veilederVisningsnavn2.navIdent, captor.getValue().get(0));
    }

    @Test
    public void skal_ikke_gjore_request_hvis_alt_er_cachet() {
        VeilederVisningsnavn veilederVisningsnavn1 = new VeilederVisningsnavn()
                .setNavIdent(NavIdent.of("Z1234"));

        VeilederVisningsnavn veilederVisningsnavn2 = new VeilederVisningsnavn()
                .setNavIdent(NavIdent.of("Z5678"));

        List<VeilederVisningsnavn> visningsnavn = new ArrayList<>() {{
            add(veilederVisningsnavn1);
            add(veilederVisningsnavn2);
        }};

        NomClient nomClient = mock(NomClient.class);
        when(nomClient.finnVisningsnavn(anyList())).thenReturn(visningsnavn);

        CachedNomClient cachedNomClient = new CachedNomClient(nomClient);

        List<NavIdent> navIdenter = List.of(veilederVisningsnavn1.navIdent, veilederVisningsnavn2.navIdent);
        cachedNomClient.finnVisningsnavn(navIdenter);
        cachedNomClient.finnVisningsnavn(navIdenter);

        verify(nomClient, times(1)).finnVisningsnavn(anyList());
    }


}
