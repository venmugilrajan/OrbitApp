package com.launchhub.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ShortcutResolver {

    /**
     * Resolves the target file path from a Windows shortcut (.lnk) file.
     */
    public static String resolveShortcut(File shortcutFile) {
        if (!shortcutFile.exists() || !shortcutFile.getName().toLowerCase().endsWith(".lnk")) {
            return shortcutFile.getAbsolutePath();
        }

        try (FileInputStream fis = new FileInputStream(shortcutFile)) {
            return parseLnkFile(fis);
        } catch (Exception e) {
            System.err.println("Failed to parse shortcut: " + shortcutFile.getName() + " - " + e.getMessage());
            return null;
        }
    }

    private static String parseLnkFile(FileInputStream fis) throws IOException {
        byte[] buffer = new byte[2048];
        int bytesRead = fis.read(buffer);
        if (bytesRead < 76) {
            return null;
        }

        // Check Shell Link signature (0x0000004c)
        if (buffer[0] != 0x4c || buffer[1] != 0x00 || buffer[2] != 0x00 || buffer[3] != 0x00) {
            return null;
        }

        // Flags are at offset 20 (0x14)
        int flags = buffer[20] & 0xFF;

        // Is there a LinkTargetIDList? (Bit 0)
        boolean hasIdList = (flags & 0x01) != 0;
        // Is there a LinkInfo? (Bit 1)
        boolean hasLinkInfo = (flags & 0x02) != 0;

        int currentOffset = 76;

        // Skip LinkTargetIDList if present
        if (hasIdList) {
            if (currentOffset + 2 > bytesRead) return null;
            int idListSize = ((buffer[currentOffset + 1] & 0xFF) << 8) | (buffer[currentOffset] & 0xFF);
            currentOffset += 2 + idListSize;
        }

        // Read LinkInfo structure
        if (hasLinkInfo && currentOffset + 4 <= bytesRead) {
            int linkInfoStart = currentOffset;
            int linkInfoSize = readInt(buffer, linkInfoStart);
            if (linkInfoSize < 28 || linkInfoStart + linkInfoSize > bytesRead) {
                return null;
            }

            int linkInfoFlags = readInt(buffer, linkInfoStart + 8);
            int localBasePathOffset = readInt(buffer, linkInfoStart + 16);

            // VolumeIDAndLocalBasePath
            if ((linkInfoFlags & 0x01) != 0 && localBasePathOffset > 0) {
                int pathStart = linkInfoStart + localBasePathOffset;
                if (pathStart < bytesRead) {
                    StringBuilder path = new StringBuilder();
                    for (int i = pathStart; i < bytesRead && buffer[i] != 0; i++) {
                        path.append((char) buffer[i]);
                    }
                    return path.toString();
                }
            }
        }

        return null;
    }

    private static int readInt(byte[] buffer, int offset) {
        return ((buffer[offset + 3] & 0xFF) << 24) |
               ((buffer[offset + 2] & 0xFF) << 16) |
               ((buffer[offset + 1] & 0xFF) << 8) |
               (buffer[offset] & 0xFF);
    }
}
