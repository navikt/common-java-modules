package no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding;


import org.jsoup.Jsoup;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;



/*
* Epostsender som sender tilbakemeldinger til en epostadresse
*/
public class Epostsender {

    private String applikasjonsnavn;
    private String epostadresse;
    private JavaMailSenderImpl mailSender;


    public Epostsender() {
    }

    public Epostsender(String host, int port, String applikasjonsnavn, String epostadresse) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        mailSender = sender;
        this.applikasjonsnavn = applikasjonsnavn;
        this.epostadresse = epostadresse;
    }

    public void sendEpost(String tilbakemelding) {
        try {

            String cleaned_tilbakemelding = EpostCleaner.cleanbody(tilbakemelding);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setTo(epostadresse);
            mimeMessageHelper.setFrom(epostadresse);
            mimeMessageHelper.setSubject(applikasjonsnavn);
            mimeMessageHelper.setText(cleaned_tilbakemelding);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public String getApplikasjonsnavn() {
        return applikasjonsnavn;
    }

    public String getEpostadresse() {
        return epostadresse;
    }
}
