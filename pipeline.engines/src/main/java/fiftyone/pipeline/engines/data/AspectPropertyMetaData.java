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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;

import java.util.List;

/**
 * Defines a property that can be returned by an {@link AspectEngine}. This is
 * stored in the engine itself, so if a property is not included for some reason
 * (e.g. not in a data file, or excluded from config) then the engine still has
 * knowledge of its existence.
 * <p>
 * An AspectPropertyMetaData is a property of an Aspect of a request. E.g. ‘HardwareModel’
 * is a property of the ‘Device’ aspect. They define meta-data such as property
 * name, data type, the data file types the property is present in and a flag
 * indicating if the property is disabled.
 */
public interface AspectPropertyMetaData extends ElementPropertyMetaData {

    /**
     * A list of the data tiers that can be used to determine values for this
     * property. Examples values are:
     * Lite
     * Premium
     * Enterprise
     *
     * @return list of data tiers (empty if none)
     */
    List<String> getDataTiersWherePresent();
}
