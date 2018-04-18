package no.nav.apiapp.config;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class IssoConfig {
    public String username;
    public String password;
}
