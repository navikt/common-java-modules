package no.nav.util.common;

import java.util.UUID;

public class IdUtils {
    public static String generateId() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }
}
