package info.kgeorgiy.ja.Zaitsev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Implementor implements Impler {

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Incorrect arguments. Usage: <className> <outputPath>");
            return;
        }
        final Implementor implementor = new Implementor();
        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ClassNotFoundException e) {
            System.err.println("Couldn't find class [" + args[0] + "]: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("An error occurred during implementation: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid output file path: " + e.getMessage());
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("Incorrect class token, interface expected");
        }
        if(Modifier.isPrivate(token.getModifiers())){
            throw new ImplerException("Can't implement class from private interface");
        }

        Path packagePath = root.resolve(token.getPackageName().replace('.', File.separatorChar));
        Path filePath = packagePath.resolve(token.getSimpleName() + "Impl.java");

        try {
            Files.createDirectories(packagePath);
        } catch (IOException e) {
            throw new ImplerException("Can't create package directories");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {

            if (!token.getPackageName().isEmpty()) {
                writer.write(String.format("package %s;%n%n", token.getPackageName()));
            }
            writer.write(String.format("public class %sImpl implements %s {%n", token.getSimpleName(), token.getCanonicalName()));

            for (final Method method : token.getMethods()) {
                writer.write(String.format("\tpublic %s %s(%s) {%n\t\treturn%s;%n\t}%n%n",
                        method.getReturnType().getCanonicalName(),
                        method.getName(),
                        arguments(method),
                        defaultValue(method.getReturnType())
                ));
            }
            writer.write(String.format("}%n"));

        } catch (IOException e) {
            throw new ImplerException("Couldn't write to output file");
        }
    }

    private static String arguments(Method method) {
        return Arrays.stream(method.getParameters())
                .map(parameter -> (parameter.getType().getCanonicalName() + ' ' + parameter.getName()))
                .collect(Collectors.joining(", "));
    }

    private String defaultValue(Class<?> methodReturnType) {
        if (!methodReturnType.isPrimitive()) {
            return " null";
        } else if (methodReturnType.equals(boolean.class)) {
            return " false";
        } else if (methodReturnType.equals(void.class)) {
            return "";
        }
        return " 0";
    }
}
