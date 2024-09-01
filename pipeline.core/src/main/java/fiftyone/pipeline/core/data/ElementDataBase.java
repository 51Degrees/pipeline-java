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

package fiftyone.pipeline.core.data;

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import org.slf4j.Logger;

import java.util.Map;
import java.util.TreeMap;

/**
 * Base implementation of {@link ElementData}.
 * Represents property values that have be determined by a specific
 * {@link FlowElement} based on the supplied evidence.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#element-data">Specification</a>
 */
public abstract class ElementDataBase extends DataBase implements ElementData {

    private Pipeline pipeline;

    /**
     * Constructs a new instance with a non-thread-safe, case-insensitive
     * {@link Map} as the underlying storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     */
    public ElementDataBase(Logger logger, FlowData flowData) {
        this(logger, flowData, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Constructs a new instance with a custom {@link Map} as the underlying
     * storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param data the custom {@link Map} implementation to use as the
     *             underlying storage
     */
    public ElementDataBase(
        Logger logger,
        FlowData flowData,
        Map<String, Object> data) {
        super(logger, data);
        pipeline = flowData.getPipeline();
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }
}
