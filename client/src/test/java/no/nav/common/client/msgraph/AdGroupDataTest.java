package no.nav.common.client.msgraph;

import no.nav.common.types.identer.AzureObjectId;
import org.junit.Test;
import static org.junit.Assert.*;

public class AdGroupDataTest {

    @Test
    public void testConstructorAndGetters() {
        AdGroupData adGroup = new AdGroupData(new AzureObjectId("group-123"), "Test Group");

        assertEquals(new AzureObjectId("group-123"), adGroup.id());
        assertEquals("Test Group", adGroup.displayName());
    }

    @Test
    public void testEqualsAndHashCode() {
        AdGroupData group1 = new AdGroupData(new AzureObjectId("123"), ("Group"));
        AdGroupData group2 = new AdGroupData(new AzureObjectId("123"), ("Group"));
        AdGroupData group3 = new AdGroupData(new AzureObjectId("456"), ("Different"));

        // Test equals
        assertEquals(group1, group2);
        assertNotEquals(group2, group3);
        assertNotEquals(null, group2);

        // Test hashCode
        assertEquals(group1.hashCode(), group2.hashCode());
    }

    @Test
    public void testToString() {
        AdGroupData group = new AdGroupData(new AzureObjectId("group-123"), ("Test Group"));

        String toString = group.toString();

        assertTrue(toString.contains("group-123"));
        assertTrue(toString.contains("Test Group"));
    }

    @Test
    public void testAdditionalEqualsScenarios() {
        AdGroupData group = new AdGroupData(new AzureObjectId("123"), ("Group"));

        // Test self-equality
        assertEquals(group, group);

        // Test same id but different name
        AdGroupData sameIdDiffName = new AdGroupData(new AzureObjectId("123"), ("Different Name"));

        assertNotEquals(group, sameIdDiffName);

        // Test null fields

    }
}
