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
                System.out.println("file " + filename + "not found");
                return;
            }

            byte[] data = inputStream.readAllBytes();
            inputStream.close();

            System.out.println("file size: " + data.length + " byte");

            System.out.print("first 16 bytes: ");
            for (int i = 0; i < Math.min(16, data.length); i++) {
                System.out.printf("%02x ", data[i] & 0xFF);
            }
            System.out.println();

            // пропускаем префикс 9c55545344 (5 байт)
            if (data.length < 5) {
                System.out.println("file to short");
                return;
            }

            if (data[0] != (byte) 0x9c || data[1] != (byte) 0x55 ||
                    data[2] != (byte) 0x54 || data[3] != (byte) 0x53 ||
                    data[4] != (byte) 0x44) {
                System.out.println("unexpected prefix");
                System.out.printf("prefix: %02x %02x %02x %02x %02x\n",
                        data[0] & 0xFF, data[1] & 0xFF, data[2] & 0xFF, data[3] & 0xFF, data[4] & 0xFF);
            } else {
                System.out.println("prefix found!");
            }

            int[] xorKeys = {0x20, 0x42, 0x69, 0xFF, 0x33, 0x55, 0x77, 0xAA, 0x00, 0x01, 0x11, 0x22};

            for (int key : xorKeys) {
                System.out.println("\n=== XOR with key 0x" + String.format("%02x", key) + " ===");

                byte[] decrypted = new byte[data.length];

                System.arraycopy(data, 0, decrypted, 0, 5);

                // XOR расшифровка начиная с 6-го байта
                for (int i = 5; i < data.length; i++) {
                    decrypted[i] = (byte) ((data[i] & 0xFF) ^ key);
                }

                System.out.print("after decrypt: ");
                for (int i = 5; i < Math.min(15, decrypted.length); i++) {
                    System.out.printf("%02x ", decrypted[i] & 0xFF);
                }
                System.out.println();

                try {
                    String text = new String(decrypted, 5, Math.min(30, decrypted.length - 5), "UTF-8");
                    String cleanText = text.replaceAll("[\\x00-\\x1F\\x7F-\\xFF]", ".");
                    System.out.println("as text: " + cleanText);
                } catch (Exception e) {
                    System.out.println("decrypt text error");
                }

                // проверяем на lua сигнатуру (1B 4C 75 61 = ESC + "Lua")
                if (decrypted.length > 8) {
                    if ((decrypted[5] & 0xFF) == 0x1B && (decrypted[6] & 0xFF) == 0x4C &&
                            (decrypted[7] & 0xFF) == 0x75 && (decrypted[8] & 0xFF) == 0x61) {
                        System.out.println("***FOUND LUA SIGNATURE! ***");

                        System.out.print("Lua header: ");
                        for (int i = 5; i < Math.min(20, decrypted.length); i++) {
                            System.out.printf("%02x ", decrypted[i] & 0xFF);
                        }
                        System.out.println();

                        try {
                            String outputFile = "file_decrypted_" + String.format("%02x", key) + ".luac";
                            Files.write(Paths.get(outputFile), decrypted);
                            System.out.println("file saved: " + outputFile);
                        } catch (IOException e) {
                            System.out.println("saving error: " + e.getMessage());
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("file reading error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
