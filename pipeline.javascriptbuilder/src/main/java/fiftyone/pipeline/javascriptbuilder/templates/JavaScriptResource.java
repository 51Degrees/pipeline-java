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
    private final boolean _hasDelayedProperties;

    /**
     * Constructor
     * The callback mechanism is a feature that allows the client-side
     * data to be updated in the background when any properties that
     * contain JavaScript code have been executed on the client and
     * therefore, new evidence is available.
     * @param objName the name of the global-scope JavaScript object that will
     *                be created on the client-side by the JavaScript produced
     *                by the template.
     * @param jsonObject the JSON data payload to be inserted into the template.
     * @param supportsPromises if true, the template will produce JavaScript
     *                         that makes use of promises. If false, promises
     *                         will not be used.
     * @param url the complete URL to use for the callback mechanism described
     *            in remarks for this constructor.
     * @param enableCookies if false, any cookies created by JavaScript
     *                      properties that execute on the client-side and that
     *                      start with '51D_' will be deleted automatically.
     * @param updateEnabled true to use the callback mechanism that is described
     *                      in remarks for this constructor. False to disable
     *                      that mechanism. In this case, a second request must
     *                      be initiated by the user in order for the server to
     *                      access the additional evidence gathered by
     *                      client-side code.
     * @param hasDelayedProperties true to include support for JavaScript
     *                             properties that are not executed immediately
     *                             when the JavaScript is loaded.
     */
    public JavaScriptResource(
        String objName,
        String jsonObject,
        boolean supportsPromises,
        String url,
        boolean enableCookies,
        boolean updateEnabled,
        boolean hasDelayedProperties)
    {
        _objName = objName;
        _jsonObject = jsonObject.isEmpty() == false
            ? jsonObject : "{\"errors\":[\"Json data missing.\"]}";
        _supportsPromises = supportsPromises;
        _url = url;
        _enableCookies = enableCookies;
        _updateEnabled = updateEnabled;
        _hasDelayedProperties = hasDelayedProperties;
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
        hash.put("_hasDelayedProperties", _hasDelayedProperties);

        return hash;
    }
}
//! [class]
