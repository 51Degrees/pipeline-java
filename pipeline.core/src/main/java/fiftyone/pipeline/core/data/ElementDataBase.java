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

package fiftyone.pipeline.core.data;

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import org.slf4j.Logger;

import java.util.Map;
import java.util.TreeMap;

/**
 * Data created by a FlowElement within the FlowData. Properties can be
 * fetched using their name as a key.
 *
 * @see FlowElement
 */
public abstract class ElementDataBase extends DataBase implements ElementData {

    private Pipeline pipeline;

    public ElementDataBase(Logger logger, FlowData flowData) {
        this(logger, flowData, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    public ElementDataBase(Logger logger, FlowData flowData, Map<String, Object> data) {
        super(logger, data);
        pipeline = flowData.getPipeline();
    }

    @Override
    public Pipeline getPipline() {
        return pipeline;
    }

    @Override
    public void setPipline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }
}
