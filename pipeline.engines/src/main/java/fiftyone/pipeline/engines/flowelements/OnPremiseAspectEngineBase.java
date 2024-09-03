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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.util.Types;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Base class for on-premise aspect engines.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#on-premise-engines">Specification</a>
 * @param <TData> the type of aspect data that the flow element will write to
 * @param <TProperty> the type of meta data that the flow element will supply
 *                    about the properties it populates.
 */
public abstract class OnPremiseAspectEngineBase<
    TData extends AspectData,
    TProperty extends AspectPropertyMetaData>
    extends AspectEngineBase<TData, TProperty>
    implements OnPremiseAspectEngine<TData, TProperty> {

    private final List<AspectEngineDataFile> dataFiles;

    private String tempDataDirPath;

    /**
     * Construct a new instance of the {@link OnPremiseAspectEngine}.
     * @param logger logger instance to use for logging
     * @param aspectDataFactory the factory to use when creating a TData
     *                          instance
     * @param tempDataDirPath the directory where a temporary data file copy
     *                        will be stored if one is created
     */
    public OnPremiseAspectEngineBase(
        Logger logger,
        ElementDataFactory<TData> aspectDataFactory,
        String tempDataDirPath) {
        super(logger, aspectDataFactory);
        this.dataFiles = new ArrayList<>();
        setTempDataDirPath(tempDataDirPath);
    }

    @Override
    public List<AspectEngineDataFile> getDataFiles() {
        return Collections.unmodifiableList(dataFiles);
    }

    @Override
    public String getTempDataDirPath() {
        return tempDataDirPath;
    }

    protected void setTempDataDirPath(String tempDataDirPath) {
        this.tempDataDirPath = tempDataDirPath;
    }

    @Override
    public AspectEngineDataFile getDataFileMetaData(String dataFileIdentifier) {
        if(dataFiles.size() == 0) {
            return null;
        }
        else if (dataFiles.size() == 1) {
            return dataFiles.get(0);
        }
        else {
            for (AspectEngineDataFile dataFile : dataFiles) {
                if (dataFile.getIdentifier().equals(dataFileIdentifier)) {
                    return dataFile;
                }
            }
            return null;
        }
    }

    @Override
    public AspectEngineDataFile getDataFileMetaData() {
        return getDataFileMetaData(null);
    }

    /**
     * Get the date/time that the specified data file was published
     * @param dataFileIdentifier the identifier of the data file to get meta
     *                           data for. This parameter is ignored if the
     *                           engine only has one data files
     * @return data the data file was published
     */
    public abstract Date getDataFilePublishedDate(String dataFileIdentifier);

    /**
     * Get the date/time that the default data file was published. This takes no
     * identifier and is designed for engines which only use a single data file.
     * @return data the data file was published
     */
    public Date getDataFilePublishedDate() {
        return getDataFilePublishedDate(null);
    }

    /**
     * Get the date/time that an update is expected to be available
     * for the specified data file.
     * @param dataFileIdentifier the identifier of the data file to get meta
     *                           data for. This parameter is ignored if the
     *                           engine only has one data file
     * @return date the next data file update is available
     */
    public abstract Date getDataFileUpdateAvailableTime(String dataFileIdentifier);

    /**
     * Get the date/time that an update is expected to be available for the
     * default data file. This takes no identifier and is designed for engines
     * which only use a single data file.
     * @return date the next data file update is available
     */
    public Date getDataFileUpdateAvailableTime() {
        return getDataFileUpdateAvailableTime(null);
    }

    @Override
    public abstract void refreshData(String dataFileIdentifier);

    @Override
    public abstract void refreshData(String dataFileIdentifier, byte[] data);

    @Override
    protected void managedResourcesCleanup() {
        for (AspectEngineDataFile dataFile : dataFiles) {
            try {
                dataFile.close();
            } catch (IOException e) {
                // Do nothing.
            }
        }
        super.managedResourcesCleanup();
    }

    @Override
    public void addDataFile(AspectEngineDataFile dataFile) {
        dataFiles.add(dataFile);

        if (dataFile.getDataFilePath() != null &&
            dataFile.getDataFilePath().isEmpty() == false) {
            refreshData(dataFile.getIdentifier());
        }
        else if (dataFile.getConfiguration().getData() != null) {
            refreshData(
                dataFile.getIdentifier(),
                dataFile.getConfiguration().getData());
        }
        else {
            throw new PipelineConfigurationException(
                "This engine requires the configured data file " +
                    "to have either the 'Data' property or 'DataFilePath' " +
                    "property populated but it has neither.");
        }
        dataFile.setEngine(this);

    }

    @Override
    public TypedKey<TData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(
                getElementDataKey(),
                Types.findSubClassParameterType(this, OnPremiseAspectEngineBase.class, 0));
        }
        return typedKey;
    }

}
