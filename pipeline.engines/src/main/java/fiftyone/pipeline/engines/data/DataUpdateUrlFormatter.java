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

package fiftyone.pipeline.engines.data;

/**
 * Interface defining a URL formatter which is used by the
 * {@link AspectEngineDataFile#getFormattedUrl()} method to get the data update
 * URL.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/data-updates.md#update-polling">Specification</a>
 */
public interface DataUpdateUrlFormatter {
    /**
     * Get the formatted URL.
     * @param dataFile file to get the update URL for
     * @return formatted URL
     */
    String getFormattedDataUpdateUrl(AspectEngineDataFile dataFile);
}
