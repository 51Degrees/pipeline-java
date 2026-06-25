/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.translation.data;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.FlowData;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Data object populated by
 * {@link fiftyone.pipeline.translation.flowelements.TranslationEngine}.
 */
public class TranslationData extends ElementDataBase implements ITranslationData {

    /**
     * Construct a new instance.
     * @param logger used for logging
     * @param flowData the {@link FlowData} the element data will be added to
     */
    public TranslationData(Logger logger, FlowData flowData) {
        super(logger, flowData);
    }

    /**
     * Construct a new instance using an existing map of values.
     * @param logger used for logging
     * @param flowData the {@link FlowData} the element data will be added to
     * @param data the values to populate the element data with
     */
    public TranslationData(
        Logger logger,
        FlowData flowData,
        Map<String, Object> data) {
        super(logger, flowData, data);
    }
}
