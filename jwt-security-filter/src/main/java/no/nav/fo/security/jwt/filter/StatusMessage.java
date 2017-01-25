package no.nav.fo.security.jwt.filter;

import javax.ws.rs.core.Response.Status;

public class StatusMessage {

    private String message;
    private Status status;

    public StatusMessage(String message, Status status) {
        this.message = message;
        this.status = status;
    }

    @Override
    public String toString() {
        return StatusMessage.class.getSimpleName() +
                "<status=" + status +
                ",message=" + message +
                ">";
    }
}
