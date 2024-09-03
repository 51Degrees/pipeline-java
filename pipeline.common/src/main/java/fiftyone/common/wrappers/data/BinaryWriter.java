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

package fiftyone.common.wrappers.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinaryWriter implements AutoCloseable {

    /**
     * Length of the byte buffer.
     */
    private static final int BUFFER_LENGTH = 2048;
    /**
     * Buffer used to correctly format the bytes for writing to disk.
     */
    private final ByteBuffer buffer;
    /**
     * Output stream for the final data file.
     */
    private WritableSeekable stream;

    private BinaryWriter() {
        buffer = ByteBuffer.allocate(BUFFER_LENGTH);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Construct a new instance of the binary writer ready to write a data set
     * to the file specified.
     *
     * @param outputStream where the data file should be written
     * @throws IOException if there was a problem writing to the output stream
     */
    public BinaryWriter(FileOutputStream outputStream) throws IOException {
        this();
        stream = new WritableSeekable.File(outputStream);
    }

    public BinaryWriter(ByteArrayOutputStream outputStream) {
        this();
        stream = new WritableSeekable.Memory(outputStream);
    }

    /**
     * Write a short to the output file.
     *
     * @param value short
     * @throws IOException if there was a problem writing to the output stream
     */
    public void writeInt16(short value) throws IOException {
        buffer.clear();
        buffer.putShort(value);
        writeBytes(buffer.array(), 2);
    }

    /**
     * Write an int to the output file.
     *
     * @param value int
     * @throws IOException if there was a problem writing to the output stream
     */
    public void writeInt32(int value) throws IOException {
        buffer.clear();
        buffer.putInt(value);
        writeBytes(buffer.array(), 4);
    }

    /**
     * Write a long to the output file.
     *
     * @param value long
     * @throws IOException if there was a problem writing to the output stream
     */
    public void writeInt64(long value) throws IOException {
        buffer.clear();
        buffer.putLong(value);
        writeBytes(buffer.array(), 8);
    }

    /**
     * Write a series of bytes to the output file.
     *
     * @param value bytes
     * @throws IOException if there was a problem writing to the output stream
     */
    public void writeBytes(byte[] value) throws IOException {
        writeBytes(value, value.length);
    }

    private void writeBytesBuffered(byte[] value, int length) throws IOException {
        int pos = 0;
        while (pos < length && pos < value.length) {
            int read = Math.min(buffer.limit(), length - pos);
            buffer.clear();
            buffer.put(value, pos, read);
            stream.write(buffer.array(), 0, read);
            pos += read;
        }
    }

    public void writeBytes(byte[] value, int length) throws IOException {
        buffer.clear();
        if (buffer.limit() < value.length) {
            writeBytesBuffered(value, length);
        } else {
            buffer.put(value, 0, length);
            stream.write(buffer.array(), 0, length);
        }
    }

    /**
     * Write a single byte to the output file.
     *
     * @param value byte
     * @throws IOException if there was a problem writing to the output stream
     */
    public void writeByte(byte value) throws IOException {
        buffer.clear();
        buffer.put(value);
        writeBytes(buffer.array(), 1);
    }

    @Override
    public void close() throws IOException {
        stream.flush();
        stream.close();
    }

    interface WritableSeekable extends Closeable, Flushable {
        void write(byte b[], int off, int len) throws IOException;

        class File implements WritableSeekable {

            private final FileOutputStream stream;

            public File(FileOutputStream stream) {
                this.stream = stream;
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                stream.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                stream.close();
            }

            @Override
            public void flush() throws IOException {
                stream.flush();
            }
        }

        class Memory implements WritableSeekable {

            private final ByteArrayOutputStream stream;

            public Memory(ByteArrayOutputStream stream) {
                this.stream = stream;
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                stream.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                stream.close();
            }

            @Override
            public void flush() throws IOException {
                stream.flush();
            }
        }
    }
}
