package no.nav.fo.feed.exception;


import no.nav.fo.feed.producer.FeedWebhookResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class NoCallbackUrlException extends WebApplicationException {
    public NoCallbackUrlException() {
        super(
                Response
                .status(BAD_REQUEST)
                .entity(new FeedWebhookResponse().setMelding( "Respons må inneholde callback-url"))
                .build()
        );
    }
}
