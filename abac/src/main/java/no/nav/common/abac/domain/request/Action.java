package no.nav.common.abac.domain.request;


import no.nav.common.abac.domain.BaseAttribute;

public class Action extends BaseAttribute {

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

}
