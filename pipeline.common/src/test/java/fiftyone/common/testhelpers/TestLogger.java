/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
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

import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class TestLogger implements Logger {

    private final Logger internalLogger;

    public List<String> warningsLogged = new ArrayList<>();
    public List<String> errorsLogged = new ArrayList<>();

    public TestLogger(String name, Logger internalLogger) {
        this.internalLogger = internalLogger;
    }

    public void assertMaxWarnings(int count) {
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
        if (errorsLogged.size() > count) {
            String message = errorsLogged.size() + " errors occurred " +
                "during test " +
                (count > 0 ? ("expected no more than " + count) : " ") + ":";
            for (String error : errorsLogged) {
                message += "\n";
                message += "\n";
                message += error;
            }
            fail(message);
        }
    }

    @Override
    public String getName() {
        return internalLogger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return internalLogger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        internalLogger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        internalLogger.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        internalLogger.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        internalLogger.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        internalLogger.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return internalLogger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        internalLogger.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        internalLogger.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        internalLogger.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        internalLogger.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        internalLogger.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return internalLogger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        internalLogger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        internalLogger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        internalLogger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        internalLogger.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        internalLogger.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return internalLogger.isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String msg) {
        internalLogger.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        internalLogger.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        internalLogger.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        internalLogger.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        internalLogger.debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return internalLogger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        internalLogger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        internalLogger.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        internalLogger.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        internalLogger.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        internalLogger.info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return internalLogger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        internalLogger.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        internalLogger.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        internalLogger.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        internalLogger.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        internalLogger.info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return internalLogger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        warningsLogged.add(msg);
        internalLogger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        warningsLogged.add(String.format(format, arg));
        internalLogger.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        warningsLogged.add(String.format(format, arguments));
        internalLogger.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        warningsLogged.add(String.format(format, arg1, arg2));
        internalLogger.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        warningsLogged.add(msg);
        internalLogger.warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return internalLogger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        warningsLogged.add(msg);
        internalLogger.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        warningsLogged.add(String.format(format, arg));
        internalLogger.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warningsLogged.add(String.format(format, arg1, arg2));
        internalLogger.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        warningsLogged.add(String.format(format, arguments));
        internalLogger.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        warningsLogged.add(msg);
        internalLogger.warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return internalLogger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        errorsLogged.add(msg);
        internalLogger.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        errorsLogged.add(String.format(format, arg));
        internalLogger.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        errorsLogged.add(String.format(format, arg1, arg2));
        internalLogger.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        errorsLogged.add(String.format(format, arguments));
        internalLogger.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        errorsLogged.add(msg);
        internalLogger.error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return internalLogger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        errorsLogged.add(msg);
        internalLogger.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        errorsLogged.add(String.format(format, arg));
        internalLogger.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        errorsLogged.add(String.format(format, arg1, arg2));
        internalLogger.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        errorsLogged.add(String.format(format, arguments));
        internalLogger.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        errorsLogged.add(msg);
        internalLogger.error(marker, msg, t);
    }
}
