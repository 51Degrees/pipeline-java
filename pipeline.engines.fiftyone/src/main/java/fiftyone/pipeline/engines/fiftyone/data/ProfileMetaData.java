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
 * Meta data relating to a profile within the data set.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/data-model-specification/README.md#profile">Specification</a>
 */
public interface ProfileMetaData extends Closeable {

    /**
     * Get the unique id of the profile.
     * @return unique id of the profile
     */
    int getProfileId();

    /**
     * Get the values which are defined in the profile (for some Engines
     * multiple profiles are required to build a full set of results).
     * @return values defined in the profile
     */
    Iterable<ValueMetaData> getValues();

    /**
     * Gets the values associated with the profile and the property name.
     * @param propertyName to get the values for
     * @return values matching the property
     */
    Iterable<ValueMetaData> getValues(String propertyName);

    /**
     * If there is a value for the profile with the property name and value then
     * return an instance of it.
     * @param propertyName to get the value for
     * @param valueName value to look for
     * @return value instance for property and value, or null if it doesn't
     * exist
     */
    ValueMetaData getValue(String propertyName, String valueName);

    /**
     * The component which the profile belongs to.
     * @return the component for the profile
     */
    ComponentMetaData getComponent();

    /**
     * The name of the profile. Usually indicates the type of device.
     * @return name of the profile
     */
    String getName();
}
