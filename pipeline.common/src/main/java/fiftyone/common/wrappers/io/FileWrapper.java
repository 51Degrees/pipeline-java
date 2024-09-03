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

package fiftyone.common.wrappers.io;

import fiftyone.common.wrappers.data.BinaryReader;
import fiftyone.common.wrappers.data.BinaryWriter;

import java.io.IOException;

/**
 * Interface for a data file. By default this is a
 * simple wrapper for basic file operations. But this allows the file system
 * to be removed from unit tests by implementing the interface.
 */
public interface FileWrapper {

    /**
     * Get a {@link BinaryWriter} for this file, positioned at the first byte.
     * The file must already exist.
     *
     * @return new {@link BinaryWriter}
     * @throws IOException if the file is not found
     */
    BinaryWriter getWriter() throws IOException;

    /**
     * Get a {@link BinaryReader} for this file, positioned at the first byte.
     * The file must exist.
     *
     * @return new {@link BinaryReader}
     * @throws IOException if the file is not found
     */
    BinaryReader getReader() throws IOException;

    /**
     * Get the time the file was last modified.
     *
     * @return last modified date
     */
    long getLastModified();

    /**
     * Get the path to the data file.
     *
     * @return file path
     */
    String getPath();

    boolean exists();

    void delete();
}
