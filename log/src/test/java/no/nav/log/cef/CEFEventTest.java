package no.nav.log.cef;

import org.junit.Test;

import static no.nav.log.cef.CEFEvent.Severity.INFO;
import static org.junit.Assert.assertEquals;

public class CEFEventTest {

    @Test
    public void toStringWithEscaping() {
        CEFEvent event = CEFEvent.builder()
                .cefVersion("123")
                .applicationName("application|Name")
                .logName("log\\Name")
                .logFormatVersion("321.123")
                .eventType("eventType")
                .description("description")
                .severity(INFO)
                .addAttribute("a1","a1value")
                .addAttribute("a2","a2\\value")
                .addAttribute("a3","a3value")
                .addAttribute("a4","a4value")
                .addAttribute("a5","a5=value")
                .addAttribute("a6","a6value")
                .build();

        String expected =
                "CEF:123|application\\|Name|log\\\\Name|321.123|eventType|description|INFO|a1=a1value a2=a2\\\\value a3=a3value a4=a4value a5=a5\\=value a6=a6value";

        assertEquals(expected, event.toString());
    }
}
