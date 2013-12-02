package no.nav.sbl.dialogarena.common.integrasjon.features;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.activation.DataSource;

import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.LoggerFactory;

/**
 * Interceptor som sørger for at vi uansett hva som sjker lukker inputstreamen
 * til attachments cxf har skrevet til tempfil, slik at de ikke blir hengenende.
 * 
 * Hentet fra http://davekieras.wordpress.com/2013/05/30/apache-cxf-attachment-temp-file-leak/ 
 */
public class AttachmentCleanupInterceptor extends AbstractPhaseInterceptor<Message> {

	public AttachmentCleanupInterceptor() {
		super(Phase.PREPARE_SEND_ENDING);
	}

	public void handleMessage(Message message) {
		Exchange exchange = message.getExchange();
		cleanRequestAttachment(exchange);
	}

	private void cleanRequestAttachment(Exchange exchange) {
		Collection<Attachment> inAttachments = exchange.getInMessage().getAttachments();
		if (inAttachments != null) {
			for (Attachment attachment : inAttachments) {
				try {
					cleanRequestAttachment(attachment);
				} catch (IOException e) {
					LoggerFactory.getLogger(AttachmentCleanupInterceptor.class).warn("Feil ved fjerning av tempfil for cxf-attachment", e);
				}
			}
		}
	}

	protected void cleanRequestAttachment(Attachment attachment) throws IOException {
		DataSource ds = attachment.getDataHandler().getDataSource();
		if (ds instanceof AttachmentDataSource) {
			InputStream is = ds.getInputStream();
			is.close();
		}
	}

}