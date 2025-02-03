/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2025 51 Degrees Mobile Experts Limited, Davidson House,
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

/**
 * Represents an object that filters evidence key names based on some criteria.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/advertize-accepted-evidence.md">Specification</a>
 */
public interface EvidenceKeyFilter {

    /**
     * Check if the specified evidence key is included by this filter.
     * @param key the key to check
     * @return true if the key is included and false if not
     */
    boolean include(String key);

    /**
     * Get the order of precedence of the specified key.
     * @param key the key to check
     * @return the order, where lower values indicate a higher order of
     * precedence. Null if the key is not in the white list
     */
    Integer order(String key);
}
