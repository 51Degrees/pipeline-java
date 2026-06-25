/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.translation.util;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses the flat <code>key: value</code> YAML maps used by the translation
 * files (e.g. <code>countrycodes.en_GB.yml</code>,
 * <code>countries.fr_FR.yml</code>) into an insertion-ordered map.
 * <p>
 * Uses snakeyaml-engine (YAML 1.2). YAML 1.2 is important here: the country
 * code file contains the key <code>NO</code> (Norway), which YAML 1.1 parsers
 * (including the classic snakeyaml library) would resolve to the boolean
 * <code>false</code>. Under YAML 1.2 it correctly remains the string
 * "NO".
 */
public class YamlTranslations {

    private YamlTranslations() {
    }

    /**
     * Parse a flat <code>key: value</code> YAML document into an insertion
     * ordered map. Keys and values are returned as strings.
     * @param yaml the YAML document contents, or null
     * @return an ordered map of the parsed entries (never null)
     */
    public static Map<String, String> parse(String yaml) {
        Map<String, String> result = new LinkedHashMap<>();
        if (yaml == null || yaml.isEmpty()) {
            return result;
        }
        LoadSettings settings = LoadSettings.builder().build();
        Object loaded = new Load(settings).loadFromString(yaml);
        if (loaded instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) loaded).entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String key = entry.getKey().toString();
                String value = entry.getValue() == null
                    ? ""
                    : entry.getValue().toString();
                result.put(key, value);
            }
        }
        return result;
    }
}
