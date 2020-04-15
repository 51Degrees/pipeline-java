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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessiblePropertyMetaData {

    public static class LicencedProducts {
        public LicencedProducts(JSONObject json) {
        this.products = new HashMap<>();
            for (String key : json.keySet()) {
                products.put(key, new ProductMetaData(json.getJSONObject(key)));
            }
        }
        public Map<String, ProductMetaData> products;
    }

    public static class ProductMetaData {
        public ProductMetaData() {
            this.properties = null;
            this.dataTier = null;
        }
        public ProductMetaData(JSONObject json) {
            this.properties = new ArrayList<>();
            dataTier = json.getString("DataTier");
            JSONArray jsonProperties = json.getJSONArray("Properties");
            for (int i = 0; i < jsonProperties.length(); i++) {
                properties.add(new PropertyMetaData(jsonProperties.getJSONObject(i)));
            }
        }
        public String dataTier;

        public List<PropertyMetaData> properties;
    }

    public static class PropertyMetaData {
        public PropertyMetaData(
            String name,
            String type,
            String category,
            List<PropertyMetaData> itemProperties) {
            this.name = name;
            this.type = type;
            this.category = category;
            this.itemProperties = itemProperties;
        }

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
        }

        public String name;

        public String type;

        public String category;

        public List<PropertyMetaData> itemProperties;

        public Class getPropertyType()
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
