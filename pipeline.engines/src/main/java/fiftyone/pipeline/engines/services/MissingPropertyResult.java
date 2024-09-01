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

package fiftyone.pipeline.engines.services;

/**
 * Encapsulates the reason and explanation of why a property was missing
 * from a set of results.
 */
public class MissingPropertyResult {

    private final MissingPropertyReason reason;

    private final String description;

    /**
     * Construct a new instance of MissingPropertyResult.
     *
     * @param reason      reason for the missing property
     * @param description explanation for the missing property
     */
    public MissingPropertyResult(
        final MissingPropertyReason reason,
        final String description) {
        this.reason = reason;
        this.description = description;
    }

    /**
     * Get the reason the property was missing.
     *
     * @return {@link MissingPropertyReason} enum value
     */
    public MissingPropertyReason getReason() {
        return this.reason;
    }

    /**
     * Get the full explanation to why the property was missing.
     *
     * @return message to user
     */
    public String getDescription() {
        return this.description;
    }
}
