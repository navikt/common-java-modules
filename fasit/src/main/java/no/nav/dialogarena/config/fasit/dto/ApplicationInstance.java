package no.nav.dialogarena.config.fasit.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ApplicationInstance {
    public String environment;
    public List<Resource> usedresources;
}
