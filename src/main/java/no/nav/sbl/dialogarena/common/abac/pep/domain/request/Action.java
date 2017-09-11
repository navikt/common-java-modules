package no.nav.sbl.dialogarena.common.abac.pep.domain.request;


import no.nav.sbl.dialogarena.common.abac.pep.domain.BaseAttribute;

public class Action extends BaseAttribute {

    public enum ActionId {
        READ("read"), WRITE("write");

        private String id;

        ActionId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

}
