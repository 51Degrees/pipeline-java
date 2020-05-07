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

package fiftyone.pipeline.javascriptbuilder.templates;

import java.util.HashMap;

//! [class]
public class JavaScriptResource {
    private final String _objName;
    private final String _jsonObject;
    private final boolean _supportsPromises;
    private final String _url;
    private final boolean _enableCookies;
    private final boolean _updateEnabled;

    public JavaScriptResource(
        String objName,
        String jsonObject,
        boolean supportsPromises,
        String url,
        boolean enableCookies,
        boolean updateEnabled)
    {
        _objName = objName;
        _jsonObject = jsonObject.isEmpty() == false
            ? jsonObject : "{\"errors\":[\"Json data missing.\"]}";
        _supportsPromises = supportsPromises;
        _url = url;
        _enableCookies = enableCookies;
        _updateEnabled = updateEnabled;
    }

    public HashMap<String, Object> asMap()
    {
        HashMap<String, Object> hash = new HashMap<>();

        hash.put("_objName", _objName);
        hash.put("_jsonObject", _jsonObject);
        hash.put("_supportsPromises", _supportsPromises);
        hash.put("_url", _url);
        hash.put("_enableCookies", _enableCookies);
        hash.put("_updateEnabled", _updateEnabled);

        return hash;
    }
}
//! [class]
