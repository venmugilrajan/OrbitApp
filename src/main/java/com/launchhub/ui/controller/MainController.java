package com.launchhub.ui.controller;

import com.launchhub.repository.ApplicationRepository;
import com.launchhub.repository.SettingsRepository;
import com.launchhub.service.ScannerService;
import com.launchhub.service.ThemeService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainController {

    @FXML private ToggleButton btnDashboard;
    @FXML private ToggleButton btnFavorites;
    @FXML private ToggleButton btnCategories;
    @FXML private ToggleButton btnStatistics;
    @FXML private ToggleButton btnSettings;
    @FXML private BorderPane contentArea;

    private ApplicationRepository appRepository;
    private SettingsRepository settingsRepository;
    private ScannerService scannerService;
    private ThemeService themeService;

    public void init(ApplicationRepository appRepository, SettingsRepository settingsRepository,
                     ScannerService scannerService, ThemeService themeService) {
        this.appRepository = appRepository;
        this.settingsRepository = settingsRepository;
        this.scannerService = scannerService;
        this.themeService = themeService;

        setupNavigation();
        showDashboard(); // default view
    }

    private void setupNavigation() {
        btnDashboard.setOnAction(e -> showDashboard());
        btnFavorites.setOnAction(e -> showFavorites());
        btnCategories.setOnAction(e -> showCategories());
        btnStatistics.setOnAction(e -> showStatistics());
        btnSettings.setOnAction(e -> showSettings());
    }

    private void selectButton(ToggleButton selectedButton) {
        btnDashboard.setSelected(false);
        btnFavorites.setSelected(false);
        btnCategories.setSelected(false);
        btnStatistics.setSelected(false);
        btnSettings.setSelected(false);
        
        selectedButton.setSelected(true);
    }

    private void loadView(String fxmlFile, java.util.function.Consumer<Object> initCallback) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/launchhub/fxml/" + fxmlFile));
            Parent view = loader.load();
            Object controller = loader.getController();
            if (initCallback != null) {
                initCallback.accept(controller);
            }
            contentArea.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showDashboard() {
        selectButton(btnDashboard);
        loadView("dashboard.fxml", obj -> {
            DashboardController controller = (DashboardController) obj;
            controller.init(appRepository, scannerService, this);
        });
    }

    public void showFavorites() {
        selectButton(btnFavorites);
        loadView("favorites.fxml", obj -> {
            FavoritesController controller = (FavoritesController) obj;
            controller.init(appRepository, this);
        });
    }

    public void showCategories() {
        selectButton(btnCategories);
        loadView("categories.fxml", obj -> {
            CategoriesController controller = (CategoriesController) obj;
            controller.init(appRepository, this);
        });
    }

    public void showStatistics() {
        selectButton(btnStatistics);
        loadView("statistics.fxml", obj -> {
            StatisticsController controller = (StatisticsController) obj;
            controller.init(appRepository);
        });
    }

    public void showSettings() {
        selectButton(btnSettings);
        loadView("settings.fxml", obj -> {
            SettingsController controller = (SettingsController) obj;
            controller.init(settingsRepository, scannerService, themeService, this);
        });
    }
}
