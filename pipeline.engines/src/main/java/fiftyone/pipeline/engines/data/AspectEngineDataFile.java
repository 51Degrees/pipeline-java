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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.DataUpdateService;

import java.io.Closeable;
import java.util.Date;

/**
 * Interface for the details of a data file used by an Aspect engine.
 * These properties are used by the <see cref="DataUpdateService"/>
 * to perform automatic updates if enabled.
 */
public interface AspectEngineDataFile extends Closeable {
    /**
     * Get an identifier for this data file. Each data file used by an engine
     * must have a different identifier.
     */
    String getIdentifier();

    /**
     * Set an identifier for this data file. Each data file used by an engine
     * must have a different identifier.
     */
    void setIdentifier(String identifier);

    /**
     * Get the engine this data file is used by
     */
    OnPremiseAspectEngine getEngine();
    /**
     * Set the engine this data file is used by
     */
    void setEngine(OnPremiseAspectEngine engine);

    /**
     * The complete file path to the location of the data file.
     * This value will be null if the file has been supplied from
     * a byte[] in memory.
     */
    String getDataFilePath();

    /**
     * The complete file path to the location of the temporary
     * copy of the data file that is currently being used by the
     * AspectEngine.
     * Engines often make a temporary copy of the data file in order
     * to allow the original to be updated.
     * This value will be null if the file is loaded entirely into memory.
     */
    String getTempDataFilePath();

    void setTempDataFilePath(String file);

    /**
     * The path to use when working with temporary files associated with this
     * data file.
     */
    String getTempDataDirPath();

    void setTempDataDirPath(String path);

    /**
     * True if automatic updates are enabled, false otherwise.
     */
    boolean getAutomaticUpdatesEnabled();

    /**
     * The date and time by which an update to the current data file is
     * expected to have been published.
     */
    Date getUpdateAvailableTime();

    void setUpdateAvailableTime(Date availableTime);

    /**
     * The date and time that the current data file was published.
     */
    Date getDataPublishedDateTime();

    void setDataPublishedDateTime(Date published);

    /**
     * Get the configuration that was provided for this data file.
     */
    DataFileConfiguration getConfiguration();

    /**
     * Set the configuration that was provided for this data file.
     */
    void setConfiguration(DataFileConfiguration configuration);

    /**
     * Returns true if this file has been registered with the data update
     * service. False if not.
     */
    boolean getIsRegistered();

    /**
     * Get the data update URL complete with any query string
     * parameters that are needed to retrieve the data.
     * By default, no query string parameters are added to the URL.
     */
    String getFormattedUrl();

    /**
     * Set the data update service that this data file is registered with.
     * @param dataUpdateService The data update service.
     */
    void setDataUpdateService(DataUpdateService dataUpdateService);
}