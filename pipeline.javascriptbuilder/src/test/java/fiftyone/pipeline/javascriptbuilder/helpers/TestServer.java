/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.javascriptbuilder.helpers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;

public class TestServer {

    private final HttpServer httpServer;
    private final String page = "<!DOCTYPE>" +
            "<html>" +
            "  <head>" +
            "    <title>Test Page</title>" +
            "  </head>" +
            "  <body>" +
            "  </body>" +
            "</html>";

    private final HttpHandler handler = exchange -> {
        byte[] response = page.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    };

    /**
     * Bind to a port the operating system chooses. Choosing a free port first
     * and binding it afterwards leaves a gap in which something else, such as
     * the browser opening a connection, can take the port, and the bind then
     * fails with "Address already in use".
     */
    public TestServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
    }

    /**
     * @return the port the server is bound to
     */
    public int getPort() {
        return httpServer.getAddress().getPort();
    }

    public void start() {
        httpServer.createContext("/", handler);
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }
}
