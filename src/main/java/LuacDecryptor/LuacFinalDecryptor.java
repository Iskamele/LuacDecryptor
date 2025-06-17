package LuacDecryptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LuacFinalDecryptor {
    public static void main(String[] args) {
        try {
            byte[] hybridData = Files.readAllBytes(Paths.get("File1_hybrid.lua"));
            System.out.println("hybrid file size: " + hybridData.length);

            System.out.println("\n=== hybrid file analysis ===");

            int readableStart = -1;
            for (int i = 1400; i < hybridData.length - 20; i++) {
                String chunk = new String(hybridData, i, 20);
                if (chunk.contains("play_impact") || chunk.contains("damage") || chunk.contains("managers")) {
                    readableStart = i;
                    break;
                }
            }

            System.out.println("readable code starts at position: " + readableStart);

            if (readableStart > 0) {
                System.out.println("\n=== try different algorithms first ===");

                byte[] bestResult = new byte[hybridData.length];
                System.arraycopy(hybridData, 0, bestResult, 0, hybridData.length);

                int[] keys = {0x20, 0x42, 0x69, 0x33, 0x55, 0x77};

                for (int key : keys) {
                    System.out.println("\ntry XOR 0x" + Integer.toHexString(key) + " for start:");

                    byte[] testResult = new byte[hybridData.length];
                    System.arraycopy(hybridData, 0, testResult, 0, hybridData.length);

                    // юзаем XOR только к первой части (до читаемого кода)
                    for (int i = 5; i < readableStart; i++) {
                        testResult[i] = (byte) ((hybridData[i] & 0xFF) ^ key);
                    }

                    String sample = new String(testResult, 5, Math.min(100, readableStart - 5))
                            .replaceAll("[\\x00-\\x1F\\x7F-\\xFF]", ".");
                    System.out.println("result: " + sample);

                    String fullText = new String(testResult).toLowerCase();
                    if (fullText.contains("function") || fullText.contains("local") ||
                            fullText.contains("return") || fullText.contains("end")) {
                        System.out.println("*** FOUND LUA WORDS with key 0x" + Integer.toHexString(key) + "! ***");

                        String outputFile = "File1_final_" + Integer.toHexString(key) + ".lua";
                        Files.write(Paths.get(outputFile), testResult);
                        System.out.println("saved: " + outputFile);
                    }
                }

                System.out.println("\n=== readable file part ===");
                if (readableStart < hybridData.length - 100) {
                    String readablePart = new String(hybridData, readableStart,
                            Math.min(500, hybridData.length - readableStart));
                    System.out.println(readablePart);
                }
            }

            createFinalVersion();

        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    private static void createFinalVersion() {
        try {
            InputStream inputStream = LuacFinalDecryptor.class.getClassLoader()
                    .getResourceAsStream("File1.luac");
            byte[] originalData = inputStream.readAllBytes();
            inputStream.close();

            System.out.println("\n=== creating final version ===");

            // первая часть: другой алгоритм
            // вторая часть (с позиции ±1400): оставляем как есть (уже читаемая)

            byte[] finalResult = new byte[originalData.length];
            System.arraycopy(originalData, 0, finalResult, 0, 5); // префикс

            // зона 1: с 5 до 1400 - пробуем разные
            for (int i = 5; i < 1400 && i < originalData.length; i++) {
                // простой XOR 0x20
                finalResult[i] = (byte) ((originalData[i] & 0xFF) ^ 0x20);
            }

            // Зона 2: с 1400 до конца - оставляем оригинал (уже чиатется)
            if (originalData.length > 1400) {
                System.arraycopy(originalData, 1400, finalResult, 1400, originalData.length - 1400);
            }

            String finalFile = "File1_FINAL.lua";
            Files.write(Paths.get(finalFile), finalResult);
            System.out.println("created final file: " + finalFile);

            String preview = new String(finalResult, 1400, Math.min(300, finalResult.length - 1400));
            System.out.println("\nfinal part preview:");
            System.out.println(preview);

        } catch (IOException e) {
            System.out.println("error creating final version: " + e.getMessage());
        }
    }
}
