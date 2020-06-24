package no.nav.common.health.selftest;

import lombok.Value;
import no.nav.common.health.HealthCheck;

@Value
public class SelfTestCheck {

    boolean isCritical;

    HealthCheck check;

}
