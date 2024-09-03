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

import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class TestLogger implements Logger {

    private final Logger internalLogger;
    private final String name;

    public List<String> warningsLogged = new ArrayList<>();
    public List<String> errorsLogged = new ArrayList<>();

    public TestLogger(String name, Logger internalLogger) {
        this.name = name;
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
        return internalLogger == null ? name : internalLogger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return internalLogger != null && internalLogger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        if (internalLogger != null) {
            internalLogger.trace(msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (internalLogger != null) {
            internalLogger.trace(format, arg);
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (internalLogger != null) {
            internalLogger.trace(format, arg1, arg2);
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (internalLogger != null) {
            internalLogger.trace(format, arguments);
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (internalLogger != null) {
            internalLogger.trace(msg, t);
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return internalLogger != null && internalLogger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (internalLogger != null) {
            internalLogger.trace(marker, msg);
        }
    }


    @Override
    public void trace(Marker marker, String format, Object arg) {
        if (internalLogger != null) {
            internalLogger.trace(marker, format, arg);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (internalLogger != null) {
            internalLogger.trace(marker, format, arg1, arg2);
        }
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        if (internalLogger != null) {
            internalLogger.trace(marker, format, argArray);
        }
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (internalLogger != null) {
            internalLogger.trace(marker, msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return internalLogger != null && internalLogger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        if (internalLogger != null) {
            internalLogger.debug(msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (internalLogger != null) {
            internalLogger.debug(format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (internalLogger != null) {
            internalLogger.debug(format, arg1, arg2);
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (internalLogger != null) {
            internalLogger.debug(format, arguments);
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (internalLogger != null) {
            internalLogger.debug(msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return internalLogger != null && internalLogger.isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (internalLogger != null) {
            internalLogger.debug(marker, msg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        if (internalLogger != null) {
            internalLogger.debug(marker, format, arg);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (internalLogger != null) {
            internalLogger.debug(marker, format, arg1, arg2);
        }
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        if (internalLogger != null) {
            internalLogger.debug(marker, format, arguments);
        }
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (internalLogger != null) {
            internalLogger.debug(marker, msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return internalLogger != null && internalLogger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (internalLogger != null) {
            internalLogger.info(msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (internalLogger != null) {
            internalLogger.info(format, arg);
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (internalLogger != null) {
            internalLogger.info(format, arg1, arg2);
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (internalLogger != null) {
            internalLogger.info(format, arguments);
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (internalLogger != null) {
            internalLogger.info(msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return internalLogger != null && internalLogger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        if (internalLogger != null) {
            internalLogger.info(marker, msg);
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        if (internalLogger != null) {
            internalLogger.info(marker, format, arg);
        }
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (internalLogger != null) {
            internalLogger.info(marker, format, arg1, arg2);
        }
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        if (internalLogger != null) {
            internalLogger.info(marker, format, arguments);
        }
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (internalLogger != null) {
            internalLogger.info(marker, msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        // Always enable warnings so we can track them.
        return true;
    }

    @Override
    public void warn(String msg) {
        warningsLogged.add(msg);
        if (internalLogger != null && internalLogger.isWarnEnabled()) {
            internalLogger.warn(msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        warningsLogged.add(String.format(format, arg));
        if (internalLogger != null && internalLogger.isWarnEnabled()) {
            internalLogger.warn(format, arg);
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        warningsLogged.add(String.format(format, arguments));
        if (internalLogger != null && internalLogger.isWarnEnabled()) {
            internalLogger.warn(format, arguments);
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        warningsLogged.add(String.format(format, arg1, arg2));
        if (internalLogger != null && internalLogger.isWarnEnabled()) {
            internalLogger.warn(format, arg1, arg2);
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        warningsLogged.add(msg);
        if (internalLogger != null && internalLogger.isWarnEnabled()) {
            internalLogger.warn(msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        // Always enable warnings so we can track them.
        return true;
    }

    @Override
    public void warn(Marker marker, String msg) {
        warningsLogged.add(msg);
        if (internalLogger != null && internalLogger.isWarnEnabled(marker)) {
            internalLogger.warn(marker, msg);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        warningsLogged.add(String.format(format, arg));
        if (internalLogger != null && internalLogger.isWarnEnabled(marker)) {
            internalLogger.warn(marker, format, arg);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warningsLogged.add(String.format(format, arg1, arg2));
        if (internalLogger != null && internalLogger.isWarnEnabled(marker)) {
            internalLogger.warn(marker, format, arg1, arg2);
        }
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        warningsLogged.add(String.format(format, arguments));
        if (internalLogger != null && internalLogger.isWarnEnabled(marker)) {
            internalLogger.warn(marker, format, arguments);
        }
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        warningsLogged.add(msg);
        if (internalLogger != null && internalLogger.isWarnEnabled(marker)) {
            internalLogger.warn(marker, msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        // Always enable errors so we can track them
        return true;
    }

    @Override
    public void error(String msg) {
        errorsLogged.add(msg);
        if (internalLogger != null && internalLogger.isErrorEnabled()) {
            internalLogger.error(msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        errorsLogged.add(String.format(format, arg));
        if (internalLogger != null && internalLogger.isErrorEnabled()) {
            internalLogger.error(format, arg);
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        errorsLogged.add(String.format(format, arg1, arg2));
        if (internalLogger != null && internalLogger.isErrorEnabled()) {
            internalLogger.error(format, arg1, arg2);
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        errorsLogged.add(String.format(format, arguments));
        if (internalLogger != null && internalLogger.isErrorEnabled()) {
            internalLogger.error(format, arguments);
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        errorsLogged.add(msg);
        if (internalLogger != null && internalLogger.isErrorEnabled()) {
            internalLogger.error(msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        // Always enable error so we can track them.
        return true;
    }

    @Override
    public void error(Marker marker, String msg) {
        errorsLogged.add(msg);
        if (internalLogger != null && internalLogger.isErrorEnabled(marker)) {
            internalLogger.error(marker, msg);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        errorsLogged.add(String.format(format, arg));
        if (internalLogger != null && internalLogger.isErrorEnabled(marker)) {
            internalLogger.error(marker, format, arg);
        }
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        errorsLogged.add(String.format(format, arg1, arg2));
        if (internalLogger != null && internalLogger.isErrorEnabled(marker)) {
            internalLogger.error(marker, format, arg1, arg2);
        }
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        errorsLogged.add(String.format(format, arguments));
        if (internalLogger != null && internalLogger.isErrorEnabled(marker)) {
            internalLogger.error(marker, format, arguments);
        }
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        errorsLogged.add(msg);
        if (internalLogger != null && internalLogger.isErrorEnabled(marker)) {
            internalLogger.error(marker, msg, t);
        }
    }
}
