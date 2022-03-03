package info.kgeorgiy.ja.Zaitsev.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Walk {
    private static final int BUFFER_SIZE = 1024;
    // :NOTE: EMPTY_SHA
    private static final byte[] EMPTY_HASH = new byte[20];
    // :NOTE: move to a const
    private static final MessageDigest digest = initDigest();

    private static MessageDigest initDigest() {
        try {
            return MessageDigest.getInstance("Sha-1");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Failed to initialise MessageDigest:" + e.getMessage());
            return null;
        }
    }

    private static byte[] calculateSHA(String file) {
        if (digest == null) {
            return EMPTY_HASH;
        }
        try (InputStream in = Files.newInputStream(Paths.get(file))) {
            byte[] bytes = new byte[BUFFER_SIZE];

            int readSize;
            while ((readSize = in.read(bytes)) >= 0) {
                digest.update(bytes, 0, readSize);
            }
            return digest.digest();
        } catch (IOException | InvalidPathException e) {
            // :NOTE: logs
            System.err.println("Failed to read file while calculating hash:" + e.getMessage());
            return EMPTY_HASH;
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Incorrect arguments. Usage: <inputPath> <outputPath>");
            return;
        }

        Path in;
        try {
            in = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            System.err.println("Invalid input file path:" + e.getMessage());
            return;
        }

        Path out;
        try {
            out = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.err.println("Invalid output file path: " + e.getMessage());
            return;
        }
        try {
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
        } catch (IOException e) {
            System.err.println("Can't create directories for output");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                String curFile;
                while ((curFile = reader.readLine()) != null) {
                    byte[] hash = calculateSHA(curFile);
                    writer.write(String.format("%0" + (hash.length * 2) + "x %s%n", new BigInteger(1, hash), curFile));
                }
            } catch (IOException e) {
                System.err.println("I/O exception while writing to output file: " + e.getMessage());
            }
            // :NOTE: do not merge exception
        } catch (IOException e) {
            System.err.println("I/O exception while reading input file: " + e.getMessage());
            // :NOTE: securityException
        }
    }
}
