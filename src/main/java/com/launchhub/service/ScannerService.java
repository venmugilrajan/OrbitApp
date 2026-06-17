package com.launchhub.service;

import com.launchhub.model.ApplicationInfo;
import com.launchhub.repository.ApplicationRepository;
import com.launchhub.repository.DatabaseManager;
import com.launchhub.util.IconHelper;
import com.launchhub.util.ShortcutResolver;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScannerService {

    private final ApplicationRepository repository;

    public ScannerService(ApplicationRepository repository) {
        this.repository = repository;
    }

    public void performScan() {
        System.out.println("Starting application scan...");
        Map<String, ApplicationInfo> scannedApps = new HashMap<>();

        // 1. Scan Start Menu
        scanStartMenu(scannedApps);

        // 2. Scan Windows Registry
        scanRegistry(scannedApps);

        // 3. Scan UWP Apps
        scanUwpApps(scannedApps);

        // Save/Update in DB
        String iconDir = DatabaseManager.getDbDirectory() + File.separator + "icons";
        for (ApplicationInfo app : scannedApps.values()) {
            // Classify category if not set
            if (app.getCategory() == null || app.getCategory().isEmpty() || app.getCategory().equals("Others")) {
                app.setCategory(CategoryService.classify(app.getName(), app.getPublisher(), app.getInstallLocation()));
            }

            // Extract icon if not cached
            if (app.getIconPath() == null || app.getIconPath().isEmpty() || !new File(app.getIconPath()).exists()) {
                String cachedIcon = IconHelper.extractAndSaveIcon(app.getExecutablePath(), iconDir, app.getName());
                if (cachedIcon != null) {
                    app.setIconPath(cachedIcon);
                }
            }

            // Save to DB
            repository.save(app);
        }
        System.out.println("Scan completed. Total applications: " + scannedApps.size());
    }

    private void scanUwpApps(Map<String, ApplicationInfo> scannedApps) {
        try {
            System.out.println("Scanning UWP apps...");
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                "Get-StartApps | ForEach-Object { $_.Name + '::' + $_.AppID }"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)
            );

            java.util.Set<String> existingNames = new java.util.HashSet<>();
            for (ApplicationInfo app : scannedApps.values()) {
                existingNames.add(app.getName().toLowerCase());
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || !line.contains("::")) {
                    continue;
                }
                String[] parts = line.split("::", 2);
                if (parts.length < 2) continue;
                String name = parts[0].trim();
                String appId = parts[1].trim();

                // Skip system items that are not useful or duplicates
                if (name.isEmpty() || appId.isEmpty()) continue;

                // Check if name is already present
                if (!existingNames.contains(name.toLowerCase())) {
                    // Filter or check if it is UWP / Store app
                    boolean isUwp = appId.contains("!") || !appId.contains("\\") || appId.startsWith("windows.");
                    
                    if (isUwp) {
                        ApplicationInfo app = new ApplicationInfo();
                        app.setName(name);
                        app.setExecutablePath("shell:AppsFolder\\" + appId);
                        app.setInstallLocation("");
                        app.setPublisher("Microsoft Store");
                        app.setVersion("1.0");
                        app.setInstallDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
                        app.setSizeBytes(0);
                        
                        scannedApps.put(app.getExecutablePath().toLowerCase(), app);
                        existingNames.add(name.toLowerCase());
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error scanning UWP apps: " + e.getMessage());
        }
    }

    private void scanStartMenu(Map<String, ApplicationInfo> scannedApps) {
        String commonStartMenu = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs";
        String userStartMenu = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs";

        scanStartMenuDirectory(new File(commonStartMenu), scannedApps);
        scanStartMenuDirectory(new File(userStartMenu), scannedApps);
    }

    private void scanStartMenuDirectory(File dir, Map<String, ApplicationInfo> scannedApps) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanStartMenuDirectory(file, scannedApps);
            } else if (file.getName().toLowerCase().endsWith(".lnk")) {
                String targetPath = ShortcutResolver.resolveShortcut(file);
                String name = file.getName().substring(0, file.getName().length() - 4);
                
                ApplicationInfo app = new ApplicationInfo();
                app.setName(name);
                app.setInstallDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(file.lastModified())));

                if (targetPath != null && targetPath.toLowerCase().endsWith(".exe")) {
                    File exeFile = new File(targetPath);
                    if (exeFile.exists()) {
                        app.setExecutablePath(targetPath);
                        app.setInstallLocation(exeFile.getParent());
                        app.setSizeBytes(exeFile.length());
                    }
                } else {
                    // Fallback for UWP apps / System settings shortcuts (e.g. Settings, Calculator)
                    app.setExecutablePath(file.getAbsolutePath());
                    app.setInstallLocation(file.getParent());
                    app.setSizeBytes(file.length());
                }

                if (app.getExecutablePath() != null) {
                    scannedApps.put(app.getExecutablePath().toLowerCase(), app);
                }
            }
        }
    }

    private void scanRegistry(Map<String, ApplicationInfo> scannedApps) {
        // Registry paths
        String[] registryPaths = {
            "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "SOFTWARE\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        };

        // HKLM
        for (String path : registryPaths) {
            scanRegistryRoot(WinReg.HKEY_LOCAL_MACHINE, path, scannedApps);
        }

        // HKCU
        scanRegistryRoot(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall", scannedApps);
    }

    private void scanRegistryRoot(WinReg.HKEY rootKey, String path, Map<String, ApplicationInfo> scannedApps) {
        try {
            if (!Advapi32Util.registryKeyExists(rootKey, path)) {
                return;
            }

            String[] subKeys = Advapi32Util.registryGetKeys(rootKey, path);
            for (String subKey : subKeys) {
                String fullPath = path + "\\" + subKey;
                try {
                    Map<String, Object> values = Advapi32Util.registryGetValues(rootKey, fullPath);
                    String displayName = (String) values.get("DisplayName");
                    if (displayName == null || displayName.trim().isEmpty()) {
                        continue;
                    }

                    // Exclude system updates, patches, drivers, etc.
                    if (displayName.contains("Security Update") || displayName.contains("Update for Windows") || displayName.contains("KB") && displayName.matches(".*KB\\d+.*")) {
                        continue;
                    }

                    String publisher = (String) values.get("Publisher");
                    String displayVersion = (String) values.get("DisplayVersion");
                    String installLocation = (String) values.get("InstallLocation");
                    String displayIcon = (String) values.get("DisplayIcon");
                    String installDate = (String) values.get("InstallDate");

                    long sizeBytes = 0;
                    if (values.containsKey("EstimatedSize")) {
                        Object sizeObj = values.get("EstimatedSize");
                        if (sizeObj instanceof Number) {
                            sizeBytes = ((Number) sizeObj).longValue() * 1024; // KB to Bytes
                        }
                    }

                    // Try to resolve the executable path
                    String exePath = null;
                    if (displayIcon != null && !displayIcon.trim().isEmpty()) {
                        exePath = displayIcon.split(",")[0].replace("\"", "").trim();
                    }

                    if (exePath == null || !exePath.toLowerCase().endsWith(".exe")) {
                        // Fallback to searching the install location for files
                        if (installLocation != null && !installLocation.trim().isEmpty()) {
                            File installDir = new File(installLocation.replace("\"", "").trim());
                            if (installDir.exists() && installDir.isDirectory()) {
                                File[] files = installDir.listFiles();
                                if (files != null) {
                                    for (File f : files) {
                                        if (f.isFile() && f.getName().toLowerCase().endsWith(".exe") && !f.getName().toLowerCase().contains("uninstall")) {
                                            exePath = f.getAbsolutePath();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (exePath != null && !exePath.isEmpty() && new File(exePath).exists()) {
                        String key = exePath.toLowerCase();
                        if (scannedApps.containsKey(key)) {
                            // Merge details
                            ApplicationInfo existing = scannedApps.get(key);
                            if (existing.getPublisher() == null) existing.setPublisher(publisher);
                            if (existing.getVersion() == null) existing.setVersion(displayVersion);
                            if (existing.getSizeBytes() == 0) existing.setSizeBytes(sizeBytes);
                            if (installDate != null && !installDate.isEmpty()) {
                                existing.setInstallDate(formatInstallDate(installDate));
                            }
                        } else {
                            ApplicationInfo app = new ApplicationInfo();
                            app.setName(displayName);
                            app.setPublisher(publisher);
                            app.setVersion(displayVersion);
                            app.setInstallLocation(installLocation);
                            app.setExecutablePath(exePath);
                            app.setSizeBytes(sizeBytes == 0 ? new File(exePath).length() : sizeBytes);
                            app.setInstallDate(formatInstallDate(installDate));
                            scannedApps.put(key, app);
                        }
                    }
                } catch (Exception e) {
                    // Ignore reading errors on single keys
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading registry path: " + path + " - " + e.getMessage());
        }
    }

    private String formatInstallDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        }
        // Often formatted as YYYYMMDD in registry
        if (rawDate.length() == 8) {
            try {
                return rawDate.substring(0, 4) + "-" + rawDate.substring(4, 6) + "-" + rawDate.substring(6, 8);
            } catch (Exception e) {
                // fall through
            }
        }
        return rawDate;
    }
}
