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

package fiftyone.pipeline.core.data;

import java.util.*;

/**
 * This evidence filter will only include keys that are on a whitelist that is
 * specified at construction time.
 */
public class EvidenceKeyFilterWhitelist implements EvidenceKeyFilter {

    protected final Map<String, Integer> whitelist;

    protected final Comparator<String> comparator;

    /**
     * Construct a new instance using the list of evidence keys provided.
     * By default, all keys will have the same order of precedence.
     * @param whitelist evidence keys to add to the whitelist
     */
    public EvidenceKeyFilterWhitelist(List<String> whitelist) {
        this.whitelist = new TreeMap<>();
        addValues(whitelist);
        this.comparator = null;
    }

    /**
     * Construct a new instance using the list of evidence keys provided and
     * a custom string comparator to use when calling the
     * {@link #include(String)} method.
     * By default, all keys will have the same order of precedence.
     * @param whitelist evidence keys to add to the whitelist
     * @param comparator the string comparator to use on the keys
     */
    public EvidenceKeyFilterWhitelist(
        List<String> whitelist,
        Comparator<String> comparator) {
        this.whitelist = new TreeMap<>(comparator);
        addValues(whitelist);
        this.comparator = comparator;
    }

    /**
     * Construct a new instance using the map of evidence keys and order of
     * precedence provided.
     * The order of precedence of each key is given by the value of the
     * key/value pair.
     * @param whitelist evidence keys to add to the whitelist
     */
    public EvidenceKeyFilterWhitelist(Map<String, Integer> whitelist) {
        this.whitelist = new TreeMap<>();
        addValues(whitelist);
        this.comparator = null;
    }

    /**
     * Construct a new instance using the map of evidence keys and order of
     * precedence provided.
     * The order of precedence of each key is given by the value of the
     * key/value pair.
     * @param whitelist evidence keys to add to the whitelist
     * @param comparator the string comparator to use on the keys
     */
    public EvidenceKeyFilterWhitelist(
        Map<String, Integer> whitelist,
        Comparator<String> comparator) {
        this.whitelist = new TreeMap<>(comparator);
        addValues(whitelist);
        this.comparator = comparator;
    }

    /**
     * Add the keys to the internal map. This should only be called by
     * constructors.
     * @param values values to add
     */
    private void addValues(Map<String, Integer> values) {
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (whitelist.containsKey(entry.getKey()) == false) {
                whitelist.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Add the keys to the internal map. This should only be called by
     * constructors.
     * @param values values to add
     */
    private void addValues(List<String> values) {
        for (String value : values) {
            if (whitelist.containsKey(value) == false) {
                whitelist.put(value, 0);
            }
        }
    }

    /**
     * Get the internal list of whitelisted evidence keys along with their order
     * of precedence as an unmodifiable map.
     * @return internal whitelist
     */
    public Map<String, Integer> getWhitelist() {
        return Collections.unmodifiableMap(whitelist);
    }

    @Override
    public boolean include(String key) {
        return whitelist.containsKey(key);
    }

    @Override
    public Integer order(String key) {
        if (whitelist.containsKey(key)) {
            return whitelist.get(key);
        } else {
            return null;
        }
    }
}
