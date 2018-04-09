package no.nav.fo.feed.exception;

import no.nav.fo.feed.common.FeedWebhookResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class MissingIdException extends WebApplicationException{
    public MissingIdException() {
        super(
                Response
                        .status(BAD_REQUEST)
                        .entity(new FeedWebhookResponse().setMelding("Request må inneholde id"))
                        .build()
        );
    }
}
