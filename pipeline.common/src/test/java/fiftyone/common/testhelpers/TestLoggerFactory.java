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

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public final class TestLoggerFactory implements ILoggerFactory {

    private final ILoggerFactory internalLoggerFactory;
    public List<TestLogger> loggers = new ArrayList<>();

    public TestLoggerFactory(ILoggerFactory internalLoggerFactory) {
        this.internalLoggerFactory = internalLoggerFactory;
    }

    @Override
    public Logger getLogger(String name) {
        TestLogger logger = new TestLogger(
            name,
            internalLoggerFactory == null ?
                null : internalLoggerFactory.getLogger(name));
        loggers.add(logger);
        return logger;
    }

    public void assertMaxWarnings(int count) {
        List<String> warningsLogged = new ArrayList<>();
        for (TestLogger logger : loggers) {
            warningsLogged.addAll(logger.warningsLogged);
        }
        if (warningsLogged.size() > count) {
            String message = warningsLogged.size() + " warnings occurred " +
                "during test " +
                (count > 0 ? ("expected no more than " + count) : " ") + ":";
            for (String warning : warningsLogged) {
                message += "\n";
                message += "\n";
                message += warning;
            }
            fail(message);
        }
    }

    public void assertMaxErrors(int count) {
        List<String> errorsLogged = new ArrayList<>();
        for (TestLogger logger : loggers) {
            errorsLogged.addAll(logger.errorsLogged);
        }
        if (errorsLogged.size() > count) {
            String message = errorsLogged.size() + " errors occurred " +
                "during test " +
                (count > 0 ? ("expected no more than " + count) : " ") + ":";
            for (String warning : errorsLogged) {
                message += "\n";
                message += "\n";
                message += warning;
            }
            fail(message);
        }
    }
}
