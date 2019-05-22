package no.nav.common.utils;

import java.util.UUID;

public class IdUtils {
    public static String generateId() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }
}
