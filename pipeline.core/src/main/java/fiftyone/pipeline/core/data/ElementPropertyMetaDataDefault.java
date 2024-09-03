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

package fiftyone.pipeline.core.data;

import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElement;

import java.util.List;

/**
 * Default implementation of the {@link ElementPropertyMetaData} interface.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/properties.md#property-metadata">Specification</a>
 */
@SuppressWarnings("rawtypes")
public class ElementPropertyMetaDataDefault implements ElementPropertyMetaData {

    private final String name;
    private final FlowElement element;
    private final String category;
    private final Class<?> type;
    private final boolean available;
    private final List<ElementPropertyMetaData> itemProperties;
    private final boolean delayExecution;
    private final List<String> evidenceProperties;

    /**
     * Construct a new instance of {@link ElementPropertyMetaDataDefault}.
     * @param name the name of the property
     * @param element the element which the property belongs to
     * @param category the category the property belongs to
     * @param type the data type which values of the property have
     * @param available true if the property is available
     */
    public ElementPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class<?> type,
        boolean available) {
        this(
            name,
            element,
            category,
            type,
            available,
            null,
            false,
            null);
    }

    /**
     * Construct a new instance of {@link ElementPropertyMetaDataDefault}.
     * @param name name of the property
     * @param element the element which the property belongs to
     * @param category the category which the property belongs to
     * @param type the type of value returned by the property
     * @param available true if the property is available
     * @param itemProperties list of sub-properties contained within the
     *                       property
     */
    public ElementPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class<?> type,
        boolean available,
        List<ElementPropertyMetaData> itemProperties) {
        this(
            name,
            element,
            category,
            type,
            available,
            itemProperties,
            false,
            null);
    }

    /**
     * Construct a new instance of {@link ElementPropertyMetaDataDefault}.
     * @param name the name of the property
     * @param element the element which the property belongs to
     * @param category the category the property belongs to
     * @param type the data type which values of the property have
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
    public ElementPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class<?> type,
        boolean available,
        List<ElementPropertyMetaData> itemProperties,
        boolean delayExecution,
        List<String> evidenceProperties) {
        this.name = name;
        this.element = element;
        this.category = category;
        this.type = type;
        this.available = available;
        this.itemProperties = itemProperties;
        this.delayExecution = delayExecution;
        this.evidenceProperties = evidenceProperties;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FlowElement getElement() {
        return element;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public List<ElementPropertyMetaData> getItemProperties() {
        return itemProperties;
    }

    @Override
    public boolean getDelayExecution() {
        return delayExecution;
    }

    @Override
    public List<String> getEvidenceProperties() {
        return evidenceProperties;
    }
}
