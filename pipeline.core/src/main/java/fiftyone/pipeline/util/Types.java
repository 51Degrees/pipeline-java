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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.flowelements.FlowElementBase;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Static type methods.
 */
public class Types {

    /**
     * Get a map of primitive types to their boxed type e.g. boolean and Boolean
     * @return primitive type map
     */
    public static Map<Class<?>, Class<?>> getPrimitiveTypeMap() {
        Map<Class<?>, Class<?>> map = new HashMap<>();
        map.put(boolean.class, Boolean.class);
        map.put(int.class, Integer.class);
        map.put(float.class, Float.class);
        map.put(double.class, Double.class);
        map.put(long.class, Long.class);
        return map;
    }

    /**
     * Get the type parameter of a class in relation to a parent class.
     * @param instance to get the type parameter of
     * @param classOfInterest a class above instance in the the inheritance
     *                        hierarchy which the type of instance defines a
     *                        generic type parameter of
     * @param parameterIndex index in the list of type parameters
     * @return the parameter type, or null if it could not be determined
     */
    public static Class<?> findSubClassParameterType(
        Object instance,
        Class<?> classOfInterest,
        int parameterIndex) {
        Class<?> instanceClass = instance.getClass();
        while (classOfInterest != instanceClass.getSuperclass()) {
            instanceClass = instanceClass.getSuperclass();
            if (instanceClass == null) return null;
        }

        ParameterizedType parameterizedType = (ParameterizedType) instanceClass.getGenericSuperclass();
        Type actualType = parameterizedType.getActualTypeArguments()[parameterIndex];

        return (Class<?>) actualType;
    }

    /**
     * Get the type of {@link ElementData} which be the result of an instance's
     * processing
     * @param instance an instance extending {@link FlowElementBase}
     * @return element data type or null
     */
    public static Class<?> getDataTypeFromElement(Object instance) {
        return findSubClassParameterType(instance, FlowElementBase.class, 0);
    }
}
