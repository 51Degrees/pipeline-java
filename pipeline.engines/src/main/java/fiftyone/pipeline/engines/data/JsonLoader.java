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

import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON implementation of IDataLoader. This deserialises the data from a JSON
 * file to the type T.
 * @param <T> type of object to load
 */
public class JsonLoader<T> implements DataLoader<T> {

    private T loadData(Reader reader, Class<T> type) throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        int bufferLength = 1024;
        char[] buffer = new char[bufferLength];
        StringBuilder builder = new StringBuilder();
        int read = reader.read(buffer, 0, bufferLength);
        while (read > 0) {
            builder.append(buffer, 0, read);
            read = reader.read(buffer, 0, bufferLength);
        }
        String json = builder.toString();
        JSONObject jsonObj = new JSONObject(json);

        @SuppressWarnings("deprecation")
        T instance = type.newInstance();

        Map<String, Object> parameters = new HashMap<>();
        for (String key : jsonObj.keySet()) {
            parameters.put(key.toLowerCase(), jsonObj. get(key));
        }

        for (Method method : type.getMethods()) {
            if (method.getName().startsWith("set")) {
                String variableName = method.getName().substring("set".length()).toLowerCase();
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 1 && parameters.containsKey(variableName)) {
                    Object variable = parameters.get(variableName);
                    if (types[0].isAssignableFrom(variable.getClass())) {
                        method.invoke(instance, variable);
                    }
                }
            }
        }

        return instance;
    }

    @Override
    public T loadData(String filePath, Class<T> type) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try (Reader reader = new FileReader(filePath)) {
            return loadData(reader, type);
        }
    }

    @Override
    public T loadData(byte[] data, Class<T> type) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            try (Reader reader = new InputStreamReader(inputStream)) {
                return loadData(reader, type);
            }
        }
    }
}
