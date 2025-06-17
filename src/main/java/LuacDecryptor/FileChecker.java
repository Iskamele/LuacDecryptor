package LuacDecryptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileChecker {
    public static void main(String[] args) {
        String[] files = {
                "file1_final_20.lua",
                "file1_final_42.lua",
                "file1_final_69.lua",
                "file1_final_33.lua",
                "file1_FINAL.lua"
        };

        for (String filename : files) {
            try {
                if (Files.exists(Paths.get(filename))) {
                    byte[] data = Files.readAllBytes(Paths.get(filename));
                    String content = new String(data);

                    System.out.println("\n=== " + filename + " ===");
                    System.out.println("size: " + data.length + " bytes");

                    int functionCount = countOccurrences(content, "function");
                    int localCount = countOccurrences(content, "local");
                    int endCount = countOccurrences(content, "end");
                    int returnCount = countOccurrences(content, "return");

                    System.out.println("function: " + functionCount);
                    System.out.println("local: " + localCount);
                    System.out.println("end: " + endCount);
                    System.out.println("return: " + returnCount);

                    int readableStart = content.indexOf("play_impact_sound");
                    if (readableStart == -1) readableStart = content.indexOf("damage_fire");
                    if (readableStart == -1) readableStart = 1400;

                    if (readableStart < content.length() - 50) {
                        String sample = content.substring(readableStart,
                                Math.min(readableStart + 200, content.length()));
                        System.out.println("readable fragment:");
                        System.out.println(sample.replaceAll("[\\x00-\\x1F\\x7F-\\xFF]", "."));
                    }

                    String beginning = content.substring(5, Math.min(200, content.length()));
                    boolean hasLuaStructure = beginning.contains("function") ||
                            beginning.contains("local") ||
                            beginning.contains("=") ||
                            beginning.contains("--");

                    System.out.println("has a Lua structure at the beginning: " + hasLuaStructure);

                    if (hasLuaStructure) {
                        System.out.println("*** LOOKS LIKE CORRECT LUA CODE! ***");
                        System.out.println("start of file:");
                        System.out.println(beginning.substring(0, Math.min(100, beginning.length()))
                                .replaceAll("[\\x00-\\x1F\\x7F-\\xFF]", "."));
                    }
                }
            } catch (IOException e) {
                System.out.println("read error " + filename + ": " + e.getMessage());
            }
        }

        System.out.println("\n=== creating a clean version ===");
        createCleanVersion();
    }

    private static int countOccurrences(String text, String word) {
        return (text.length() - text.replace(word, "").length()) / word.length();
    }

    private static void createCleanVersion() {
        try {
            String bestFile = "file1_final_20.lua";

            if (Files.exists(Paths.get(bestFile))) {
                byte[] data = Files.readAllBytes(Paths.get(bestFile));

                byte[] cleanData = new byte[data.length - 5];
                System.arraycopy(data, 5, cleanData, 0, data.length - 5);

                for (int i = 0; i < cleanData.length; i++) {
                    if (cleanData[i] < 32 || cleanData[i] > 126) {
                        if (cleanData[i] == 0) {
                            cleanData[i] = 10; // заменяем \0 на \n
                        } else {
                            cleanData[i] = 32; // остальные на пробел
                        }
                    }
                }

                String cleanFile = "File1_CLEAN.lua";
                Files.write(Paths.get(cleanFile), cleanData);
                System.out.println("created clean file: " + cleanFile);

                String preview = new String(cleanData, 0, Math.min(500, cleanData.length));
                System.out.println("\nclean file preview:");
                System.out.println(preview);
            }
        } catch (IOException e) {
            System.out.println("error creating clean version: " + e.getMessage());
        }
    }
}
