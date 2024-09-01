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

package fiftyone.pipeline.engines.fiftyone.data;

import java.io.Closeable;

/**
 * Meta data relating to a specific value within a data set.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/data-model-specification/README.md#value">Specification</a>
 */
public interface ValueMetaData extends Closeable {
    /**
     * Get the property which the value relates to i.e. the value is a value
     * which can be returned by the property.
     * @return the property relating to the value
     */
    FiftyOneAspectPropertyMetaData getProperty();

    /**
     * Get the name of the value e.g. "True" or "Samsung".
     * @return name of the value
     */
    String getName();

    /**
     * Get the full description of the value.
     * @return value description
     */
    String getDescription();

    /**
     * Get the URL relating to the value if more information is available.
     * @return URL for more information
     */
    String getUrl();
}
