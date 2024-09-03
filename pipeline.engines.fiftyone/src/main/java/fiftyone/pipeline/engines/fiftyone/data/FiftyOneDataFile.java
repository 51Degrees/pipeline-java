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

package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.engines.data.AspectEngineDataFile;

/**
 * 51Degrees specific data file.
 */
public interface FiftyOneDataFile extends AspectEngineDataFile {

    /**
     * Get the type name to send when checking for data file updates e.g.
     * HashV41.
     * In general, this value should be pulled from the Engine, which will have
     * read it from the data file. However, in some cases, we want to know the
     * type name before the engine is created. (e.g. when UpdateOnStartup is
     * set) This is why the value can also be set.
     * @return download type
     */
    String getDataUpdateDownloadType();

    /**
     * Set the data download type e.g. HashV41.
     * @param type download type
     */
    void setDataUpdateDownloadType(String type);
}
