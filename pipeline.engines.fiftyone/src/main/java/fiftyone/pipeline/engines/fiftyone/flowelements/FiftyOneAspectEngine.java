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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.fiftyone.data.*;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;

/**
 * 51Degrees specific Engine interface. This adds the concept of license keys to
 * the standard Engine interface.
 * @param <TData> the type of aspect data that the flow element will write to
 * @param <TProperty> the type of meta data that the flow element will supply
 *                    about the properties it populates.
 */
public interface FiftyOneAspectEngine<
    TData extends AspectData,
    TProperty extends FiftyOneAspectPropertyMetaData>
    extends OnPremiseAspectEngine<TData, TProperty> {

    /**
     * Get all profiles contained in the data set which the engine is using.
     * @return a {@link CloseableIterable} containing all profiles
     */
    CloseableIterable<ProfileMetaData> getProfiles();

    /**
     * Get the profile with the unique profile id from the data set.
     * @param profileId unique profile id
     * @return the profile with the id, or null if not found
     * @throws Exception if an exception was thrown internally
     */
    ProfileMetaData getProfile(int profileId) throws Exception;

    /**
     * Get all data components contained in the data set which the engine is
     * using e.g. Hardware, Software etc.
     * @return a {@link CloseableIterable} containing all components
     */
    CloseableIterable<ComponentMetaData> getComponents();

    /**
     * Get the component with the specified name from the data set.
     * @param name the name of the component
     * @return the component, or null if not found
     * @throws Exception if an exception was thrown internally
     */
    ComponentMetaData getComponent(String name) throws Exception;

    /**
     * Get all property values contained in the data set which the engine is
     * using.
     * @return a {@link CloseableIterable} containing all values
     */
    CloseableIterable<ValueMetaData> getValues();

    /**
     * Get the component with the specified property and value name from the
     * data set.
     * @param propertyName the name of the property which the value belongs to
     * @param valueName the name of the value
     * @return the value or, null if not found
     * @throws Exception if an exception was thrown internally
     */
    ValueMetaData getValue(String propertyName, String valueName) throws Exception;
}
