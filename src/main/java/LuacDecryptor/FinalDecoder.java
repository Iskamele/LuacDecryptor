package LuacDecryptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public class FinalDecoder {
    public static void main(String[] args) {
        try {
            InputStream inputStream = FinalDecoder.class.getClassLoader()
                    .getResourceAsStream("file1.luac");
            byte[] data = inputStream.readAllBytes();
            inputStream.close();

            System.out.println("=== creating the final Lua file ===");

            Set<String> functions = new LinkedHashSet<>();
            Set<String> variables = new LinkedHashSet<>();
            Set<String> gameAPIs = new LinkedHashSet<>();

            String readablePart = new String(data, 1478, data.length - 1478);

            String[] tokens = readablePart.split("[\\x00-\\x1F]+");

            for (String token : tokens) {
                token = token.trim().replaceAll("^\"|\"$", "");

                if (token.length() > 2 && !token.matches(".*[\\x00-\\x1F].*")) {
                    if (token.contains("play_") || token.contains("damage_") ||
                            token.contains("get_") || token.contains("sync_")) {
                        functions.add(token);
                    } else if (token.equals("managers") || token.equals("session") ||
                            token.equals("network") || token.equals("math")) {
                        gameAPIs.add(token);
                    } else if (token.contains("_") || token.matches("^[a-z_]+$")) {
                        variables.add(token);
                    }
                }
            }

            StringBuilder luaCode = new StringBuilder();

            luaCode.append("-- FILE1 Module\n");
            luaCode.append("-- decompiled from encrypted .luac format\n");
            luaCode.append("-- this module handles something\n\n");

            luaCode.append("-- API access\n");
            for (String api : gameAPIs) {
                luaCode.append("local ").append(api).append(" = ").append(api).append("\n");
            }
            luaCode.append("\n");

            luaCode.append("-- Local variables\n");
            for (String var : variables) {
                if (!var.equals("base") && !var.equals("dead") && !var.equals("normal")) {
                    luaCode.append("local ").append(var).append(" = nil\n");
                }
            }
            luaCode.append("\n");

            luaCode.append("-- Main something handler\n");
            // strings
            luaCode.append("end\n\n");

            luaCode.append("-- Helper functions (referenced in collision handler)\n");
            for (String func : functions) {
                if (!func.equals("on_collision_something")) {
                    luaCode.append("-- ").append(func).append("() - function\n");
                }
            }

            luaCode.append("\n-- End of module\n");
            luaCode.append("-- This module enhances something\n");

            String finalFile = "File1_FINAL_DECODED.lua";
            Files.write(Paths.get(finalFile), luaCode.toString().getBytes());

            System.out.println("created final decoded file: " + finalFile);
            System.out.println("size: " + luaCode.length() + " symbols");
            System.out.println("functions found: " + functions.size());
            System.out.println("variables found: " + variables.size());

            System.out.println("\npreview of decoded file:");
            System.out.println(luaCode.toString().substring(0, Math.min(800, luaCode.length())));
            System.out.println("... (the file is saved completely)");

        } catch (IOException e) {
            System.out.println("error: " + e.getMessage());
        }
    }
}