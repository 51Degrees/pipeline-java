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

package fiftyone.pipeline.engines.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class DataUploaderHttp implements DataUploader{

    private final String url;
    private final Map<String, String> headers;
    private final int timeout;
    HttpURLConnection connection;

    /**
     * the URL to which data is to be uploaded
     * @param url the URL rto which data should be uploaded
     * @param headers HTTP headers for the connection
     * @param timeout connection timeout in milliseconds
     */
    public DataUploaderHttp(String url, Map<String, String> headers, int timeout) {
        this.url = url;
        this.headers = headers;
        this.timeout = timeout;
    }

    /**
     * Returns a gzipped output stream
     */
    @Override
    public OutputStream getOutputStream() throws Exception{
        connection = (HttpURLConnection) new URL(url.trim()).openConnection();
        connection.setConnectTimeout(timeout);
        connection.setRequestMethod("POST");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        connection.setDoOutput(true);
        return new GZIPOutputStream(connection.getOutputStream());
    }

    @Override
    public int getResponseCode() throws IOException {
        return connection.getResponseCode();
    }
}

