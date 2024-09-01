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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Default implementation of {@link FileWrapper} using basic file operations.
 */
public class FileWrapperDefault implements FileWrapper {
    /**
     * The file on disk.
     */
    private final File file;

    /**
     * Construct a new instance of {@link FileWrapper} using an existing file
     * located at the path provided.
     *
     * @param path path to the data file
     */
    public FileWrapperDefault(String path) {
        file = new File(path);
    }

    @Override
    public BinaryWriter getWriter() throws IOException {
        if (Files.exists(file.toPath()) == false) {
            file.createNewFile();
        }
        return new BinaryWriter(new FileOutputStream(file));
    }

    @Override
    public BinaryReader getReader() throws IOException {
        return new BinaryReader(new FileInputStream(file));
    }

    @Override
    public long getLastModified() {
        return file.lastModified();
    }

    @Override
    public String getPath() {
        return file.getPath();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public void delete() {
        file.delete();
    }
}
