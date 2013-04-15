package no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


/*
* Kjører ikke på byggeserver grunnet smtp-testserveren derfor satt til ignore.
*/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TilbakemeldingTestContext.class})
public class EpostsenderIntegrationTest {

    @Inject
    private Epostsender epostsender;

    @Inject
    private Integer smtpPort;


    @Ignore
    @Test
    public void skalSendeEpost() throws MessagingException, IOException {
        Wiser smtpServer = new Wiser();
        smtpServer.setHostname("127.0.0.1");
        smtpServer.setPort(smtpPort);
        smtpServer.start();

        String innhold = "innhold";
        epostsender.sendEpost(innhold);

        List<WiserMessage> messages = smtpServer.getMessages();
        assertThat(messages.size(), is(1));
        MimeMessage mimeMessage = messages.get(0).getMimeMessage();
        assertThat(messages.get(0).getEnvelopeReceiver(), is(epostsender.getEpostadresse()));
        assertThat(messages.get(0).getEnvelopeSender(), is(epostsender.getEpostadresse()));
        assertThat(mimeMessage.getContent(), instanceOf(MimeMultipart.class));
        assertThat(mimeMessage.getSubject(), is(epostsender.getApplikasjonsnavn()));
        smtpServer.stop();
    }



}