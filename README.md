# 🚀 OrbitApps

OrbitApps is a premium, visually stunning **Windows Application Dashboard & Launcher** built using JavaFX. It features a modern iOS-inspired **liquid glassmorphism** theme, providing a centralized control panel to organize, track, and launch all software on your PC.

---

## ✨ Features

* **iOS-Style Liquid Glassmorphism**: High-transparency frosted glass panels over a vibrant, colorful ambient neon mesh gradient background. Available in both Light and Dark modes.
* **Unified Application Scanner**: Automatically indexes both classic Win32 applications and Microsoft Store (UWP) apps (like Settings, Calculator, Spotify, Apple Music).
* **Smart Categorization**: Classifies apps into categories (Browsers, Development, Media, Utilities, etc.) with support for creating custom user categories.
* **Contextual Actions**: Right-click any app card to add/remove from favorites, change category, view details, open the install directory, or remove it from the dashboard.
* **Statistics & CleanUp**: Track launch counts, recently used software, and find space-consuming applications that you haven't opened in 30, 90, or 180 days.
* **Instant Search**: Capsule search bar with an integrated, context-aware "✕" clear button overlay.

---

## 🛠️ Tech Stack

* **Language**: Java 21+ (Compatible with JDK 25)
* **GUI Framework**: JavaFX 21
* **Windows APIs**: JNA (Java Native Access) for registry reads and shortcut resolution
* **Database**: SQLite JDBC for local statistics and user preferences
* **Packaging**: Maven & JDK `jpackage` for native `.exe` bundling

---

## 🚀 How to Run

### Option 1: Portable Executable (Recommended)
1. Go to the [Releases](https://github.com/venmugilrajan/OrbitApp/releases) page.
2. Download **`OrbitApps-v1.0.0-windows-x64.zip`**.
3. Extract the ZIP file and run **`OrbitApps.exe`** directly!

### Option 2: Build from Source
Ensure you have **JDK 21 or later** installed:

1. Clone the repository:
   ```bash
   git clone https://github.com/venmugilrajan/OrbitApp.git
   cd OrbitApp
   ```
2. Compile and run:
   ```bash
   mvn clean javafx:run
   ```
3. Create a native Windows portable app bundle:
   ```bash
   mvn clean package
   jpackage --name OrbitApps --input target --main-jar launchhub-1.0-SNAPSHOT.jar --main-class com.launchhub.Launcher --dest dist --type app-image --java-options "--add-exports=java.desktop/sun.awt.shell=ALL-UNNAMED"
   ```
   The executable will be generated at `dist/OrbitApps/OrbitApps.exe`.

---

## 📄 License
This project is open-source and free to use.
