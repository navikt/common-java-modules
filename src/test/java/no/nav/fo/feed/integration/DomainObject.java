package no.nav.fo.feed.integration;

class DomainObject implements Comparable<DomainObject> {
    public String id;
    public String name;

    public DomainObject() {
    }

    public DomainObject(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int compareTo(DomainObject o) {
        return id.compareTo(o.id);
    }
}
