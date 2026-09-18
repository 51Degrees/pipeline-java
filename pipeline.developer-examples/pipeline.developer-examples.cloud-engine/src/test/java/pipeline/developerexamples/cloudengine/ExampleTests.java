/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package pipeline.developerexamples.cloudengine;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs the cloud engine example against the live cloud service.
 * <p>
 * Nothing is caught here. The example builds a cloud request engine,
 * which asks the service for its accessible properties and its evidence
 * keys, and then processes evidence through it, so any failure to reach
 * the service, or any answer the pipeline cannot use, has to be seen.
 * Catching and skipping would report success for a run that never
 * reached the service at all.
 */
public class ExampleTests {

    @Test
    public void SimpleCloudEngine_Test() throws Exception {
        if (Main.configuredResourceKey() == null) {
            fail("This test runs the cloud engine example against the live "
                + "51Degrees cloud service, so it needs a resource key. None "
                + "of " + String.join(", ", Main.RESOURCE_KEY_NAMES) + " is "
                + "set, as an environment variable or as a system property, "
                + "tried in that order. Any key works here, because the "
                + "example reports a product the key does not carry rather "
                + "than failing on it. Create a key at "
                + "https://configure.51degrees.com.");
        }
        new Main.Example().run();
    }
}
