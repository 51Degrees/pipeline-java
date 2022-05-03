package fiftyone.pipeline.util;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.TextStringBuilder;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

import java.util.HashMap;
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
                val = System.getProperty(key);
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
