package fiftyone.pipeline.engines.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * provides an OutputStream to share usage
 */
public interface DataUploader {
    /**
     * returns an OutputStream through which data may be uploaded
     */
    OutputStream getOutputStream() throws Exception;

    /**
     *
     * @return 200 for success
     */
    int getResponseCode() throws IOException;


}
