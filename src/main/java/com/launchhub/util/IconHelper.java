package com.launchhub.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class IconHelper {

    public static String extractAndSaveIcon(String exePath, String outputDir, String appName) {
        if (exePath == null || exePath.isEmpty()) {
            return null;
        }

        if (exePath.startsWith("shell:AppsFolder\\")) {
            return extractUwpIcon(exePath, outputDir, appName);
        }

        try {
            // Strip index and quotes
            String cleanPath = exePath;
            if (cleanPath.contains(",")) {
                int lastComma = cleanPath.lastIndexOf(",");
                if (lastComma > cleanPath.lastIndexOf(".")) {
                    cleanPath = cleanPath.substring(0, lastComma);
                }
            }
            cleanPath = cleanPath.replace("\"", "").trim();

            File exeFile = new File(cleanPath);
            if (!exeFile.exists()) {
                return null;
            }

            // Extract a crisp 128x128 icon, fall back to large icon (32x32) if unavailable
            sun.awt.shell.ShellFolder sf = sun.awt.shell.ShellFolder.getShellFolder(exeFile);
            java.awt.Image largeImage = null;
            try {
                largeImage = sf.getIcon(128, 128);
            } catch (Exception e) {
                // fall through
            }
            if (largeImage == null) {
                largeImage = sf.getIcon(true);
            }
            if (largeImage == null) {
                return null;
            }

            int width = largeImage.getWidth(null);
            int height = largeImage.getHeight(null);
            if (width <= 0 || height <= 0) {
                width = 128;
                height = 128;
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(largeImage, 0, 0, null);
            g.dispose();

            File outFolder = new File(outputDir);
            if (!outFolder.exists()) {
                outFolder.mkdirs();
            }

            String safeName = appName.replaceAll("[^a-zA-Z0-9-_]", "_") + ".png";
            File outFile = new File(outFolder, safeName);
            ImageIO.write(image, "PNG", outFile);
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Error extracting icon for " + appName + ": " + e.getMessage());
            return null;
        }
    }

    private static java.util.Map<String, String> uwpPackageLocations = null;

    public static synchronized void loadUwpPackages() {
        if (uwpPackageLocations != null) return;
        uwpPackageLocations = new java.util.HashMap<>();
        try {
            System.out.println("Caching UWP app installation locations...");
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                "Get-AppxPackage | ForEach-Object { $_.PackageFamilyName + '::' + $_.InstallLocation }"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || !line.contains("::")) continue;
                String[] parts = line.split("::", 2);
                if (parts.length == 2) {
                    uwpPackageLocations.put(parts[0].toLowerCase(), parts[1].trim());
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error loading UWP packages: " + e.getMessage());
        }
    }

    public static String extractUwpIcon(String exePath, String outputDir, String appName) {
        if (exePath == null || !exePath.startsWith("shell:AppsFolder\\")) {
            return null;
        }

        try {
            loadUwpPackages();

            String appId = exePath.substring("shell:AppsFolder\\".length());
            String pfn = appId.split("!")[0];

            // For Windows Settings or immersive control panel, use its default image
            if (pfn.toLowerCase().contains("immersivecontrolpanel")) {
                File settingsImg = new File("C:\\Windows\\ImmersiveControlPanel\\images\\Apps.png");
                if (settingsImg.exists()) {
                    File outFolder = new File(outputDir);
                    if (!outFolder.exists()) outFolder.mkdirs();
                    String safeName = appName.replaceAll("[^a-zA-Z0-9-_]", "_") + ".png";
                    File outFile = new File(outFolder, safeName);
                    java.nio.file.Files.copy(settingsImg.toPath(), outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return outFile.getAbsolutePath();
                }
            }

            String installLocation = uwpPackageLocations.get(pfn.toLowerCase());
            
            // If not found directly, try finding package by key containment (e.g. if the PFN matched the package name instead)
            if (installLocation == null || installLocation.trim().isEmpty()) {
                for (java.util.Map.Entry<String, String> entry : uwpPackageLocations.entrySet()) {
                    if (entry.getKey().contains(pfn.toLowerCase()) || pfn.toLowerCase().contains(entry.getKey())) {
                        installLocation = entry.getValue();
                        break;
                    }
                }
            }

            if (installLocation == null || installLocation.trim().isEmpty()) {
                return null;
            }

            installLocation = installLocation.trim();
            String relativeLogoPath = getUwpLogoPath(installLocation);
            if (relativeLogoPath == null || relativeLogoPath.isEmpty()) {
                return null;
            }

            // Clean relative path (remove extension if present)
            String baseLogoPath = relativeLogoPath;
            if (baseLogoPath.toLowerCase().endsWith(".png")) {
                baseLogoPath = baseLogoPath.substring(0, baseLogoPath.length() - 4);
            }
            baseLogoPath = baseLogoPath.replace("/", File.separator).replace("\\", File.separator);

            File resolvedLogoFile = findBestUwpLogo(new File(installLocation), baseLogoPath);
            if (resolvedLogoFile != null && resolvedLogoFile.exists()) {
                File outFolder = new File(outputDir);
                if (!outFolder.exists()) {
                    outFolder.mkdirs();
                }

                String safeName = appName.replaceAll("[^a-zA-Z0-9-_]", "_") + ".png";
                File outFile = new File(outFolder, safeName);
                java.nio.file.Files.copy(resolvedLogoFile.toPath(), outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return outFile.getAbsolutePath();
            }
        } catch (Exception e) {
            System.err.println("Error extracting UWP icon for " + appName + ": " + e.getMessage());
        }
        return null;
    }

    private static String getUwpLogoPath(String installLocation) {
        try {
            File manifestFile = new File(installLocation, "AppxManifest.xml");
            if (!manifestFile.exists()) {
                return null;
            }

            javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(manifestFile);
            doc.getDocumentElement().normalize();

            org.w3c.dom.NodeList nList = doc.getElementsByTagNameNS("*", "VisualElements");
            if (nList.getLength() > 0) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) nList.item(0);
                String logoAttr = element.getAttribute("Square150x150Logo");
                if (logoAttr == null || logoAttr.isEmpty()) {
                    logoAttr = element.getAttribute("Square44x44Logo");
                }
                if (logoAttr == null || logoAttr.isEmpty()) {
                    logoAttr = element.getAttribute("Logo");
                }
                if (logoAttr != null && !logoAttr.isEmpty()) {
                    return logoAttr;
                }
            }

            org.w3c.dom.NodeList propList = doc.getElementsByTagNameNS("*", "Properties");
            if (propList.getLength() > 0) {
                org.w3c.dom.Element propElement = (org.w3c.dom.Element) propList.item(0);
                org.w3c.dom.NodeList logoList = propElement.getElementsByTagNameNS("*", "Logo");
                if (logoList.getLength() > 0) {
                    return logoList.item(0).getTextContent();
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing AppxManifest.xml: " + e.getMessage());
        }
        return null;
    }

    private static File findBestUwpLogo(File installDir, String baseLogoPath) {
        File baseFile = new File(installDir, baseLogoPath);
        File parentDir = baseFile.getParentFile();
        if (parentDir == null || !parentDir.exists()) {
            return null;
        }

        String baseName = baseFile.getName();
        File[] candidates = parentDir.listFiles();
        if (candidates == null) {
            return null;
        }

        // Search for scale/targetsize matches in order of preference
        String[] preferences = {
            ".scale-200.png",
            ".scale-100.png",
            ".scale-150.png",
            ".scale-125.png",
            ".targetsize-256.png",
            ".targetsize-48.png",
            ".png"
        };

        for (String suffix : preferences) {
            for (File c : candidates) {
                if (c.getName().equalsIgnoreCase(baseName + suffix)) {
                    return c;
                }
            }
        }

        // Wildcard match fallback
        for (File c : candidates) {
            if (c.getName().toLowerCase().startsWith(baseName.toLowerCase()) && c.getName().toLowerCase().endsWith(".png")) {
                return c;
            }
        }

        return null;
    }
}
