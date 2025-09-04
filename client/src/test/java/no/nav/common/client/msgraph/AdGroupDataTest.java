package no.nav.common.client.msgraph;

import no.nav.common.types.identer.AzureObjectId;
import org.junit.Test;
import static org.junit.Assert.*;

public class AdGroupDataTest {

    @Test
    public void testConstructorAndGetters() {
        AdGroupData adGroup = new AdGroupData()
                .setId(AzureObjectId.of("group-123"))
                .setDisplayName("Test Group");

        assertEquals(new AzureObjectId("group-123"), adGroup.getId());
        assertEquals("Test Group", adGroup.getDisplayName());
    }

    @Test
    public void testEqualsAndHashCode() {
        AdGroupData group1 = new AdGroupData().setId(AzureObjectId.of("123")).setDisplayName("Group");
        AdGroupData group2 = new AdGroupData().setId(AzureObjectId.of("123")).setDisplayName("Group");
        AdGroupData group3 = new AdGroupData().setId(AzureObjectId.of("456")).setDisplayName("Different");

        // Test equals
        assertEquals(group1, group2);
        assertNotEquals(group1, group3);
        assertNotEquals(null, group1);

        // Test hashCode
        assertEquals(group1.hashCode(), group2.hashCode());
    }

    @Test
    public void testToString() {
        AdGroupData group = new AdGroupData()
                .setId(AzureObjectId.of("group-123"))
                .setDisplayName("Test Group");

        String toString = group.toString();

        assertTrue(toString.contains("group-123"));
        assertTrue(toString.contains("Test Group"));
    }
}
