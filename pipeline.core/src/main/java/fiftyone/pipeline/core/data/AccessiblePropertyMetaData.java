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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains classes which deserialise JSON property definitions.
 */
public class AccessiblePropertyMetaData {

    /**
     * Class containing a list of {@link ProductMetaData}s.
     */
    public static class LicencedProducts {
        /**
         * Construct a new instance by constructing {@link ProductMetaData}s
         * from the JSON provided.
         * @param json the JSON map of product names to {@link ProductMetaData}
         */
        public LicencedProducts(JSONObject json) {
        this.products = new HashMap<>();
            for (String key : json.keySet()) {
                products.put(key, new ProductMetaData(json.getJSONObject(key)));
            }
        }
        public final Map<String, ProductMetaData> products;
    }

    /**
     * Licenced properties class used to deserialise accessible property
     * information from cloud services.
     */
    public static class ProductMetaData {
        /**
         * Construct an uninitialised instance.
         */
        public ProductMetaData() {
            this.properties = null;
            this.dataTier = null;
        }

        /**
         * Construct a new instance from a {@link JSONObject}.
         * @param json the JSON to construct the instance from
         */
        public ProductMetaData(JSONObject json) {
            this.properties = new ArrayList<>();
            dataTier = json.getString("DataTier");
            JSONArray jsonProperties = json.getJSONArray("Properties");
            for (int i = 0; i < jsonProperties.length(); i++) {
                properties.add(new PropertyMetaData(jsonProperties.getJSONObject(i)));
            }
        }

        /**
         * Accessible data tiers.
         */
        public String dataTier;

        /**
         * Accessible properties
         */
        public List<PropertyMetaData> properties;
    }

    /**
     * Standalone instance of {@link ElementPropertyMetaData}, used to serialise
     * element or aspect properties.
     */
    public static class PropertyMetaData {

        /**
         * Construct a new instance of {@link PropertyMetaData}.
         * @param name the name of the property
         * @param type the data type which values of the property have
         * @param category the category the property belongs to
         * @param itemProperties list of sub-properties contained within the
         *                       property
         */
        public PropertyMetaData(
            String name,
            String type,
            String category,
            List<PropertyMetaData> itemProperties) {
            this.name = name;
            this.type = type;
            this.category = category;
            this.itemProperties = itemProperties;
            this.delayExecution = false;
            this.evidenceProperties = null;
        }
        
        /**
         * Construct a new instance of {@link PropertyMetaData}.
         * @param name the name of the property
         * @param type the data type which values of the property have
         * @param category the category the property belongs to
         * @param itemProperties list of sub-properties contained within the
         *                       property
         * @param delayExecution delay execution flag
         * @param evidenceProperties a list of properties which supplement 
         *                           evidence for this property
         */
        public PropertyMetaData(
            String name,
            String type,
            String category,
            List<PropertyMetaData> itemProperties,
            Boolean delayExecution,
            List<String> evidenceProperties) {
            this.name = name;
            this.type = type;
            this.category = category;
            this.itemProperties = itemProperties;
            this.delayExecution = delayExecution;
            this.evidenceProperties = evidenceProperties;
        }

        /**
         * Construct a new instance from the JSON provided.
         * @param json JSON defining the {@link PropertyMetaData}
         */
        public PropertyMetaData(JSONObject json) {
            this.name = json.getString("Name");
            this.type = json.getString("Type");
            this.category = json.getString("Category");
            if (json.has("ItemProperties")) {
                JSONArray array = json.getJSONArray("ItemProperties");
                this.itemProperties = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    this.itemProperties.add(new PropertyMetaData(array.getJSONObject(i)));
                }
            }
            if (json.has("DelayExecution")) {
                this.delayExecution = json.getBoolean("DelayExecution");    
            }
            if(json.has("EvidenceProperties")) {
                JSONArray array = json.getJSONArray("EvidenceProperties");
                this.evidenceProperties = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    this.evidenceProperties.add(array.getString(i));
                }
            }
        }

        /**
         * Name of the property.
         */
        public String name;

        /**
         * Data type for values of the property as a string.
         */
        public String type;

        /**
         * Category the property belongs to.
         */
        public String category;

        /**
         * Sub-properties of the property if any.
         */
        public List<PropertyMetaData> itemProperties;
        
        /**
         * Delay execution flag.
         */
        public Boolean delayExecution;
        
        /**
         * Evidence properties.
         */
        public List<String> evidenceProperties;

        /**
         * Parse the {@link #type} string to the {@link Class} which it
         * represents.
         * @return type class for values of the property
         */
        public Class<?> getPropertyType()
        {
            switch (type)
            {
                case "String":
                    return String.class;
                case "Int32":
                    return int.class;
                case "Array":
                    return List.class;
                case "Boolean":
                    return boolean.class;
                case "JavaScript":
                    return JavaScript.class;
                case "Double":
                    return double.class;
                default:
                    throw new TypeNotPresentException(type, null);
            }
        }
    }
}
