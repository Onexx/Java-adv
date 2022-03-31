package info.kgeorgiy.ja.Zaitsev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


/**
 * Implementation of {@link JarImpler} interface.
 * <p>
 * Provides methods for generating {@code .java} and {@code .jar} files implementing the provided interface.
 *
 * @author Zaitsev Ilya
 */
public class Implementor implements JarImpler {
    /**
     * Main class for implementor.
     * Runs in two modes:
     * <ul>
     *     <li>
     *         2 arguments - <strong>className</strong> and <strong>outputPath</strong>
     *         - creates {@code .java} file by invoking {@link #implement(Class, Path)}.
     *     </li>
     *
     *     <li>
     *         3 arguments - <strong>-jar</strong>, <strong>className</strong> and <strong>jarPath</strong>
     *         - creates {@code .jar} file by invoking {@link #implementJar(Class, Path)}.
     *     </li>
     * </ul>
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.err.println("Incorrect arguments. Usage: <className> <outputPath> or -jar <className> <jarPath>");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.err.println("Incorrect arguments. Usage: <className> <outputPath> or -jar <className> <jarPath>");
                return;
            }
        }
        if (args.length == 3 && !args[0].equals("-jar")) {
            System.err.println("When calling a method with three arguments, the first argument must be equal to -jar");
            return;
        }
        final Implementor implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Couldn't find class: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("An error occurred during implementation: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid output file path: " + e.getMessage());
        }
    }


    /**
     * Converts all unicode characters in {@code text} to unicode escape notation.
     *
     * @param text Text for conversion.
     * @return string with characters in unicode escape notation.
     */
    private static String toUnicode(String text) {
        StringBuilder b = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c < 128) {
                b.append(c);
            } else {
                b.append("\\u").append(String.format("%04X", (int) c));
            }
        }
        return b.toString();
    }

    /**
     * Produces code implementing interface specified by provided {@code token}.
     * <p>
     * The name of the generated class is the same as the class name of the {@code token} type
     * with the {@code Impl} suffix added. Generated source code will be placed in the subdirectory of the specified
     * {@code root} directory.
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("Incorrect class token, interface expected");
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't implement class from private interface");
        }

        final Path packagePath = root.resolve(token.getPackageName().replace('.', File.separatorChar));
        final Path filePath = packagePath.resolve(token.getSimpleName() + "Impl.java");

        try {
            Files.createDirectories(packagePath);
        } catch (IOException e) {
            throw new ImplerException("Can't create package directories");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            if (!token.getPackageName().isEmpty()) {
                writer.write(String.format("package %s;%n%n", token.getPackageName()));
            }
            writer.write(toUnicode(String.format("public class %sImpl implements %s {%n", token.getSimpleName(), token.getCanonicalName())));

            for (final Method method : token.getMethods()) {
                writer.write(String.format("\tpublic %s %s(%s) {%n\t\treturn%s;%n\t}%n%n",
                        method.getReturnType().getCanonicalName(),
                        method.getName(),
                        getArguments(method),
                        getDefaultValue(method.getReturnType())
                ));
            }
            writer.write(String.format("}%n"));

        } catch (IOException e) {
            throw new ImplerException("Couldn't write to output file");
        }
    }

    /**
     * Returns arguments of provided method.
     *
     * @param method method whose arguments need to be returned.
     * @return string containing all arguments of provided method.
     */
    private static String getArguments(Method method) {
        return Arrays.stream(method.getParameters())
                .map(parameter -> (parameter.getType().getCanonicalName() + ' ' + parameter.getName()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns string representation of a default value for provided return type.
     *
     * @param methodReturnType return type of a method.
     * @return null for all non-primitive classes and default value otherwise.
     */
    private String getDefaultValue(Class<?> methodReturnType) {
        if (!methodReturnType.isPrimitive()) {
            return " null";
        } else if (methodReturnType.equals(boolean.class)) {
            return " false";
        } else if (methodReturnType.equals(void.class)) {
            return "";
        }
        return " 0";
    }

    /**
     * Compiles class that implements {@code token} class
     *
     * @param token     Type token of class for compilation.
     * @param dir       Directory for compiled file.
     * @param className Class name in it's string form.
     * @throws ImplerException If an exception occurred during compilation.
     */
    public static void compile(Class<?> token, Path dir, String className) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String classPath;
        try {
            classPath = dir.getFileName() + File.pathSeparator +
                    Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("Failed to get classPath");
        }
        final String filePath = dir.resolve(className).toString() + ".java";
        final String[] args = {"-cp", classPath, filePath};
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Failed to compile file");
        }
    }

    /**
     * Produces {@code .jar} file implementing interface specified by provided {@code token}.
     * <p>
     * The name of the generated class is the same as the class name of the {@code token} type
     * with the {@code Impl} suffix added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        final Path tmpDir = Paths.get(".");
        implement(token, tmpDir);

        final String classFilename = Paths.get(token.getPackageName().replace('.', File.separatorChar))
                .resolve(token.getSimpleName() + "Impl").toString();

        compile(token, tmpDir, classFilename);

        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile))) {
            out.putNextEntry(new ZipEntry(classFilename.replace(File.separatorChar, '/') + ".class"));
            Files.copy(Paths.get(tmpDir.resolve(classFilename) + ".class"), out);
        } catch (IOException e) {
            throw new ImplerException("Couldn't write to jar file");
        }
    }
}
