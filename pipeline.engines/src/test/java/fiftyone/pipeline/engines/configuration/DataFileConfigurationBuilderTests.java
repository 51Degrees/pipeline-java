/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

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
