package com.launchhub.service;

import com.launchhub.repository.SettingsRepository;
import javafx.scene.Scene;

public class ThemeService {
    private final SettingsRepository settingsRepository;
    private String currentTheme;

    public ThemeService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        this.currentTheme = settingsRepository.get("theme", "dark");
    }

    public void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        String cssPath = "/com/launchhub/css/" + currentTheme + ".css";
        var resource = getClass().getResource(cssPath);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        } else {
            System.err.println("Theme stylesheet not found: " + cssPath);
        }
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(Scene scene, String theme) {
        this.currentTheme = theme;
        settingsRepository.set("theme", theme);
        applyTheme(scene);
    }
}
