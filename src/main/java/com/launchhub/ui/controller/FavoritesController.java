package com.launchhub.ui.controller;

import com.launchhub.model.ApplicationInfo;
import com.launchhub.repository.ApplicationRepository;
import com.launchhub.ui.component.AppCard;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FavoritesController {

    @FXML private TextField searchField;
    @FXML private Button btnClearSearch;
    @FXML private FlowPane appsGrid;

    private ApplicationRepository appRepository;
    private MainController mainController;
    private List<ApplicationInfo> favApps = new ArrayList<>();

    public void init(ApplicationRepository appRepository, MainController mainController) {
        this.appRepository = appRepository;
        this.mainController = mainController;

        setupActions();
        loadFavorites();
    }

    private void setupActions() {
        btnClearSearch.setOnAction(e -> searchField.clear());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            btnClearSearch.setVisible(newValue != null && !newValue.isEmpty());
            filterApps(newValue);
        });
    }

    private void loadFavorites() {
        favApps = appRepository.getFavorites();
        populateGrid(favApps);
    }

    private void populateGrid(List<ApplicationInfo> apps) {
        appsGrid.getChildren().clear();
        com.launchhub.repository.SettingsRepository settingsRepo = new com.launchhub.repository.SettingsRepository();
        String gridSize = settingsRepo.get("grid_size", "Medium");
        List<String> categories = appRepository.getCategoriesList();

        for (ApplicationInfo app : apps) {
            AppCard card = new AppCard(app, appRepository, this::showAppDetails, this::loadFavorites, gridSize, categories);
            appsGrid.getChildren().add(card);
        }
    }

    private void filterApps(String query) {
        if (query == null || query.isEmpty()) {
            populateGrid(favApps);
            return;
        }
        String lower = query.toLowerCase();
        List<ApplicationInfo> filtered = favApps.stream()
                .filter(app -> app.getName().toLowerCase().contains(lower) 
                        || (app.getPublisher() != null && app.getPublisher().toLowerCase().contains(lower)))
                .toList();
        populateGrid(filtered);
    }

    private void showAppDetails(ApplicationInfo app) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(app.getName() + " - Details");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.getStyleClass().add("dialog-container");
        layout.setStyle("-fx-background-color: #151518;");

        // App Icon
        ImageView iconView = new ImageView();
        iconView.setFitWidth(80);
        iconView.setFitHeight(80);
        if (app.getIconPath() != null && new File(app.getIconPath()).exists()) {
            iconView.setImage(new Image(new File(app.getIconPath()).toURI().toString()));
        } else {
            iconView.setImage(new Image(getClass().getResourceAsStream("/com/launchhub/css/default_app.png")));
        }

        Label title = new Label(app.getName());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        GridPane details = new GridPane();
        details.setHgap(15);
        details.setVgap(10);
        details.setAlignment(Pos.CENTER);

        String[][] fields = {
            {"Publisher:", app.getPublisher() != null ? app.getPublisher() : "N/A"},
            {"Version:", app.getVersion() != null ? app.getVersion() : "N/A"},
            {"Location:", app.getInstallLocation() != null ? app.getInstallLocation() : "N/A"},
            {"Category:", app.getCategory()},
            {"Launches:", String.valueOf(app.getLaunchCount())},
            {"Last Used:", app.getLastUsed() != null ? app.getLastUsed() : "Never"},
            {"File Size:", app.getSizeBytes() > 0 ? (app.getSizeBytes() / (1024 * 1024)) + " MB" : "N/A"}
        };

        for (int i = 0; i < fields.length; i++) {
            Label label = new Label(fields[i][0]);
            label.setStyle("-fx-text-fill: #a1a1aa; -fx-font-weight: bold;");
            Label val = new Label(fields[i][1]);
            val.setStyle("-fx-text-fill: #e4e4e7;");
            val.setWrapText(true);
            val.setMaxWidth(300);
            details.add(label, 0, i);
            details.add(val, 1, i);
        }

        Button btnClose = new Button("Close");
        btnClose.getStyleClass().add("button-secondary");
        btnClose.setOnAction(e -> dialog.close());

        layout.getChildren().addAll(iconView, title, details, btnClose);

        Scene scene = new Scene(layout);
        scene.getStylesheets().addAll(searchField.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
