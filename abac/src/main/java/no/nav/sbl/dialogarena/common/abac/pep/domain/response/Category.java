package no.nav.sbl.dialogarena.common.abac.pep.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private String categoryId;
    private Attribute attribute;
}

