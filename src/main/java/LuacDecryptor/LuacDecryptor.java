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

            System.out.print("first 20 bytes: ");
            for (int i = 0; i < Math.min(20, data.length); i++) {
                System.out.printf("%02x ", data[i] & 0xFF);
            }
            System.out.println();

            System.out.print("last 10 bytes: ");
            for (int i = Math.max(0, data.length - 10); i < data.length; i++) {
                System.out.printf("%02x ", data[i] & 0xFF);
            }
            System.out.println();

            if (data.length >= 5 && data[0] == (byte)0x9c && data[1] == (byte)0x55 &&
                    data[2] == (byte)0x54 && data[3] == (byte)0x53 && data[4] == (byte)0x44) {
                System.out.println("prefix found!");

                // пробуем XOR с самыми перспективными ключами
                int[] xorKeys = {0x20, 0x42, 0x69, 0x33};

                for (int key : xorKeys) {
                    System.out.println("\n=== XOR with key 0x" + String.format("%02x", key) + " ===");

                    byte[] decrypted = new byte[data.length];
                    System.arraycopy(data, 0, decrypted, 0, 5);

                    for (int i = 5; i < data.length; i++) {
                        decrypted[i] = (byte)((data[i] & 0xFF) ^ key);
                    }

                    System.out.print("start after prefix: ");
                    for (int i = 5; i < Math.min(25, decrypted.length); i++) {
                        System.out.printf("%02x ", decrypted[i] & 0xFF);
                    }
                    System.out.println();

                    if (decrypted.length > 8 &&
                            (decrypted[5] & 0xFF) == 0x1B && (decrypted[6] & 0xFF) == 0x4C &&
                            (decrypted[7] & 0xFF) == 0x75 && (decrypted[8] & 0xFF) == 0x61) {
                        System.out.println("*** LUA SIGNATURE FOUND! ***");

                        System.out.print("Lua header (first 30 bytes): ");
                        for (int i = 5; i < Math.min(35, decrypted.length); i++) {
                            System.out.printf("%02x ", decrypted[i] & 0xFF);
                        }
                        System.out.println();

                        String outputFile = filename.replace(".luac", "_decrypted.luac");
                        Files.write(Paths.get(outputFile), decrypted);
                        System.out.println("saved: " + outputFile);

                        System.out.println("\nlet's try to decompile...");
                        tryDecompile(outputFile);
                    }

                    if (decrypted.length > 50) {
                        try {
                            String text = new String(decrypted, 5, Math.min(100, decrypted.length - 5), "UTF-8");
                            String cleanText = text.replaceAll("[\\x00-\\x1F\\x7F-\\xFF]", ".");
                            System.out.println("as test: " + cleanText);
                        } catch (Exception e) {
                            System.out.println("text decompiling error");
                        }
                    }
                }
            } else {
                System.out.println("unexpected prefix!");
            }

        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void tryDecompile(String filename) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(filename));
            System.out.println("trying to parse as Lua file...");
            System.out.println("size: " + data.length + " bytes");

            if (data.length > 12) {
                System.out.printf("signature: %02x %02x %02x %02x\n",
                        data[5] & 0xFF, data[6] & 0xFF, data[7] & 0xFF, data[8] & 0xFF);
                System.out.printf("version: %02x\n", data[9] & 0xFF);
                System.out.printf("format: %02x\n", data[10] & 0xFF);
            }

        } catch (Exception e) {
            System.out.println("analysis error: " + e.getMessage());
        }
    }
}
