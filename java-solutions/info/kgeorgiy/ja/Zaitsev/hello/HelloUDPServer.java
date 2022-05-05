package info.kgeorgiy.ja.Zaitsev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Implementation of {@link HelloServer} interface.
 *
 * @author Zaitsev Ilya
 */
public class HelloUDPServer implements HelloServer {

    private ExecutorService executorService;
    private DatagramSocket socket;

    @Override
    public void start(int port, int threads) {
        executorService = Executors.newFixedThreadPool(threads);
        try {
            socket = new DatagramSocket(port);
            final int bufferSize = socket.getReceiveBufferSize();
            for (int i = 0; i < threads; i++) {
                executorService.submit(() -> {
                    while (!socket.isClosed()) {
                        try {
                            DatagramPacket request = new DatagramPacket(new byte[bufferSize], bufferSize);
                            socket.receive(request);
                            String name = new String(request.getData(), request.getOffset(), request.getLength(), StandardCharsets.UTF_8);
                            byte[] responseBody = ("Hello, " + name).getBytes(StandardCharsets.UTF_8);
                            DatagramPacket response = new DatagramPacket(responseBody, responseBody.length, request.getSocketAddress());
                            socket.send(response);
                        } catch (IOException e) {
                            if (!socket.isClosed()) {
                                System.err.println("IOException: " + e.getMessage());
                            }
                        }
                    }
                });
            }
        } catch (SocketException e) {
            System.err.println("Couldn't create socket");
        }
    }

    @Override
    public void close() {
        socket.close();
        executorService.shutdownNow();
    }

    /**
     * Main function. Requires exactly 2 arguments. Parses arguments and calls {@link #start} method in {@link HelloUDPClient}.
     *
     * @param args 2 arguments: [port] [threads]
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Incorrect arguments. Usage: <port> <threads>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            int threads = Integer.parseInt(args[1]);
            new HelloUDPServer().start(port, threads);
        } catch (NumberFormatException ignored) {
            System.err.println("Couldn't parse arguments");
        }
    }
}
