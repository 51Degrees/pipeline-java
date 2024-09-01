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

package fiftyone.pipeline.util;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.TextStringBuilder;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Used for Interpolation of ${variable} in PipelineOptions and elsewhere
 */
public class FiftyOneLookup {

    private static final Map<String, StringLookup> customLookups= new HashMap<>();
    static {
        customLookups.put("envsys", new EnvSysLookup());
        customLookups.put("filepath", new FilePathLookup());
    }
    public static StringLookup FIFTYONE_LOOKUP =
            StringLookupFactory.INSTANCE.interpolatorStringLookup(customLookups,null,true);
    /**
     * Try to find a value from environment variable (first) or Java System Property.
     */
    static class EnvSysLookup implements StringLookup {

        @Override
        public String lookup(String key) {
            String val = System.getenv(key);
            if (val == null) {
                val = System.getenv(key.toUpperCase(Locale.ROOT));
                if (val == null) {
                    val = System.getProperty(key);
                }
            }
            return val;
        }
    }

    /**
     * Try to find a file according to the process described at {@link FileFinder#getFilePath},
     * which matches files ending in the key, as long as the key describes whole file
     * path components.
     */
    static class FilePathLookup implements StringLookup {

        @Override
        public String lookup(String key) {
            try {
                return getFilePath(key).getAbsolutePath();
            } catch (Exception e) {
                return null;
            }
        }
    }
    public static StringSubstitutor  getSubstitutor() {
        return new FiftyOneStringSubstitutor(FIFTYONE_LOOKUP);
    }

    /**
     * Subclass to obtain variable resolution behaviour that we want, which is that if
     * no key is specified then we want to run envsys:. If we specify this as the
     * default lookup then failed lookups for everything else get looked up
     * via envsys, which means that - for example - ${sys:USER} yields ${env:USER}
     * which is not what we want.
     */
    public static class FiftyOneStringSubstitutor extends StringSubstitutor {
        public FiftyOneStringSubstitutor(StringLookup variableResolver) {
            super(variableResolver);
            setEnableSubstitutionInVariables(true);
        }

        @Override
        protected String resolveVariable(String variableName, TextStringBuilder buf, int startPos
                , int endPos) {
            // the definition split char and string in Lookup are all protected so
            // we will use a literal rather than trying to do it "properly"
            if (variableName.contains(":")) {
                return super.resolveVariable(variableName, buf, startPos, endPos);
            }
                return super.resolveVariable("envsys:" + variableName, buf, startPos, endPos);
        }
    }
}
