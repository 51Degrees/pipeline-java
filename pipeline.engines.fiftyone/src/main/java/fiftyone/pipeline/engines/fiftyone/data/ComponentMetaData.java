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
 * Meta data relating to a component of an Engine's results e.g. Hardware.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/data-model-specification/README.md#component">Specification</a>
 */
public interface ComponentMetaData extends Closeable {

    /**
     * Get the unique Id of the component.
     * @return unique component id
     */
    byte getComponentId();

    /**
     * Get the name of the component.
     * @return component name
     */
    String getName();

    /**
     * Get the default profile which is used by the Engine for this component.
     * @return default profile for this component
     */
    ProfileMetaData getDefaultProfile();

    /**
     * List of the properties which come under the umbrella of this component.
     * @return list of properties
     */
    Iterable<FiftyOneAspectPropertyMetaData> getProperties();

    /**
     * Get the property from the component which has the name provided. Null is
     * returned if the component does not contain a property with the name
     * provided.
     * @param propertyName name of the property to return
     * @return the property or null if not in this component
     */
    FiftyOneAspectPropertyMetaData getProperty(String propertyName);
}
