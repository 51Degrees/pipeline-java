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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElement;

import java.util.List;

/**
 * Basic implementation of the {@link AspectPropertyMetaData} interface.
 * Values are set on construction. Instances are immutable.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/properties.md#aspect-property-metadata">Specification</a>
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
    @SuppressWarnings("rawtypes")
    public AspectPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class<?> type,
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
     * @param itemProperties list of sub-properties contained within the
     *                       property
     */
    @SuppressWarnings("rawtypes")
    public AspectPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class<?> type,
        List<String> dataTiersWherePresent,
        boolean available,
        List<ElementPropertyMetaData> itemProperties) {
        this(
            name,
            element,
            category,
            type,
            dataTiersWherePresent,
            available,
            itemProperties,
            false,
            null);
    }

    /**
     * Construct a new instance
     * @param name name of the property
     * @param element the element which the property belongs to
     * @param category the category which the property belongs to
     * @param type the type of value returned by the property
     * @param dataTiersWherePresent data tiers which contain this property
     * @param available true if the property is available
     * @param itemProperties list of sub-properties contained within the
     *                       property
     * @param delayExecution only relevant if the type is {@link JavaScript}.
     *                       Defaults to false. If set to true then the
     *                       JavaScript in this property will not be executed
     *                       automatically on the client device.
     * @param evidenceProperties the names of any {@link JavaScript} properties
     *                           that, when executed, will obtain additional
     *                           evidence that can help in determining the value
     *                           of this property. Note that these names should
     *                           include any parts after the element data key.
     *                           I.e. if the complete property name is
     *                           'devices.profiles.screenwidthpixelsjavascript'
     *                           then the/ name in this list must be
     *                           'profiles.screenwidthpixelsjavascript'
     */
    @SuppressWarnings("rawtypes")
    public AspectPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class<?> type,
        List<String> dataTiersWherePresent,
        boolean available,
        List<ElementPropertyMetaData> itemProperties,
        boolean delayExecution,
        List<String> evidenceProperties) {
        super(
            name,
            element,
            category,
            type,
            available,
            itemProperties,
            delayExecution,
            evidenceProperties);
        this.dataTiersWherePresent = dataTiersWherePresent;
    }

    @Override
    public List<String> getDataTiersWherePresent() {
        return dataTiersWherePresent;
    }
}
