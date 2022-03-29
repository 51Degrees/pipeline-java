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

