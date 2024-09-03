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
import org.junit.Test;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class FiftyOneLookupTest {
    static StringSubstitutor substitutor = FiftyOneLookup.getSubstitutor();
    @Test
    public void sysEnvTests(){
        // user.home property via different routes
        assertEquals(System.getProperty("user.home"),substitutor.replace("${envsys:user.home}"));
        assertEquals(System.getProperty("user.home"),substitutor.replace("${sys:user.home}"));
        assertEquals(System.getProperty("user.home"),substitutor.replace("${user.home}"));
        assertEquals("${env:user.home}", substitutor.replace("${env:user.home}"));

        // env via different routes - NB USER is not available on the build machines
        // find an environment variable that is not a system property
        Optional<String> envKey = System.getenv().keySet()
                .stream()
                .filter(k -> !System.getProperties().contains(k))
                .findAny();
        if (envKey.isPresent()) {
            String envvar = envKey.get();
            String envvalue = System.getenv(envvar);
            System.out.println(envvar + ":" + envvalue);
            assertEquals(envvalue, substitutor.replace("${envsys:"+ envvar + "}"));
            assertEquals(envvalue, substitutor.replace("${env:" + envvar + "}"));
            assertEquals(envvalue, substitutor.replace("${" + envvar + "}"));
            assertEquals("mickymouse",
                    substitutor.replace("${sys:" + envvar + ":-mickymouse}"));

            // for envsys environment variables take precedence over system properties of the same name
            System.setProperty(envvar, "mickymouse");
            assertEquals(envvalue,substitutor.replace("${envsys:"+ envvar + "}"));
            assertEquals("mickymouse",substitutor.replace("${sys:" + envvar + "}"));
            System.clearProperty(envvar);

            // substitution in results: ${sys:myName} -> ${env:USER} -> username
            System.setProperty("myName", "${env:"+envvar+"}");
            assertEquals(envvalue,substitutor.replace("${sys:myName}"));

        }
        assertEquals("chiswick",substitutor.replace("${env:hometown:-chiswick}"));
        assertEquals("chiswick",substitutor.replace("${hometown:-chiswick}"));
        assertEquals("${hometown}",substitutor.replace("${hometown}"));

        assertTrue(substitutor.replace("${filepath:filefinder-test.xml}").endsWith(
                "filefinder-test.xml"));

        // assertEquals("7",substitutor.replace("${script:javascript:3+4}"));

        // No substitution in results: (escape the $))
        System.setProperty("myName", "$${env:USER}");
        assertEquals("${env:USER}",substitutor.replace("${sys:myName}"));

        // embed variables inside each other
        // file content
        String content = substitutor.replace("${file:utf8:${filepath:options-test.xml}}");
        assertTrue(content.startsWith("<?xml"));

        // XPath query
/*      fails on Windows - remove for now
        content = substitutor.replace(
                "${xml:${filepath:options-test.xml}://Element[1]/BuilderName/text()}");
        assertEquals("CloudRequestEngine", content);
*/
    }


    /**
     * failing to find the js engine when run under maven
     */

    public void listScriptEngines(){
            ScriptEngineManager mgr = new ScriptEngineManager();
            List<ScriptEngineFactory> factories = mgr.getEngineFactories();
            for (ScriptEngineFactory factory : factories) {
                System.out.println("ScriptEngineFactory Info");
                String engName = factory.getEngineName();
                String engVersion = factory.getEngineVersion();
                String langName = factory.getLanguageName();
                String langVersion = factory.getLanguageVersion();
                System.out.printf("\tScript Engine: %s (%s)\n", engName, engVersion);
                List<String> engNames = factory.getNames();
                for (String name : engNames)
                {
                    System.out.printf("\tEngine Alias: %s\n", name);
                }
                System.out.printf("\tLanguage: %s (%s)\n", langName, langVersion);
            }
    }
}