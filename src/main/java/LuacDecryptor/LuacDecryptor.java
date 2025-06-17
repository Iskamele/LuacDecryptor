package LuacDecryptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuacDecryptor {
    public static void main(String[] args) {
        String filename = "file1.luac";

        try {
            InputStream inputStream = LuacDecryptor.class.getClassLoader().getResourceAsStream(filename);
            byte[] data = inputStream.readAllBytes();
            inputStream.close();

            System.out.println("file size: " + data.length + " bytes");

            System.out.println("\n=== search for readable lines ===");
            findReadableStrings(data);

            System.out.println("\n=== analysis of structure ===");
            analyzeFileStructure(data);

            System.out.println("\n=== combined decoding ===");
            tryHybridDecoding(data, filename);

        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    private static void findReadableStrings(byte[] data) {
        StringBuilder sb = new StringBuilder();
        List<String> strings = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];

            // ASCII
            if ((b >= 32 && b <= 126) || b == 0) {
                if (b == 0) {
                    if (sb.length() > 3) {
                        strings.add("position " + (i - sb.length()) + ": \"" + sb + "\"");
                    }
                    sb = new StringBuilder();
                } else {
                    sb.append((char) b);
                }
            } else {
                if (sb.length() > 3) {
                    strings.add("position " + (i - sb.length()) + ": \"" + sb + "\"");
                }
                sb = new StringBuilder();
            }
        }

        strings.forEach(System.out::println);
    }

    private static void analyzeFileStructure(byte[] data) {
        Map<String, Integer> patterns = new HashMap<>();

        for (int i = 0; i < data.length - 3; i++) {
            String pattern = String.format("%02x%02x%02x%02x",
                    data[i] & 0xFF, data[i + 1] & 0xFF, data[i + 2] & 0xFF, data[i + 3] & 0xFF);
            patterns.put(pattern, patterns.getOrDefault(pattern, 0) + 1);
        }

        System.out.println("common 4-byte patterns:");
        patterns.entrySet().stream()
                .filter(e -> e.getValue() > 2)
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue() + " times"));
    }

    private static void tryHybridDecoding(byte[] data, String filename) {
        // после префикса до середины - XOR 0x20
        // середина до конца - другой алгоритм

        int midPoint = data.length / 2;

        byte[] result = new byte[data.length];
        System.arraycopy(data, 0, result, 0, 5); // префикс

        // перчая часть XOR 0x20
        for (int i = 5; i < midPoint; i++) {
            result[i] = (byte) ((data[i] & 0xFF) ^ 0x20);
        }

        // вторая без изменений/новый алгрм
        System.arraycopy(data, midPoint, result, midPoint, data.length - midPoint);

        System.out.println("hybrid decoding (first half XOR):");

        StringBuilder sb = new StringBuilder();
        for (int i = 5; i < Math.min(200, result.length); i++) {
            byte b = result[i];
            if (b >= 32 && b <= 126) {
                sb.append((char) b);
            } else {
                sb.append('.');
            }
        }
        System.out.println("first 200 symbols: " + sb);

        String resultStr = new String(result, 5, result.length - 5).toLowerCase();
        if (resultStr.contains("function") || resultStr.contains("local") ||
                resultStr.contains("end") || resultStr.contains("return")) {
            System.out.println("*** LUA KEYWORDS FOUND! ***");

            try {
                String outputFile = filename.replace(".luac", "_hybrid.lua");
                Files.write(Paths.get(outputFile), result);
                System.out.println("result saved: " + outputFile);
            } catch (IOException e) {
                System.out.println("saving error: " + e.getMessage());
            }
        }

        // ROT
        tryRotDecoding(data, filename);
    }

    private static void tryRotDecoding(byte[] data, String filename) {
        System.out.println("\n=== ROT decoding ===");

        // ROT13 after prefix
        byte[] rotResult = new byte[data.length];
        System.arraycopy(data, 0, rotResult, 0, 5);

        for (int i = 5; i < data.length; i++) {
            byte b = data[i];
            if (b >= 65 && b <= 90) { // A-Z
                rotResult[i] = (byte) (((b - 65 + 13) % 26) + 65);
            } else if (b >= 97 && b <= 122) { // a-z
                rotResult[i] = (byte) (((b - 97 + 13) % 26) + 97);
            } else {
                rotResult[i] = b;
            }
        }

        String rotStr = new String(rotResult, 5, Math.min(200, rotResult.length - 5));
        System.out.println("ROT13 result (first 200 characters): " +
                rotStr.replaceAll("[\\x00-\\x1F\\x7F-\\xFF]", "."));

        if (rotStr.toLowerCase().contains("function") || rotStr.toLowerCase().contains("local")) {
            System.out.println("*** ROT13 gave LUA code! ***");

            try {
                String outputFile = filename.replace(".luac", "_rot13.lua");
                Files.write(Paths.get(outputFile), rotResult);
                System.out.println("saved ROT13 result: " + outputFile);
            } catch (IOException e) {
                System.out.println("saving error: " + e.getMessage());
            }
        }
    }
}
