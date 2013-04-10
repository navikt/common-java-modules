package no.nav.sbl.dialogarena.common.tilbakemelding.tilbakemelding;



import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import javax.mail.internet.MimeMessage;


public class Epostsender {

    public Epostsender(){}

    public Epostsender(String host, int port){
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        mailSender = sender;
    }



    private JavaMailSenderImpl mailSender;


    public void sendEpost(String avsender, String mottaker, String emne, String innhold) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setTo(mottaker);
            mimeMessageHelper.setFrom(avsender);
            mimeMessageHelper.setSubject(emne);

      //      String fontfamily = (String) commonProperties.get("admin.epost.fontfamily");
        //    String fontsize = (String) commonProperties.get("admin.epost.fontsize");


            StringBuilder text = new StringBuilder();
            //text.append("<html><body style=\"font-family:"+fontfamily+"; font-size:"+fontsize+";\">");
            text.append(innhold);
            //text.append("</body></html>");

            mimeMessageHelper.setText(text.toString(), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
