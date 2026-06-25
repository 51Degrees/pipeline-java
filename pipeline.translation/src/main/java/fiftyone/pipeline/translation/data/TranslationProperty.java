/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.translation.data;

/**
 * Defines a translation from one property to another.
 */
public class TranslationProperty {

    private final String sourceProperty;
    private final String destinationProperty;

    /**
     * Construct a new instance.
     * @param source the name of the source property on the source element data
     * @param destination the name of the destination property to store the
     *                    translated value under on the translation engine data
     */
    public TranslationProperty(String source, String destination) {
        this.sourceProperty = source;
        this.destinationProperty = destination;
    }

    /**
     * Source property name on the source element data.
     * @return source property name
     */
    public String getSourceProperty() {
        return sourceProperty;
    }

    /**
     * Destination property name on the translation engine data.
     * @return destination property name
     */
    public String getDestinationProperty() {
        return destinationProperty;
    }
}
