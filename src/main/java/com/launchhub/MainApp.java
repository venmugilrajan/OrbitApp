package com.launchhub;

import com.launchhub.repository.ApplicationRepository;
import com.launchhub.repository.SettingsRepository;
import com.launchhub.service.ScannerService;
import com.launchhub.service.ThemeService;
import com.launchhub.ui.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;

public class MainApp extends Application {

    private ApplicationRepository appRepository;
    private SettingsRepository settingsRepository;
    private ScannerService scannerService;
    private ThemeService themeService;

    @Override
    public void init() throws Exception {
        // Initialize layers
        appRepository = new ApplicationRepository();
        settingsRepository = new SettingsRepository();
        scannerService = new ScannerService(appRepository);
        themeService = new ThemeService(settingsRepository);

        // Auto Scan on Startup check
        boolean autoScan = Boolean.parseBoolean(settingsRepository.get("auto_scan", "true"));
        if (autoScan) {
            System.out.println("Auto Scan is enabled. Performing scan in background...");
            new Thread(() -> {
                scannerService.performScan();
            }).start();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/launchhub/fxml/main.fxml"));
        Parent root = loader.load();

        // Inject dependencies into MainController
        MainController mainController = loader.getController();
        mainController.init(appRepository, settingsRepository, scannerService, themeService);

        Scene scene = new Scene(root);
        themeService.applyTheme(scene);

        primaryStage.setTitle("OrbitApps - Windows Application Dashboard");
        
        // App icon for titlebar
        var iconStream = getClass().getResourceAsStream("/com/launchhub/css/default_app.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
