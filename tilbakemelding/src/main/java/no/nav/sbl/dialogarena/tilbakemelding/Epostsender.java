package no.nav.sbl.dialogarena.tilbakemelding;

import java.util.Properties;


public class Epostsender {

/*
    @Autowired
    private JavaMailSenderImpl mailSender;

    @Autowired
    private Properties commonProperties;

    public void sendEpost(String avsender, String mottaker, String emne, String innhold) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setTo(mottaker);
            mimeMessageHelper.setFrom(avsender);
            mimeMessageHelper.setSubject(emne);

            String fontfamily = (String) commonProperties.get("admin.epost.fontfamily");
            String fontsize = (String) commonProperties.get("admin.epost.fontsize");


            StringBuilder text = new StringBuilder();
            text.append("<html><body style=\"font-family:"+fontfamily+"; font-size:"+fontsize+";\">");
            text.append(innhold);
            text.append("</body></html>");

            mimeMessageHelper.setText(text.toString(), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    */
    }
