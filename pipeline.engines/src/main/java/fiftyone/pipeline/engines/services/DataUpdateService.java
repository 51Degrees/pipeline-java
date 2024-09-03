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
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;

import java.io.Closeable;

/**
 * The data update service keeps {@link AspectEngine}’s data files up to date.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/data-updates.md">Specification</a>
 */
public interface DataUpdateService extends Closeable, PipelineService {

    /**
     * Check for an available download of a new data file, or a local data file
     * newer than the one currently in use. If either is the case, then the
     * engine is updated with the new data.
     * The engine does not need to be registered with the service for this
     * method to be used.
     *
     * @param engine {@link AspectEngine} to try updating
     * @param dataFileIdentifier the identifier of the data file to check for.
     *                           If the engine has only one data file then this
     *                           parameter is ignored
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_SUCCESS} if the data file was
     * successfully updated
     * 
     */
    AutoUpdateStatus checkForUpdate(OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine, String dataFileIdentifier);

    /**
     * Check for an available download of a new data file, or a local data file
     * newer than the one currently in use. If either is the case, then the
     * engine is updated with the new data.
     * The engine does not need to be registered with the service for this
     * method to be used.
     *
     * @param engine {@link AspectEngine} to try updating
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_SUCCESS} if the data file was
     * successfully updated
     */
    AutoUpdateStatus checkForUpdate(OnPremiseAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine);

    /**
     * Update the Engine with the byte array provided. If the Engine is is using
     * data on disk, then the file is replaced with the new data.
     *
     * @param engine {@link AspectEngine} to try updating
     * @param data new data file in memory
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_SUCCESS} if the data file was
     * successfully updated
     */
    AutoUpdateStatus updateFromMemory(AspectEngineDataFile engine, byte[] data);

    /**
     * Add an event handler fired when a call to
     * {@link #checkForUpdate(OnPremiseAspectEngine)} is completed.
     * @param onUpdateComplete event handler to call
     */
    void onUpdateComplete(OnUpdateComplete onUpdateComplete);

    /**
     * Register an engine to be automatically kept up to data. Tasks will be
     * scheduled for downloading and updating when new data files are expected.
     * A regular task is also run to update an engine if the local data file
     * is newer than the one in use.
     * @param dataFile data file to register
     */
    void registerDataFile(AspectEngineDataFile dataFile);

    /**
     * Un-register a data file which was registered with the
     * {@link #registerDataFile(AspectEngineDataFile)} method.
     * @param dataFile data file to un-register
     */
    void unregisterDataFile(AspectEngineDataFile dataFile);

    /**
     * Status code indicating the result of an update.
     */
    enum AutoUpdateStatus {

        // Update completed successfully.
        AUTO_UPDATE_SUCCESS,
        // HTTPS connection could not be established.
        AUTO_UPDATE_HTTPS_ERR,
        // No need to perform update.
        AUTO_UPDATE_NOT_NEEDED,
        // Update currently under way.
        AUTO_UPDATE_IN_PROGRESS,
        // Path to master file is directory not file
        AUTO_UPDATE_MASTER_FILE_CANT_RENAME,
        // 51Degrees server responded with 429: too many attempts.
        AUTO_UPDATE_ERR_429_TOO_MANY_ATTEMPTS,
        // 51Degrees server responded with 403 meaning key is blacklisted.
        AUTO_UPDATE_ERR_403_FORBIDDEN,
        // Used when IO operations with input or output stream failed.
        AUTO_UPDATE_ERR_READING_STREAM,
        // MD5 validation failed
        AUTO_UPDATE_ERR_MD5_VALIDATION_FAILED,
        // The new data file can't be renamed to replace the previous one.
        AUTO_UPDATE_NEW_FILE_CANT_RENAME,
        // Refreshing the engine with the new data caused an error to occur.
        AUTO_UPDATE_REFRESH_FAILED
    }

    class DataUpdateCompleteArgs {
        private AspectEngineDataFile dataFile;
        private AutoUpdateStatus status;

        public DataUpdateCompleteArgs(
            AutoUpdateStatus status,
            AspectEngineDataFile dataFile) {
            this.status = status;
            this.dataFile = dataFile;
        }

        public AutoUpdateStatus getStatus() {
            return status;
        }

        public AspectEngineDataFile getDataFile() {
            return dataFile;
        }
    }

}
