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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;

import java.util.Map;

/**
 * Engine that makes requests to the 51Degrees cloud service based on the
 * details passed at creation and the evidence in the FlowData instance. The
 * unprocessed JSON response is stored in the FlowData for other engines to make
 * use of.
 */
public interface CloudRequestEngine
    extends AspectEngine<CloudRequestData, AspectPropertyMetaData> {

    /**
     * A collection of the properties that the cloud service can populate in the
     * JSON response. Keyed on property name.
     * @return public properties
     */
    Map<String, AccessiblePropertyMetaData.ProductMetaData> getPublicProperties();
}
