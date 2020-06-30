package fiftyone.pipeline.engines.configuration;

import org.junit.Test;

import static org.junit.Assert.*;

public class DataFileConfigurationBuilderTests {
    DataFileConfigurationBuilder configBuilder =
            new DataFileConfigurationBuilder();
    /**
     * Ensure that setting the license key to null will also
     * disable the 'automatic update' and 'update on startup'
     * flags.
     */
    @Test
    public void DataFileConfigurationBuilder_LicenseKey_Null() {
        // Confirm the default state of auto update and
        // update on startup.
        DataFileConfiguration initialState = configBuilder.build(
            "test",
            true);
        assertTrue(
        "Auto updates should be enabled by default",
                initialState.getAutomaticUpdatesEnabled());
        assertFalse(
            "Update on startup should be disabled by default",
            initialState.getUpdateOnStartup());
        // Enable update on startup then set the license key
        // to null before building another configuration
        // object.
        configBuilder.setUpdateOnStartup(true);
        configBuilder.setDataUpdateLicenseKey(null);
        DataFileConfiguration result = configBuilder.build("test", true);
        // Both features should now be disabled.
        assertEquals(0, result.getDataUpdateLicenseKeys().size());
        assertFalse(
            "Auto updates should be disabled",
            result.getAutomaticUpdatesEnabled());
        assertFalse(
            "Update on startup should be disabled",
            result.getUpdateOnStartup());
    }
}
