package LuacDecryptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class StructureAnalyzer {
    public static void main(String[] args) {
        try {
            InputStream inputStream = StructureAnalyzer.class.getClassLoader()
                    .getResourceAsStream("File1.luac");
            byte[] data = inputStream.readAllBytes();
            inputStream.close();

            System.out.println("=== file structure analysis ===");
            System.out.println("size: " + data.length + " bytes");

            List<StringSection> sections = findStringSections(data);

            System.out.println("\n=== found string sections ===");
            for (StringSection section : sections) {
                System.out.printf("position %d-%d: \"%s\"\n",
                        section.start, section.end, section.content);
            }

            System.out.println("\n=== analyzing data between lines ===");
            analyzeDataSections(data, sections);

            System.out.println("\n=== reconstructing the structure ===");
            reconstructFile(data);

        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }

    static class StringSection {
        int start, end;
        String content;

        StringSection(int start, int end, String content) {
            this.start = start;
            this.end = end;
            this.content = content;
        }
    }

    private static List<StringSection> findStringSections(byte[] data) {
        List<StringSection> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int start = -1;

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];

            if (b >= 32 && b <= 126) { // ASCII
                if (start == -1) start = i;
                current.append((char) b);
            } else {
                if (current.length() > 3) { // 4 символа
                    sections.add(new StringSection(start, i - 1, current.toString()));
                }
                current = new StringBuilder();
                start = -1;
            }
        }

        if (current.length() > 3) {
            sections.add(new StringSection(start, data.length - 1, current.toString()));
        }

        return sections;
    }

    private static void analyzeDataSections(byte[] data, List<StringSection> sections) {
        for (int i = 0; i < sections.size() - 1; i++) {
            StringSection current = sections.get(i);
            StringSection next = sections.get(i + 1);

            int dataStart = current.end + 1;
            int dataEnd = next.start - 1;

            if (dataEnd > dataStart) {
                System.out.printf("\nbetween \"%s\" и \"%s\":\n",
                        current.content.substring(0, Math.min(10, current.content.length())),
                        next.content.substring(0, Math.min(10, next.content.length())));

                System.out.print("Bytes: ");
                for (int j = dataStart; j <= Math.min(dataStart + 10, dataEnd); j++) {
                    System.out.printf("%02x ", data[j] & 0xFF);
                }
                System.out.println();

                if (dataEnd - dataStart == 1) {
                    System.out.println("single byte delimiter: 0x" +
                            Integer.toHexString(data[dataStart] & 0xFF));
                }
            }
        }
    }

    private static void reconstructFile(byte[] data) {
        StringBuilder result = new StringBuilder();

        result.append("-- module\n");
        result.append("-- reconstructed from encrypted format\n\n");

        List<String> functions = new ArrayList<>();
        List<String> variables = new ArrayList<>();
        List<String> constants = new ArrayList<>();

        String fileContent = new String(data, 1400, data.length - 1400);
        Scanner scanner = new Scanner(fileContent);
        scanner.useDelimiter("[\\x00-\\x1F]+");

        while (scanner.hasNext()) {
            String token = scanner.next().trim();
            if (token.length() > 2) {
                if (token.contains("_") && !token.contains(" ")) {
                    if (token.startsWith("play_") || token.startsWith("get_") ||
                            token.startsWith("damage_") || token.contains("sound")) {
                        functions.add(token);
                    } else {
                        variables.add(token);
                    }
                } else if (token.matches("^[A-Z][a-zA-Z]+$")) {
                    constants.add(token);
                }
            }
        }
        scanner.close();

        result.append("-- constants\n");
        for (String constant : constants) {
            result.append("local ").append(constant).append(" = true\n");
        }

        result.append("\n-- variables\n");
        for (String var : variables) {
            result.append("local ").append(var).append(" = nil\n");
        }

        result.append("\n-- functions used in this module:\n");
        for (String func : functions) {
            result.append("-- ").append(func).append("()\n");
        }

        result.append("\n-- main bullet impact function\n");
        result.append("function on_collision(data)\n");
        result.append("    -- physics and damage calculations\n");
        for (String func : functions) {
            if (func.contains("damage") || func.contains("impact") || func.contains("physics")) {
                result.append("    ").append(func).append("(data)\n");
            }
        }
        result.append("end\n");

        try {
            String outputFile = "File1_RECONSTRUCTED.lua";
            Files.write(Paths.get(outputFile), result.toString().getBytes());
            System.out.println("created recreated file: " + outputFile);
            System.out.println("\ncontent:");
            System.out.println(result);
        } catch (IOException e) {
            System.out.println("saving error: " + e.getMessage());
        }
    }
}
