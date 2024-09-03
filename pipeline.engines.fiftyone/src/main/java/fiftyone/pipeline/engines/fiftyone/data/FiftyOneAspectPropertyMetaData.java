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

import fiftyone.pipeline.engines.data.AspectPropertyMetaData;

import java.io.Closeable;

/**
 * 51Degrees specific meta data. This adds meta data properties which are
 * available in 51Degrees Engines.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/data-model-specification/README.md#property">Specification</a>
 */
public interface FiftyOneAspectPropertyMetaData
    extends AspectPropertyMetaData, Closeable {

    /**
     * Get URL relating to the property.
     * @return URL relating to the property
     */
    String getUrl();

    /**
     * Get the order in which to display the property.
     * @return order to display the property
     */
    byte getDisplayOrder();

    /**
     * Get whether the property is mandatory.
     * @return true if the property is mandatory
     */
    boolean getMandatory();

    /**
     * Get whether the property value type is a list.
     * @return true if the property value type is a list
     */
    boolean getList();

    /**
     * Get whether the property is now obsolete.
     * @return true if the property is obsolete
     */
    boolean getObsolete();

    /**
     * Get whether the property should be shown.
     * @return true if the property should be shown
     */
    boolean getShow();

    /**
     * Get whether the values for the property should be shown.
     * @return true if the values for the property should be shown
     */
    boolean getShowValues();

    /**
     * Get the full description for the property.
     * @return description of the property
     */
    String getDescription();

    /**
     * Get the component which the property belongs to.
     * @return component for the property
     */
    ComponentMetaData getComponent();

    /**
     * Get the values which relate to the property i.e. the values which can be
     * returned for this property.
     * @return values which relate to the property
     */
    Iterable<ValueMetaData> getValues();

    /**
     * Get the default value for the property.
     * @return default value
     */
    ValueMetaData getDefaultValue();

    /**
     * Get the value from the property which has the name provided. Null is
     * returned if the property does not contain a value with the name provided.
     * @param valueName name of the property to return
     * @return the value or null if not in this property
     */
    ValueMetaData getValue(String valueName);
}
