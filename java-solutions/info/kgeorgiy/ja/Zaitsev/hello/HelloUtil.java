package info.kgeorgiy.ja.Zaitsev.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class HelloUtil {
    private static final int TIMEOUT = 5;

    static String getDataAsString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    static DatagramPacket compilePacket(byte[] responseBytes, SocketAddress address) {
        return new DatagramPacket(responseBytes, responseBytes.length, address);
    }

    static byte[] formResponse(String name) {
        return ("Hello, " + name).getBytes(StandardCharsets.UTF_8);
    }

    static void shutdown(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    System.err.println("Couldn't shutdown executorService");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static String stringFromBuffer(ByteBuffer buffer) {
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }
}
