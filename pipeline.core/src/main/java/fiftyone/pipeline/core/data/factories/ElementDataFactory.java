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

package fiftyone.pipeline.core.data.factories;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.typed.TypedKey;

/**
 * Factory class used to create a new {@link ElementData} instance for a
 * {@link FlowElement}. An element will contain its own implementation of this
 * interface which builds the type of {@link ElementData} specific to that
 * element.
 * An implementation of (@link ElementDataFactory} is passed to the
 * {@link FlowData#getOrAdd(TypedKey, FlowElement.DataFactory)} method and is
 * called if the key does not already exist in the {@link FlowData}.
 * @param <T> the type of {@link ElementData} which should be built by the
 *           factory
 */
public interface ElementDataFactory<T extends ElementData> {

    /**
     * Create a new instance of {@link ElementData} for the {@link FlowElement}
     * provided, linked to the {@link FlowData} provided.
     * @param flowData to link the {@link ElementData} to
     * @param flowElement to create the {@link ElementData} to
     * @return a new {@link ElementData} instance to be added to the
     * {@link FlowData}
     */
    T create(FlowData flowData, FlowElement<T, ?> flowElement);
}
