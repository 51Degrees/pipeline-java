package fiftyone.pipeline.javascriptbuilder.helpers;

import java.io.IOException;
import java.net.ServerSocket;

public class TcpHelper {

    public static int getAvailablePort() {
        try {
            ServerSocket socket = new ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
            throw new RuntimeException("No free port found", e);
        }
    }
}
