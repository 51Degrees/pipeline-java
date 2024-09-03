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

package fiftyone.pipeline.engines.data;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;

public class JsonLoaderTests {

    private static final String jsonString = "{" +
        "aString: 'string value'," +
        "anInt: 12," +
        "aBool: true" +
        "}";

    public static class TestData {
        public TestData(){

        }
        private String aString;
        private Integer anInt;
        private Boolean aBool;
        public void setAString(String aString) {
            this.aString = aString;
        }
        public void setAnInt(Integer anInt) {
            this.anInt = anInt;
        }
        public void setABool(Boolean aBool) {
            this.aBool = aBool;
        }
        public String getAString() {
            return aString;
        }
        public Integer getAnInt() {
            return anInt;
        }
        public Boolean getABool() {
            return aBool;
        }
    }

    @Test
    public void JsonLoader_FromFile() throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        File file = File.createTempFile("JsonLoaderTest", "json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonString);
        }
        DataLoader<TestData> loader = new JsonLoader<TestData>();

        TestData data = loader.loadData(file.getAbsolutePath(), TestData.class);
        assertEquals("string value", data.getAString());
        assertEquals(12, (int)data.getAnInt());
        assertEquals(true, data.getABool());

    }

    @Test
    public void JsonLoader_FromBytes() throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        byte[] jsonBytes = jsonString.getBytes();
        DataLoader<TestData> loader = new JsonLoader<>();

        TestData data = loader.loadData(jsonBytes, TestData.class);
        assertEquals("string value", data.getAString());
        assertEquals(12, (int)data.getAnInt());
        assertEquals(true, data.getABool());
    }
}
