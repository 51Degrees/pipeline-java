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

package fiftyone.pipeline.util;

import java.nio.file.Paths;
import java.util.Objects;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

/**
 * Helper to guard that an argument fulfills its contract
 */
public class Check {
    /**
     * Throw an {@link IllegalArgumentException} if a condition is not met.
     * @param condition result of a conditional expression
     * @param message message to add to the exception if one is thrown
     */
    public static void guard(boolean condition, String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks if a value is null, and throws an {@link IllegalArgumentException}
     * if it is.
     * @param value to check
     * @param message message to add to the exception if one is thrown
     * @param <T> type of the value
     * @return the value that was checked
     */
    public static <T> T getNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static boolean notNullOrBlank(String arg){
        return Objects.nonNull(arg) && isFalse(arg.trim().isEmpty());
    }

    public static boolean isNullOrBlank(String arg){
        return Objects.isNull(arg) || arg.trim().isEmpty();
    }

    public static boolean fileExists(String pathString) {
        if (isNullOrBlank(pathString)) {
            return false;
        }
        return Paths.get(pathString).toFile().exists();
    }
    public static boolean notFileExists(String pathString) {
        return !fileExists(pathString);
    }
}
