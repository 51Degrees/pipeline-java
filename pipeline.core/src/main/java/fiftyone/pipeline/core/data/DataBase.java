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

package fiftyone.pipeline.core.data;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static fiftyone.pipeline.util.CheckArgument.checkNotNull;

public abstract class DataBase implements Data {

    protected final Logger logger;

    private Map<String, Object> data;

    public DataBase(Logger logger) {
        this(logger, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    public DataBase(Logger logger, Map<String, Object> data) {
        this.logger = logger;
        this.data = checkNotNull(data, "Data supplied must not be null");
    }

    @Override
    public Object get(String key) {
        return getAs(key, Object.class);
    }

    @Override
    public void put(String key, Object value) {
        if (data.containsKey(key)) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Data '" + getClass().getName() + "'-'" + hashCode() + "' " +
                        "overwriting existing value for '" + key + "' " +
                        "(old value '" + asTruncatedString(data.get(key)) + "', " +
                        "new value '" + asTruncatedString(value) + "').");
            }
        }
        data.put(key, value);
    }

    @Override
    public Map<String, Object> asKeyMap() {
        return Collections.unmodifiableMap(data);
    }

    @Override
    public void populateFromMap(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            this.data.put(entry.getKey(), entry.getValue());
        }
    }

    protected <T> T getAs(String key, Class<T> type, Class<?>... parameterisedTypes) {
        checkNotNull(key, "Supplied key must not be null");
        logger.debug("Data '" + getClass().getSimpleName() + "'-'" +
            hashCode() + "' property value requested for key '" +
            key + "'.");
        T result = null;
        if (data.containsKey(key)) {
            result = type.cast(data.get(key));
        }
        return result;
    }

    /**
     * The string representation of the specified object.
     */
    private String asTruncatedString(Object value) {
        String str = value == null ? "NULL" : value.toString();
        if (str.length() > 50) {
            str = str.substring(0, 47) + "...";
        }
        return str;
    }
}
