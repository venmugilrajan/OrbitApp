package com.launchhub.ui.controller;

import com.launchhub.repository.SettingsRepository;
import com.launchhub.service.ScannerService;
import com.launchhub.service.ThemeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

public class SettingsController {

    @FXML private ComboBox<String> comboTheme;
    @FXML private ComboBox<String> comboGridSize;
    @FXML private CheckBox checkAutoScan;
    @FXML private Button btnResetCache;

    private SettingsRepository settingsRepository;
    private ScannerService scannerService;
    private ThemeService themeService;
    private MainController mainController;

    public void init(SettingsRepository settingsRepository, ScannerService scannerService,
                     ThemeService themeService, MainController mainController) {
        this.settingsRepository = settingsRepository;
        this.scannerService = scannerService;
        this.themeService = themeService;
        this.mainController = mainController;

        setupSettingsValues();
        setupActions();
    }

    private void setupSettingsValues() {
        // Theme
        comboTheme.setItems(FXCollections.observableArrayList("Dark", "Light"));
        String activeTheme = themeService.getCurrentTheme();
        comboTheme.getSelectionModel().select(activeTheme.substring(0, 1).toUpperCase() + activeTheme.substring(1));

        // Grid Size
        comboGridSize.setItems(FXCollections.observableArrayList("Small", "Medium", "Large"));
        comboGridSize.getSelectionModel().select(
            settingsRepository.get("grid_size", "Medium")
        );

        // Auto Scan
        boolean isAuto = Boolean.parseBoolean(settingsRepository.get("auto_scan", "true"));
        checkAutoScan.setSelected(isAuto);
    }

    private void setupActions() {
        // Theme change handler
        comboTheme.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String themeKey = newVal.toLowerCase();
                Scene scene = comboTheme.getScene();
                if (scene != null) {
                    themeService.setTheme(scene, themeKey);
                }
            }
        });

        // Grid size handler
        comboGridSize.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                settingsRepository.set("grid_size", newVal);
            }
        });

        // Auto Scan handler
        checkAutoScan.selectedProperty().addListener((obs, oldVal, newVal) -> {
            settingsRepository.set("auto_scan", String.valueOf(newVal));
        });

        // Clear and Re-scan
        btnResetCache.setOnAction(e -> {
            btnResetCache.setText("Resetting...");
            btnResetCache.setDisable(true);
            new Thread(() -> {
                // We can run a scan directly which overwrites/updates
                scannerService.performScan();
                javafx.application.Platform.runLater(() -> {
                    btnResetCache.setText("Clear Library & Re-scan");
                    btnResetCache.setDisable(false);
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Library Reset");
                    alert.setHeaderText(null);
                    alert.setContentText("Application library has been successfully refreshed.");
                    alert.showAndWait();
                });
            }).start();
        });
    }
}
