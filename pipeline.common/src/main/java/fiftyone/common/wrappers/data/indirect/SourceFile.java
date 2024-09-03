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

package fiftyone.common.wrappers.data.indirect;

import fiftyone.common.wrappers.data.Source;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SourceFile implements Source {
    private final FileChannel channel;

    public SourceFile(String fileName)
        throws IOException, FileNotFoundException {
        try(FileInputStream fis = new FileInputStream(fileName)){
             channel = fis.getChannel();
        }
        
    }

    /**
     * Creates a new ByteBuffer from the file located on the hard drive.
     *
     * @return ByteBuffer ready to read data from the data file on hard drive.
     * @throws IOException if there was a problem accessing data file.
     */
    public ByteBuffer createStream() throws IOException {
        MappedByteBuffer byteBuffer = channel.map(
            FileChannel.MapMode.READ_ONLY,
            0,
            channel.size());
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer;
    }
}