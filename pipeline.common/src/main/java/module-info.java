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

/**
 * Low level file, buffer and logging helpers shared across the 51Degrees
 * Pipeline API.
 */
module fiftyone.common {
    requires transitive org.slf4j;

    // Only used by fiftyone.common.testhelpers.LogbackHelper. Declared static
    // so that consumers using a different SLF4J binding are not forced to put
    // Logback on their module path.
    requires static ch.qos.logback.classic;
    requires static ch.qos.logback.core;

    exports fiftyone.common.testhelpers;
    exports fiftyone.common.wrappers.data;
    exports fiftyone.common.wrappers.data.direct;
    exports fiftyone.common.wrappers.data.indirect;
    exports fiftyone.common.wrappers.io;
}
