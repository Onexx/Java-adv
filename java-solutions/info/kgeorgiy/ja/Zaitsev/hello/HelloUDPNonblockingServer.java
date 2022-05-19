package info.kgeorgiy.ja.Zaitsev.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of {@link HelloServer} interface.
 *
 * @author Zaitsev Ilya
 */
public class HelloUDPNonblockingServer implements HelloServer {

    private static final int BUFFER_SIZE = 100;
    private static final int TIMEOUT = 200;


    private ExecutorService executorService;
    private DatagramChannel channel;
    private Selector selector;

    private static class Context {
        SocketAddress address;
        ByteBuffer buffer;
    }

    @Override
    public void start(int port, int threads) {
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ, new Context());
            executorService = Executors.newSingleThreadExecutor();
        } catch (IOException e) {
            System.err.println("Failed to open selector and channel: " + e.getMessage());
            return;
        }

        executorService.submit(() -> {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                while (!Thread.interrupted()) {
                    selector.select(TIMEOUT);

                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();

                        if (key.isReadable()) {
                            final DatagramChannel serverChannel = (DatagramChannel) key.channel();
                            buffer.clear();
                            Context context = (Context) key.attachment();
                            try {
                                context.address = serverChannel.receive(buffer);

                                if (context.address != null) {

                                    byte[] responseBytes = HelloUtil.formResponse(HelloUtil.stringFromBuffer(buffer));
                                    context.buffer = ByteBuffer.allocate(responseBytes.length);
                                    context.buffer.put(responseBytes);

                                    key.interestOps(SelectionKey.OP_WRITE);
                                }
                            } catch (IOException e) {
                                System.err.println("Failed to get data: " + e.getMessage());
                            }
                        }

                        if (key.isWritable()) {
                            final DatagramChannel serverChannel = (DatagramChannel) key.channel();
                            Context context = (Context) key.attachment();

                            context.buffer.flip();
                            try {
                                serverChannel.send(context.buffer, context.address);

                                key.interestOps(SelectionKey.OP_READ);
                            } catch (IOException e) {
                                System.err.println("Failed to send data: " + e.getMessage());
                            }
                        }

                        i.remove();
                    }
                }
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        });

    }

    @Override
    public void close() {
        HelloUtil.shutdown(executorService);
        try {
            channel.close();
            selector.close();
        } catch (IOException e) {
            System.err.println("Couldn't close selector and channel: " + e.getMessage());
        }

    }

    /**
     * Main function. Requires exactly 2 arguments. Parses arguments and calls {@link #start} method in {@link HelloUDPNonblockingServer}.
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
            new HelloUDPNonblockingServer().start(port, threads);
        } catch (NumberFormatException ignored) {
            System.err.println("Couldn't parse arguments");
        }
    }
}
