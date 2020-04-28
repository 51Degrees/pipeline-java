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
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.flowelements.FlowElement;

import java.util.List;

/**
 * Basic implementation of the {@link AspectPropertyMetaData} interface. Implements
 * public getters for all items.
 *
 * Values are set on construction. Instances are immutable.
 */
public class AspectPropertyMetaDataDefault
    extends ElementPropertyMetaDataDefault implements AspectPropertyMetaData {

    private final List<String> dataTiersWherePresent;

    /**
     * Construct a new instance
     * @param name name of the property
     * @param element the element which the property belongs to
     * @param category the category which the property belongs to
     * @param type the type of value returned by the property
     * @param dataTiersWherePresent data tiers which contain this property
     * @param available true if the property is available
     */
    public AspectPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class type,
        List<String> dataTiersWherePresent,
        boolean available) {
        super(name, element, category, type, available);
        this.dataTiersWherePresent = dataTiersWherePresent;
    }

    /**
     * Construct a new instance
     * @param name name of the property
     * @param element the element which the property belongs to
     * @param category the category which the property belongs to
     * @param type the type of value returned by the property
     * @param dataTiersWherePresent data tiers which contain this property
     * @param available true if the property is available
     * @param itemProperties list of sub-properties
     */
    public AspectPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class type,
        List<String> dataTiersWherePresent,
        boolean available,
        List<ElementPropertyMetaData> itemProperties) {
        super(name, element, category, type, available, itemProperties);
        this.dataTiersWherePresent = dataTiersWherePresent;
    }

    @Override
    public List<String> getDataTiersWherePresent() {
        return dataTiersWherePresent;
    }
}
