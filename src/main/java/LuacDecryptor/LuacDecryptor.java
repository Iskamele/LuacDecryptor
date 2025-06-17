package LuacDecryptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LuacDecryptor {
    public static void main(String[] args) {
        String filename = "file.luac";

        try {
            InputStream inputStream = LuacDecryptor.class.getClassLoader().getResourceAsStream(filename);
            if (inputStream == null) {
                System.out.println("file " + filename + " not found!");
                return;
            }

            byte[] data = inputStream.readAllBytes();
            inputStream.close();

            System.out.println("file size: " + data.length + " bytes");

            System.out.println("\n=== try multi-byte XOR ===");

            String[] multiKeys = {
                    "UTSD",
                    "P3D",
                    "HACK",
                    "LUA",
                    "CODE",
                    "GAME"
            };

            for (String keyStr : multiKeys) {
                byte[] key = keyStr.getBytes();
                System.out.println("\ntry key: " + keyStr);

                byte[] decrypted = new byte[data.length];
                System.arraycopy(data, 0, decrypted, 0, 5); // префикс

                // многобайтовое XOR
                for (int i = 5; i < data.length; i++) {
                    decrypted[i] = (byte) ((data[i] & 0xFF) ^ (key[(i - 5) % key.length] & 0xFF));
                }

                if (decrypted.length > 8 &&
                        (decrypted[5] & 0xFF) == 0x1B && (decrypted[6] & 0xFF) == 0x4C &&
                        (decrypted[7] & 0xFF) == 0x75 && (decrypted[8] & 0xFF) == 0x61) {
                    System.out.println("*** LUA SIGNATURE WITH KEY FOUND " + keyStr + "! ***");

                    String outputFile = filename.replace(".luac", "_decrypted_" + keyStr + ".luac");
                    Files.write(Paths.get(outputFile), decrypted);
                    System.out.println("saved: " + outputFile);
                } else {
                    System.out.print("start: ");
                    for (int i = 5; i < Math.min(15, decrypted.length); i++) {
                        System.out.printf("%02x ", decrypted[i] & 0xFF);
                    }
                    System.out.println();
                }
            }

            // арифметическая херня
            System.out.println("\n=== try arithmetic operations ===");

            for (int shift = 1; shift <= 10; shift++) {
                byte[] decrypted = new byte[data.length];
                System.arraycopy(data, 0, decrypted, 0, 5);

                for (int i = 5; i < data.length; i++) {
                    decrypted[i] = (byte) ((data[i] & 0xFF) - shift);
                }

                if (decrypted.length > 8 &&
                        (decrypted[5] & 0xFF) == 0x1B && (decrypted[6] & 0xFF) == 0x4C &&
                        (decrypted[7] & 0xFF) == 0x75 && (decrypted[8] & 0xFF) == 0x61) {
                    System.out.println("*** LUA SIGNATURE FOUND with subtraction " + shift + "! ***");

                    String outputFile = filename.replace(".luac", "_decrypted_sub" + shift + ".luac");
                    Files.write(Paths.get(outputFile), decrypted);
                    System.out.println("saved: " + outputFile);
                }
            }

            System.out.println("\n=== check how the Lua source code ===");

            // XOR 0x20
            byte[] textDecrypted = new byte[data.length - 5];
            for (int i = 5; i < data.length; i++) {
                textDecrypted[i - 5] = (byte) ((data[i] & 0xFF) ^ 0x20);
            }

            try {
                String luaCode = new String(textDecrypted, "UTF-8");
                System.out.println("as Lua code (first 200 characters):");
                System.out.println(luaCode.substring(0, Math.min(200, luaCode.length())));

                // key words
                if (luaCode.contains("function") || luaCode.contains("local") ||
                        luaCode.contains("end") || luaCode.contains("if")) {
                    System.out.println("*** LUA KEYWORDS FOUND! ***");

                    String outputFile = filename.replace(".luac", "_source.lua");
                    Files.write(Paths.get(outputFile), luaCode.getBytes());
                    System.out.println("source code preserved: " + outputFile);
                }

            } catch (Exception e) {
                System.out.println("cannot decode as text");
            }

        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }
}
