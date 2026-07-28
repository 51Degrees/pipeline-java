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
 * Shared base functions for implementing 51Degrees engines for the 51Degrees
 * Pipeline API.
 * <p>
 * This module contains
 * {@code fiftyone.pipeline.engines.fiftyone.flowelements.interop.LibLoader},
 * which calls {@code System.load} to load the native libraries used by the
 * on-premise engines. On Java 16 and above that is a restricted method, so
 * when this module is resolved from the module path run the JVM with
 * {@code --enable-native-access=fiftyone.pipeline.engines.fiftyone} to
 * suppress the associated warning.
 */
module fiftyone.pipeline.engines.fiftyone {
    requires transitive fiftyone.pipeline.engines;
    requires java.xml;
    requires org.slf4j;

    exports fiftyone.pipeline.engines.fiftyone.configuration;
    exports fiftyone.pipeline.engines.fiftyone.data;
    exports fiftyone.pipeline.engines.fiftyone.exceptions;
    exports fiftyone.pipeline.engines.fiftyone.flowelements;
    exports fiftyone.pipeline.engines.fiftyone.flowelements.interop;
    exports fiftyone.pipeline.engines.fiftyone.trackers;
}
