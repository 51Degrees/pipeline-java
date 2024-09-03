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

package fiftyone.pipeline.engines.services;

import fiftyone.pipeline.core.services.PipelineService;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;

import java.util.List;

/**
 * Service used by {@link AspectEngine}s to report the reason for a property
 * not being present in the results.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/properties.md#missing-properties">Specification</a>
 */
public interface MissingPropertyService extends PipelineService {

    /**
     * Get the reason for the property not being found. If the property can be
     * found, then the behaviour is undefined.
     *
     * @param propertyName name of the {@link AspectPropertyMetaData}
     * @param engine where the property should be found
     * @return reason the property was not found
     */
    MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine);

    /**
     * Get the reason for the property not being found. If the property can be
     * found, then the behaviour is undefined.
     *
     * @param propertyName name of the {@link AspectPropertyMetaData}
     * @param engines where the property should be found
     * @return reason the property was not found
     */
    MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> engines);
}
