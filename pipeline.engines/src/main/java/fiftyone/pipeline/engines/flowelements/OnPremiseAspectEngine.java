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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;

import java.util.List;

/**
 * Aspect engine interface which processes data internally using a data file and
 * populates the results.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#on-premise-engines">Specification</a>
 * @param <TData> the type of aspect data that the flow element will write to
 * @param <TProperty> the type of meta data that the flow element will supply
 *                    about the properties it populates.
 */
public interface OnPremiseAspectEngine<
    TData extends AspectData,
    TProperty extends AspectPropertyMetaData>
    extends AspectEngine<TData, TProperty> {

    /**
     * Details of the data files used by this engine.
     *
     * @return a read only list of data files
     */
    List<AspectEngineDataFile> getDataFiles();

    /**
     * Causes the engine to reload data from the file at
     * {@link AspectEngineDataFile#getDataFilePath()} for the data file matching
     * the given identifier. Where the engine is built from a byte[], the
     * overload with the byte[] parameter should be called instead. This method
     * is thread-safe so parallel calls to {@link #process(FlowData)} will
     * resolve as normal.
     * @param dataFileIdentifier the identifier of the data file to update.
     *                           Must match the value in
     *                           {@link AspectEngineDataFile#getIdentifier()}.
     *                           If the engine only has a single data file, this
     *                           parameter is ignored
     */
    void refreshData(String dataFileIdentifier);

    /**
     * Causes the engine to reload data from the specified byte[].
     * Where the engine is built from a data file on disk, this will
     * also update the data file with the new data.
     * This method is thread-safe so parallel calls to 'Process' will
     * resolve as normal.
     * @param dataFileIdentifier the identifier of the data file to update. Must
     *                           match the value in
     *                           {@link AspectEngineDataFile#getIdentifier()}.
     *                           If the engine only has a single data file, this
     *                           parameter is ignored
     * @param data an in-memory representation of the new data file contents
     */
    void refreshData(String dataFileIdentifier, byte[] data);

    /**
     * The complete file path to the directory that is used by the
     * engine to store temporary copies of any data files that it uses.
     * @return temporary data directory
     */
    String getTempDataDirPath();

    /**
     * Get the details of a specific data file used by this engine.
     * @param dataFileIdentifier  the identifier of the data file to get meta
     *                            data for. This parameter is ignored if the
     *                            engine only has one data file
     * @return the meta data associated with the specified data file, or null if
     * the engine has no associated data files
     */
    AspectEngineDataFile getDataFileMetaData(String dataFileIdentifier);

    /**
     * Get the details the default data file used by this engine.
     * @return the meta data associated with the specified data file, or null if
     * the engine has no associated data files
     */
    AspectEngineDataFile getDataFileMetaData();

    /**
     * Add the specified data file to the engine.
     * @param dataFile the data file to add
     */
    void addDataFile(AspectEngineDataFile dataFile);
}
