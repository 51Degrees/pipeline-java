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

package fiftyone.pipeline.core.data;

import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElement;

import java.util.List;

/**
 * Defines a property that can be returned by a {@link FlowElement}. This is
 * stored in the element itself, so if a property is not included for some reason
 * (e.g. not in a data file, or excluded from config) then the engine still has
 * knowledge of its existence.
 *
 * An ElementPropertyMetaData is a property of an Element of a request. E.g.
 * ‘HardwareModel’ is a property of the ‘Device’ aspect. They define meta-data
 * such as property name, data type, the data file types the property is present
 * in and a flag indicating if the property is disabled.
 */
public interface ElementPropertyMetaData {

    /**
     * Get the name of the property.
     *
     * @return property name
     */
    String getName();

    /**
     * Get the element which returns this property.
     *
     * @return {@link FlowElement} instance
     */
    @SuppressWarnings("rawtypes")
    FlowElement getElement();

    /**
     * The category the property belongs to.
     * @return property category
     */
    String getCategory();

    /**
     * Get the type of data which the property refers to e.g. {@link String}.
     * @return variable type class
     */
    Class<?> getType();

    /**
     * Get whether or not the property is available in the results // todo
     * @return true if the property is available
     */
    boolean isAvailable();

    /**
     * This is only relevant where Type is a collection of complex
     * objects.
     * It contains a list of the property meta-data for the
     * items in the value for this property.
     * For example, if this meta-data instance represents a list of
     * hardware devices, ItemProperties will contain a list of the
     * meta-data for properties available on each hardware device
     * element within that list.
     */
    List<ElementPropertyMetaData> getItemProperties();


    /**
     * Only relevant if the type is {@link JavaScript}. Defaults to false.
     * If set to true then the JavaScript in this property will
     * not be executed automatically on the client device.
     * This is used where executing the JavaScript would result in
     * undesirable behavior.
     * For example, attempting to access the location of the device
     * will cause the browser to show a pop-up confirming if the
     * user is happy too allow the website access to their location.
     * In general, we don't want this to happen immediately when a
     * user enters a website, but when they try to use a feature that
     * requires location data (e.g. show restaurants near me).
     * @return true if execution should be delayed
     */
    boolean getDelayExecution();

    /**
     * Get the names of any {@link JavaScript} properties that, when executed,
     * will obtain additional evidence that can help in determining the value of
     * this property. For example, the ScreenPixelsWidthJavaScript property will
     * get the pixel width of the client-device's screen.
     * This is used to update the ScreenPixelsWidth property.
     * As such, ScreenPixelsWidth will have ScreenPixelWidthJavaScript
     * in its list of evidence properties.
     * @return list of evidence properties
     */
    List<String> getEvidenceProperties();
}