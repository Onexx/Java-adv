package info.kgeorgiy.ja.Zaitsev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Implementation of {@link HelloClient} interface.
 *
 * @author Zaitsev Ilya
 */
public class HelloUDPNonblockingClient implements HelloClient {

    private static final int BUFFER_SIZE = 100;
    private static final int TIMEOUT = 200;

    private static class Context {
        ByteBuffer buffer;
        int threadNumber;
        int requestNumber;
        int maxRequests;

        public Context(int threadNumber, int maxRequests) {
            this.threadNumber = threadNumber;
            this.maxRequests = maxRequests;
            requestNumber = 0;
        }

        String getName(String prefix) {
            return String.format("%s%d_%d", prefix, threadNumber, requestNumber);
        }

        boolean sentAll() {
            return maxRequests - requestNumber == 0;
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {

        SocketAddress address = new InetSocketAddress(host, port);

        try {
            Selector selector = Selector.open();

            for (int i = 0; i < threads; i++) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.connect(address);
                channel.register(selector, SelectionKey.OP_WRITE, new Context(i, requests));
            }

            int unfinishedThreads = threads;
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            while (!Thread.interrupted() && unfinishedThreads > 0) {
                selector.select(TIMEOUT);
                if (selector.selectedKeys().isEmpty()) {
                    for (final SelectionKey key : selector.keys()) {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                } else {
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();
                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isReadable()) {
                            final DatagramChannel serverChannel = (DatagramChannel) key.channel();
                            buffer.clear();
                            Context context = (Context) key.attachment();
                            try {

                                SocketAddress socketAddress = serverChannel.receive(buffer);

                                if (socketAddress != null) {

                                    String response = HelloUtil.stringFromBuffer(buffer);
                                    if (response.contains(context.getName(prefix))) {
                                        System.out.println(response);
                                        context.requestNumber++;
                                        if (context.sentAll()) {
                                            unfinishedThreads--;
                                            serverChannel.close();
                                            key.cancel();
                                        }
                                    }

                                }
                            } catch (IOException e) {
                                System.err.println("Failed to get data: " + e.getMessage());
                            }
                            if (key.isValid()) {
                                key.interestOps(SelectionKey.OP_WRITE);
                            }
                        }

                        if (key.isValid() && key.isWritable()) {
                            final DatagramChannel serverChannel = (DatagramChannel) key.channel();
                            Context context = (Context) key.attachment();
                            byte[] nameBytes = context.getName(prefix).getBytes(StandardCharsets.UTF_8);
                            context.buffer = ByteBuffer.allocate(nameBytes.length);
                            context.buffer.put(nameBytes);
                            context.buffer.flip();
                            try {
                                int sent = serverChannel.send(context.buffer, address);
                                if (sent == nameBytes.length) {
                                    key.interestOps(SelectionKey.OP_READ);
                                }
                            } catch (IOException e) {
                                System.err.println("Failed to send data: " + e.getMessage());
                            }
                        }

                        i.remove();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    /**
     * Main function. Requires exactly 5 arguments. Parses arguments and calls {@link #run} method in {@link HelloUDPNonblockingClient}.
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
            new HelloUDPNonblockingClient().run(args[0], port, args[1], threads, requests);
        } catch (NumberFormatException ignored) {
            System.err.println("Couldn't parse arguments");
        }
    }
}
