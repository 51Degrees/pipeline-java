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

    public TestServer(int port) throws IOException {
        InetSocketAddress address = new InetSocketAddress(port);
        httpServer = HttpServer.create(address, 0);
    }

    public void start() {
        httpServer.createContext("/", handler);
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(0);
    }
}
