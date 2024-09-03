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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.DataBase;
import fiftyone.pipeline.core.data.Evidence;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Default implementation of the {@link Evidence} interface.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/evidence.md">Specification</a>
 */
class EvidenceDefault extends DataBase implements Evidence {

    /**
     * Construct a new instance of the internal {@link Evidence} class using a
     * case insensitive {@link ConcurrentSkipListMap} as the backing map.
     */
    EvidenceDefault(Logger logger) {
        super(logger, new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER));
    }
}
