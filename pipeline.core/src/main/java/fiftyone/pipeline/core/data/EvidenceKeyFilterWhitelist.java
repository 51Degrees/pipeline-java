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

import java.util.*;

public class EvidenceKeyFilterWhitelist implements EvidenceKeyFilter {

    protected Map<String, Integer> whitelist;

    protected Comparator<String> comparator;

    public EvidenceKeyFilterWhitelist(List<String> whitelist) {
        this.whitelist = new TreeMap<>();
        addValues(whitelist);
        this.comparator = null;
    }

    public EvidenceKeyFilterWhitelist(
        List<String> whitelist,
        Comparator<String> comparator) {
        this.whitelist = new TreeMap<>(comparator);
        addValues(whitelist);
        this.comparator = comparator;
    }

    public EvidenceKeyFilterWhitelist(Map<String, Integer> whitelist) {
        this.whitelist = new TreeMap<>();
        addValues(whitelist);
        this.comparator = null;
    }

    public EvidenceKeyFilterWhitelist(
        Map<String, Integer> whitelist,
        Comparator<String> comparator) {
        this.whitelist = new TreeMap<>(comparator);
        addValues(whitelist);
        this.comparator = comparator;
    }

    private void addValues(Map<String, Integer> values) {
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (whitelist.containsKey(entry.getKey()) == false) {
                whitelist.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private void addValues(List<String> values) {
        for (String value : values) {
            if (whitelist.containsKey(value) == false) {
                whitelist.put(value, 0);
            }
        }
    }

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
