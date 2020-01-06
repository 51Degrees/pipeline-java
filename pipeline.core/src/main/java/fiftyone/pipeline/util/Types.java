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

package fiftyone.pipeline.util;

import fiftyone.pipeline.core.flowelements.FlowElementBase;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Types {

    public static Map<Class<?>, Class<?>> getPrimativeTypeMap() {
        Map<Class<?>, Class<?>> map = new HashMap<>();
        map.put(boolean.class, Boolean.class);
        map.put(int.class, Integer.class);
        map.put(float.class, Float.class);
        map.put(double.class, Double.class);
        map.put(long.class, Long.class);
        return map;
    }

    public static Class<?> findSubClassParameterType(Object instance, Class<?> classOfInterest, int parameterIndex) {
        Class<?> instanceClass = instance.getClass();
        while (classOfInterest != instanceClass.getSuperclass()) {
            instanceClass = instanceClass.getSuperclass();
            if (instanceClass == null) return null;
        }

        ParameterizedType parameterizedType = (ParameterizedType) instanceClass.getGenericSuperclass();
        Type actualType = parameterizedType.getActualTypeArguments()[parameterIndex];

        return (Class<?>) actualType;
    }

    public static Class<?> getDataTypeFromElement(Object instance) {
        return findSubClassParameterType(instance, FlowElementBase.class, 0);
    }
}
