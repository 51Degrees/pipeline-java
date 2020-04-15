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

import fiftyone.pipeline.core.flowelements.FlowElement;

import java.util.List;

public class ElementPropertyMetaDataDefault implements ElementPropertyMetaData {

    private final String name;
    private final FlowElement element;
    private final String category;
    private final Class type;
    private final boolean available;
    private final List<ElementPropertyMetaData> itemProperties;

    public ElementPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class type,
        boolean available) {
        this(
            name,
            element,
            category,
            type,
            available,
            null);
    }

    public ElementPropertyMetaDataDefault(
        String name,
        FlowElement element,
        String category,
        Class type,
        boolean available,
        List<ElementPropertyMetaData> itemProperties) {
        this.name = name;
        this.element = element;
        this.category = category;
        this.type = type;
        this.available = available;
        this.itemProperties = itemProperties;
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
    public Class getType() {
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
}
