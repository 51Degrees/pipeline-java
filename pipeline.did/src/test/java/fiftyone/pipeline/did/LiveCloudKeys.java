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

package fiftyone.pipeline.did;

/**
 * Where the tests that call the live 51Degrees cloud service read their
 * resource key and licence key from, and what they say when there is
 * none.
 * <p>
 * Every name is tried as an environment variable first and then as a
 * system property, so a developer can export one and a build can pass one
 * on the command line with {@code -D}. There is no fallback to a skip,
 * because a cloud test that quietly passes with no key reports success
 * having called nothing.
 */
final class LiveCloudKeys {

    /**
     * The resource key names, in the order they are tried.
     * <p>
     * {@code _51DEGREES_RESOURCE_KEY} is first because the workflow that
     * runs these tests once for every key the organisation holds sets
     * that single name to each key in turn, and a name later in this list
     * winning would make every one of those runs use the same key.
     */
    static final String[] RESOURCE_KEY_NAMES = {
        "_51DEGREES_RESOURCE_KEY",
        "RESOURCE_KEY",
        "_51DEGREES_RESOURCE_KEY_BESPOKE",
        "TestResourceKey",
        "SUPER_RESOURCE_KEY",
    };

    /**
     * The licence key names, in the order they are tried. A licence key
     * is optional, because an account can hold its products on the
     * resource key alone.
     */
    static final String[] LICENCE_KEY_NAMES = {
        "_51DEGREES_LICENSE_KEY",
        "LICENSE_KEY",
        "_51DEGREES_LICENSE_KEY_BESPOKE",
        "_51DEGREES_LICENSE_KEY_CLOUDV5BESPOKE",
        "TestLicenseKey",
    };

    private LiveCloudKeys() {
    }

    /**
     * The first value set under any of the given names, or null when none
     * of them is set anywhere.
     */
    static String find(String[] names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value == null || value.trim().isEmpty()) {
                value = System.getProperty(name);
            }
            if (value != null && value.trim().isEmpty() == false) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * What to fail with when no resource key is set, naming every place
     * that was looked and what the key has to be entitled to.
     */
    static String noResourceKey() {
        return "These tests call the live 51Degrees cloud service, so they "
            + "need a resource key and there is nothing to fall back on. "
            + "None of " + String.join(", ", RESOURCE_KEY_NAMES) + " is set, "
            + "as an environment variable or as a system property, tried in "
            + "that order. The key has to carry the fodid product, because "
            + "these tests ask the cloud for FODiD.IdProbGlobal and "
            + "FODiD.IdProbLic. Create a key at "
            + "https://configure.51degrees.com.";
    }

    /**
     * What to fail with when the cloud answered but returned no 51Did,
     * which is the resource key not carrying the product rather than the
     * service being broken.
     */
    static String notEntitled(String detail) {
        return "The live 51Degrees cloud service answered, but returned no "
            + "51Did for " + detail + ". That is the resource key not "
            + "carrying the fodid product rather than a fault in the "
            + "service or in this package. Use a key entitled to fodid, "
            + "from https://configure.51degrees.com.";
    }
}
