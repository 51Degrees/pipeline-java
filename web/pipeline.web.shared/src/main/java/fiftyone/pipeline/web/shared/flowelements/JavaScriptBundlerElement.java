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

package fiftyone.pipeline.web.shared.flowelements;

import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.web.shared.Constants;
import fiftyone.pipeline.web.shared.data.JavaScriptData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JavaScriptBundlerElement
        extends FlowElementBase<JavaScriptData, ElementPropertyMetaData> {

    private final List<ElementPropertyMetaData> properties;

    public JavaScriptBundlerElement(
        Logger logger,
        ElementDataFactory<JavaScriptData> elementDataFactory) {
        super(logger, elementDataFactory);
        this.properties = new ArrayList<>();
        properties.add(new ElementPropertyMetaDataDefault(
            Constants.CLIENTSIDE_JAVASCRIPT_PROPERTY_NAME,
            this,
            "javascript",
            String.class,
            true));
    }

    @Override
    public String getElementDataKey(){
        return Constants.CLIENTSIDE_JAVASCRIPT_DATA_KEY;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return properties;
    }

    @Override
    protected void processInternal(FlowData data) {
        StringBuilder completeJavaScript = new StringBuilder();

        // Start the class declaration, including constructor
        completeJavaScript.append("class FOD_CO { \n");
        completeJavaScript.append("constructor() {};\n");

        Map<String, String> javaScriptProperties = data.getWhere(
            new PropertyMatcher() {
                @Override
                public boolean isMatch(ElementPropertyMetaData property) {
                    return property.getType().equals(JavaScript.class);
                }
            }
        );

        // Add each JavaScript property value as a method on the class.
        for (Map.Entry<String, String> javaScriptProperty : javaScriptProperties.entrySet()) {
            completeJavaScript.append(javaScriptProperty.getKey()
                .replace('.', '_')
                .replace('-', '_'));
            completeJavaScript.append("() {\n");
            completeJavaScript.append(javaScriptProperty.getValue() + "\n");
            completeJavaScript.append("}\n");
        }
        completeJavaScript.append("}\n");

        // Add code to create an instance of the class and call each of
        // the methods on it.
        completeJavaScript.append("let fod_co = new FOD_CO();\n");
        for (Map.Entry<String, String> javaScriptProperty : javaScriptProperties.entrySet()) {
            completeJavaScript.append("fod_co.");
            completeJavaScript.append(javaScriptProperty.getKey()
                .replace('.', '_')
                .replace('-', '_'));
            completeJavaScript.append("();\n");
        }

        // Create the element data and add it to the flow data.
        JavaScriptData elementData = data.getOrAdd(getTypedDataKey(), getDataFactory());
        elementData.setJavaScript(completeJavaScript.toString());

    }

    @Override
    protected void managedResourcesCleanup() {

    }

    @Override
    protected void unmanagedResourcesCleanup() {

    }
}
