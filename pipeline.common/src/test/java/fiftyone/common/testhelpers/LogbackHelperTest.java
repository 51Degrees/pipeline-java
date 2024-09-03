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

package fiftyone.common.testhelpers;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackHelperTest {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Test @Ignore
    // this test is ignored so as not to print red error messages, you have
    // to run it "manually" to see that it works
    public void testIntentional() {
        LogbackHelper.defaultConfig();
        logger.warn("Warning");
        logger.error("Error");
        LogbackHelper.intentionalErrorConfig();
        logger.warn("Warning");
        logger.error("Error");
        LogbackHelper.defaultConfig();
        logger.warn("Warning");
        logger.error("Error");
    }
}