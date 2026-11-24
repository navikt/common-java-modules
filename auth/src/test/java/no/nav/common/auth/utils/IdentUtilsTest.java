package no.nav.common.auth.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IdentUtilsTest {

    @Test
    public void should_return_true_when_nav_ident_is_valid() {
        assertTrue(IdentUtils.erGydligNavIdent("Z123456"));
        assertTrue(IdentUtils.erGydligNavIdent("H999777"));
        assertTrue(IdentUtils.erGydligNavIdent("A789432"));
    }

    @Test
    public void should_return_false_when_nav_ident_is_invalid() {
        assertFalse(IdentUtils.erGydligNavIdent(""));
        assertFalse(IdentUtils.erGydligNavIdent(null));
        assertFalse(IdentUtils.erGydligNavIdent("z123456"));
        assertFalse(IdentUtils.erGydligNavIdent("Z12345"));
        assertFalse(IdentUtils.erGydligNavIdent("Z123"));
        assertFalse(IdentUtils.erGydligNavIdent("Z1234567"));
        assertFalse(IdentUtils.erGydligNavIdent("ZZ123456"));
        assertFalse(IdentUtils.erGydligNavIdent("Z123B456"));

        // NAV ident skal ikke ha ÆØÅ. Æ -> A, Ø -> O, Å -> A
        assertFalse(IdentUtils.erGydligNavIdent("Æ123456"));
        assertFalse(IdentUtils.erGydligNavIdent("Ø123456"));
        assertFalse(IdentUtils.erGydligNavIdent("Å123456"));
    }
}
