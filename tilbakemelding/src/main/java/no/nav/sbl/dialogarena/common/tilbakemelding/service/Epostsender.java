package no.nav.sbl.dialogarena.common.tilbakemelding.service;

import no.nav.modig.core.exception.SystemException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/*
* Epostsender som sender tilbakemeldinger til en epostadresse
*/
public class Epostsender implements TilbakemeldingService {

    private String applikasjonsnavn;
    private String epostadresse;
    private JavaMailSender mailSender;

    public Epostsender() {}

    public Epostsender(String host, int port, String applikasjonsnavn, String epostadresse) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        mailSender = sender;
        this.applikasjonsnavn = applikasjonsnavn;
        this.epostadresse = epostadresse;
    }

    public void sendTilbakemelding(String tilbakemelding) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            instantiateAndPopulateMimeMessageHelper(mimeMessage, tilbakemelding);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new SystemException("messagingexception", e);
        }
    }

    private void instantiateAndPopulateMimeMessageHelper(MimeMessage mimeMessage, String tilbakemelding) throws MessagingException {
        String tilbakemelding_renset = EpostCleaner.cleanbody(tilbakemelding);
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
        mimeMessageHelper.setTo(epostadresse);
        mimeMessageHelper.setFrom(epostadresse);
        mimeMessageHelper.setSubject(applikasjonsnavn);
        mimeMessageHelper.setText(tilbakemelding_renset);
    }

    public String getApplikasjonsnavn() {
        return applikasjonsnavn;
    }

    public String getEpostadresse() {
        return epostadresse;
    }

}
