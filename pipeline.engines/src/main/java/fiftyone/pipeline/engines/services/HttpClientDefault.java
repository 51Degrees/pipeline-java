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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpClientDefault implements HttpClient {


    @Override
    public HttpURLConnection connect(URL url) throws IOException {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        return (HttpURLConnection)url.openConnection();
    }

    public String postData(
        HttpURLConnection connection,
        Map<String, String> headers,
        byte[] data) throws IOException {
        String reply;
        connection.setRequestMethod("POST");
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                connection.setRequestProperty(e.getKey(), e.getValue());
            }
        }
        if (data == null) {
            connection.setDoOutput(false);
        } else {
            connection.setDoOutput(true);
            connection.getOutputStream().write(data);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader((
            connection.getResponseCode() / 100 == 2 ? 
                connection.getInputStream() : 
                connection.getErrorStream())));
        StringBuilder builder = new StringBuilder();
        String current;
        while ((current = reader.readLine()) != null) {
            builder.append(current);
        }
        reply = builder.toString();

        return reply;
    }

    @Override
    public String getResponseString(HttpURLConnection connection) throws IOException {
        return getResponseString(connection, null);
    }

    @Override
    public String getResponseString(
        HttpURLConnection connection,
        Map<String, String> headers) throws IOException {
            
        connection.setRequestMethod("GET");
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                connection.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        BufferedReader in = new BufferedReader(
            new InputStreamReader(connection.getResponseCode() / 100 == 2 ? 
                connection.getInputStream() : 
                connection.getErrorStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }
}
