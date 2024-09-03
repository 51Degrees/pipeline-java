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

package fiftyone.pipeline.core.typed;

import static fiftyone.pipeline.util.Check.getNotNull;
import static fiftyone.pipeline.util.Check.guard;

/**
 * Default implementation of {@link TypedKey}.
 * @param <T> type of the key
 */
public class TypedKeyDefault<T> implements TypedKey<T> {

    private final String name;

    private final Class<T> type;

    /**
     *  Construct a new instance with the name provided, and {@link Object} as
     *  the type
     * @param name the name of the key
     */
    @SuppressWarnings("unchecked")
    public TypedKeyDefault(String name) {
        this(name, Object.class);
    }

    /**
     * Construct a new instance with the name and type provided.
     * @param name the name of the key
     * @param type the type of the key
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public TypedKeyDefault(String name, Class type) {
        //noinspection ConstantConditions
        this.name = getNotNull(name, "Name must not be null").trim();
        guard(name.isEmpty(), "Name must not be empty");
        // Not checked as argument cannot be constrained due to the non-typed
        // constructor
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getType() {
        return type;
    }
}
