package no.nav.apiapp.feil;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class FeilMapperTest {

    @Test
    public void nyFeilId_alfanumeriskId(){
        assertThat(FeilMapper.nyFeilId()).matches("\\w{32}");
    }

}