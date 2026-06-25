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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal parser for the flat <code>key: value</code> YAML maps used by the
 * translation files (e.g. <code>countrycodes.en_GB.yml</code>,
 * <code>countries.fr_FR.yml</code>). These files contain a single mapping of
 * scalar string to scalar string with no nesting, anchors, flow collections or
 * quoting, so a full YAML parser is not required.
 * <p>
 * The returned map preserves the order in which entries appear in the file,
 * which the country engine relies on for the ordered list of all known
 * countries.
 */
public class YamlTranslations {

    /**
     * Unicode byte order mark (U+FEFF), stripped from the start of a file if
     * present.
     */
    private static final int BOM = 0xFEFF;

    private YamlTranslations() {
    }

    /**
     * Parse a flat <code>key: value</code> YAML document into an insertion
     * ordered map. Blank lines and lines beginning with <code>#</code> are
     * ignored, as are lines that contain no <code>:</code> separator. Keys and
     * values are trimmed. The first <code>:</code> on a line is treated as the
     * separator.
     * @param yaml the YAML document contents, or null
     * @return an ordered map of the parsed entries (never null)
     */
    public static Map<String, String> parse(String yaml) {
        Map<String, String> result = new LinkedHashMap<>();
        if (yaml == null || yaml.isEmpty()) {
            return result;
        }
        if (yaml.charAt(0) == BOM) {
            yaml = yaml.substring(1);
        }
        for (String rawLine : yaml.split("\n", -1)) {
            // Tolerate Windows line endings and surrounding whitespace.
            String line = rawLine.trim();
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            int separator = line.indexOf(':');
            if (separator < 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (key.isEmpty()) {
                continue;
            }
            result.put(key, value);
        }
        return result;
    }
}
