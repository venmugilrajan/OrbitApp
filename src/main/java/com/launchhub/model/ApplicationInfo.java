package com.launchhub.model;

public class ApplicationInfo {
    private int id;
    private String name;
    private String publisher;
    private String version;
    private String installLocation;
    private String installDate;
    private long sizeBytes;
    private String executablePath;
    private String iconPath;
    private String category;
    private boolean isFavorite;
    private int launchCount;
    private String lastUsed;

    public ApplicationInfo() {}

    public ApplicationInfo(int id, String name, String publisher, String version, String installLocation,
                           String installDate, long sizeBytes, String executablePath, String iconPath,
                           String category, boolean isFavorite, int launchCount, String lastUsed) {
        this.id = id;
        this.name = name;
        this.publisher = publisher;
        this.version = version;
        this.installLocation = installLocation;
        this.installDate = installDate;
        this.sizeBytes = sizeBytes;
        this.executablePath = executablePath;
        this.iconPath = iconPath;
        this.category = category;
        this.isFavorite = isFavorite;
        this.launchCount = launchCount;
        this.lastUsed = lastUsed;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getInstallLocation() { return installLocation; }
    public void setInstallLocation(String installLocation) { this.installLocation = installLocation; }

    public String getInstallDate() { return installDate; }
    public void setInstallDate(String installDate) { this.installDate = installDate; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getExecutablePath() { return executablePath; }
    public void setExecutablePath(String executablePath) { this.executablePath = executablePath; }

    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public int getLaunchCount() { return launchCount; }
    public void setLaunchCount(int launchCount) { this.launchCount = launchCount; }

    public String getLastUsed() { return lastUsed; }
    public void setLastUsed(String lastUsed) { this.lastUsed = lastUsed; }
}
