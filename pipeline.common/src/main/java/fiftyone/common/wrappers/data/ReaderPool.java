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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ReaderPool implements Closeable {

    /**
     * Linked list of readers available for use.
     */
    private final ConcurrentLinkedQueue<BinaryReader> readers =
        new ConcurrentLinkedQueue<>();

    /**
     * A source of file readers to use to read data from the file.
     */
    private final Source source;

    /**
     * The number of readers that have been created. May not be the same as
     * the readers in the queue as some may be in use.
     */
    private final AtomicInteger readerCount = new AtomicInteger(0);

    public ReaderPool(Source source) {
        this.source = source;
    }

    /**
     * Returns a reader to the temp file for exclusive use. Release method must
     * be called to return the reader to the pool when finished.
     *
     * @return Reader open and ready to read from the temp file.
     * @throws IOException if there was a problem accessing data file.
     */
    public BinaryReader getReader() throws IOException {
        BinaryReader reader = readers.poll();

        if (reader == null) {
            // There are no readers available so create one
            // and ensure that the reader count is incremented
            // after doing so.
            readerCount.incrementAndGet();
            reader = new BinaryReader(source.createStream());
        }

        return reader;
    }

    /**
     * Returns the reader to the pool to be used by another process later.
     *
     * @param reader Reader open and ready to read from the temp file
     */
    public void release(BinaryReader reader) {
        readers.add(reader);
    }

    /**
     * The number of readers that have been created. May not be the same as
     * the readers in the queue as some may be in use.
     *
     * @return The number of readers that have been created.
     */
    public int getReadersCreated() {
        return readerCount.get();
    }

    /**
     * Returns The number of readers in the queue.
     *
     * @return The number of readers in the queue.
     */
    public int getReadersQueued() {
        return readers.size();
    }

    public void close() throws IOException {
        while (!readers.isEmpty()) {
            BinaryReader reader = readers.poll();
            if (reader != null) {
                reader.close();
            }
        }
    }
}
