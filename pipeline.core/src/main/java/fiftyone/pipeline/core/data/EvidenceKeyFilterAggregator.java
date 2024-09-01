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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This implementation of {@link EvidenceKeyFilter} aggregates multiple other
 * filters using a logical OR approach. I.e. if any one of the child filters
 * would allow the inclusion of an evidence key then this aggregator will allow
 * it as well, even if none of the other child filters do.
 */
public class EvidenceKeyFilterAggregator extends EvidenceKeyFilterWhitelist {

    private final List<EvidenceKeyFilter> filters;

    /**
     * Construct a new empty instance where the evidence keys are case
     * insensitive.
     */
    public EvidenceKeyFilterAggregator() {
        super(new ArrayList<String>(), String.CASE_INSENSITIVE_ORDER);
        filters = new ArrayList<>();
    }

    /**
     * Add a child filter to this aggregator.
     * @param filter child filter to add
     */
    public void addFilter(EvidenceKeyFilter filter) {
        boolean addFilter = true;
        if (filter instanceof EvidenceKeyFilterWhitelist) {
            EvidenceKeyFilterWhitelist whitelistFilter =
                (EvidenceKeyFilterWhitelist) filter;
            // Only add this filter's whitelist to the overall whitelist
            // if it is case insensitive. Otherwise, we will have to
            // add the filter to the list of sub filters.
            if(whitelistFilter.comparator != null &&
                whitelistFilter.comparator == String.CASE_INSENSITIVE_ORDER) {
                addFilter = false;
                for (Map.Entry<String, Integer> entry :
                        whitelistFilter.whitelist.entrySet()) {
                    if (whitelist.containsKey(entry.getKey()) == false) {
                        whitelist.put(entry.getKey(), entry.getValue());
                    }
                }
                if (filter.getClass().getSuperclass() != null &&
                        filter.getClass().getSuperclass() == EvidenceKeyFilterWhitelist.class) {
                    addFilter = true;
                }
            }
        }
        if (addFilter == true) {
            filters.add(filter);
        }
    }

    @Override
    public boolean include(String key) {
        boolean include = super.include(key);

        int index = 0;
        while (include == false && index < filters.size()) {
            include = filters.get(index).include(key);
            index++;
        }

        return include;
    }

    @Override
    public Integer order(String key) {
        Integer order = super.order(key);

        int index = 0;
        while (order == null && index < filters.size()) {
            order = filters.get(index).order(key);
            index++;
        }
        return order;
    }
}
