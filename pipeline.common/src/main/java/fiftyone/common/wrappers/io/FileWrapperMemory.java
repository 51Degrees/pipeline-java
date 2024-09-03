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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * In memory implementation of the {@link FileWrapperFactory}. This is only used
 * for tests, where it makes more sense to write things to memory rather than
 * writing to the actual file system.
 */
public class FileWrapperMemory implements FileWrapper {

    /**
     * Stream containing the data for the data file in memory.
     */
    protected ByteArrayOutputStream outputStream;

    /**
     * Construct a new empty {@link FileWrapper} instance.
     */
    public FileWrapperMemory() {
        outputStream = new ByteArrayOutputStream();
    }

    @Override
    public BinaryWriter getWriter() throws IOException {
        outputStream.reset();
        return new BinaryWriter(outputStream);
    }

    @Override
    public BinaryReader getReader() throws IOException {
        return new BinaryReader(ByteBuffer.wrap(outputStream.toByteArray()));
    }

    @Override
    public long getLastModified() {
        return Long.MIN_VALUE;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public boolean exists() {
        return outputStream.size() > 0;
    }

    @Override
    public void delete() {
        outputStream = new ByteArrayOutputStream();
    }
}
