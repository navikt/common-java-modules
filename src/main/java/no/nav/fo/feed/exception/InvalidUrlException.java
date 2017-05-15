package no.nav.fo.feed.exception;

import no.nav.fo.feed.producer.FeedWebhookResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class InvalidUrlException extends WebApplicationException{
    public InvalidUrlException() {
        super(
                Response
                        .status(BAD_REQUEST)
                        .entity(new FeedWebhookResponse().setMelding("Feil format på callback-url"))
                        .build()
        );
    }
}
