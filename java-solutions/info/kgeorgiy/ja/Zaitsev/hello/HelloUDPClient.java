package info.kgeorgiy.ja.Zaitsev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link HelloClient} interface.
 *
 * @author Zaitsev Ilya
 */
public class HelloUDPClient implements HelloClient {
    private static final int TERMINATION_TIMEOUT = 120;
    private static final int SO_TIMEOUT = 200;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        SocketAddress address = new InetSocketAddress(host, port);
        for (int i = 0; i < threads; i++) {
            final int threadNumber = i;
            executorService.submit(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    final int bufferSize = socket.getReceiveBufferSize();
                    socket.setSoTimeout(SO_TIMEOUT);
                    for (int requestNumber = 0; requestNumber < requests; requestNumber++) {
                        while (true) {
                            String name = String.format("%s%d_%d", prefix, threadNumber, requestNumber);
                            DatagramPacket request = HelloUtil.compilePacket(name.getBytes(StandardCharsets.UTF_8), address);
                            try {
                                socket.send(request);
                            } catch (IOException e) {
                                continue;
                            }

                            DatagramPacket response = new DatagramPacket(new byte[bufferSize], bufferSize);
                            try {
                                socket.receive(response);
                            } catch (IOException e) {
                                continue;
                            }

                            String responseBody = HelloUtil.getDataAsString(response);
                            if (responseBody.contains(name)) {
                                System.out.println(responseBody);
                                break;
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.err.println("Couldn't create socket");
                }
            });
        }
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                System.err.println("Threads are not responding");
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for threads to finish:" + e.getMessage());
        }
    }

    /**
     * Main function. Requires exactly 5 arguments. Parses arguments and calls {@link #run} method in {@link HelloUDPClient}.
     *
     * @param args 5 arguments: [host] [port] [prefix] [threads] [requests]
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Incorrect arguments. Usage: <host> <port> <prefix> <threads> <requests>");
            return;
        }

        try {
            int port = Integer.parseInt(args[1]);
            int threads = Integer.parseInt(args[3]);
            int requests = Integer.parseInt(args[4]);
            new HelloUDPClient().run(args[0], port, args[1], threads, requests);
        } catch (NumberFormatException ignored) {
            System.err.println("Couldn't parse arguments");
        }
    }
}
