package no.nav.common.abac.domain.request;

public enum ActionId {
    READ("read"), WRITE("update");

    private String id;

    ActionId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
