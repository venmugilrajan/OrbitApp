package com.launchhub.ui.controller;

import com.launchhub.model.ApplicationInfo;
import com.launchhub.repository.ApplicationRepository;
import com.launchhub.service.ScannerService;
import com.launchhub.ui.component.AppCard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML private TextField searchField;
    @FXML private Button btnClearSearch;
    @FXML private Button btnAddApp;
    @FXML private Button btnScan;
    @FXML private FlowPane appsGrid;

    private ApplicationRepository appRepository;
    private ScannerService scannerService;
    private MainController mainController;
    private List<ApplicationInfo> allApps = new ArrayList<>();

    public void init(ApplicationRepository appRepository, ScannerService scannerService, MainController mainController) {
        this.appRepository = appRepository;
        this.scannerService = scannerService;
        this.mainController = mainController;

        setupActions();
        loadApps();
    }

    private void setupActions() {
        btnClearSearch.setOnAction(e -> searchField.clear());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            btnClearSearch.setVisible(newValue != null && !newValue.isEmpty());
            filterApps(newValue);
        });
        
        btnScan.setOnAction(e -> {
            btnScan.setText("Scanning...");
            btnScan.setDisable(true);
            new Thread(() -> {
                scannerService.performScan();
                Platform.runLater(() -> {
                    btnScan.setText("Scan Library");
                    btnScan.setDisable(false);
                    loadApps();
                });
            }).start();
        });

        btnAddApp.setOnAction(e -> showAddAppDialog());
    }

    private void showAddAppDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Application Manually");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setStyle("-fx-background-color: #151518;");
        grid.getStyleClass().add("dialog-container");

        TextField nameField = new TextField();
        nameField.setPromptText("Application Name");

        TextField pathField = new TextField();
        pathField.setPromptText("Path to Executable (.exe)");
        pathField.setPrefWidth(250);

        Button btnBrowse = new Button("Browse...");
        btnBrowse.getStyleClass().add("button-secondary");
        btnBrowse.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Executable File");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Executable Files", "*.exe")
            );
            File selectedFile = fileChooser.showOpenDialog(dialog);
            if (selectedFile != null) {
                pathField.setText(selectedFile.getAbsolutePath());
                if (nameField.getText().isEmpty()) {
                    String baseName = selectedFile.getName();
                    if (baseName.toLowerCase().endsWith(".exe")) {
                        baseName = baseName.substring(0, baseName.length() - 4);
                    }
                    nameField.setText(baseName);
                }
            }
        });

        TextField pubField = new TextField();
        pubField.setPromptText("Publisher (Optional)");

        TextField verField = new TextField();
        verField.setPromptText("Version (Optional)");

        ComboBox<String> comboCat = new ComboBox<>();
        comboCat.setItems(javafx.collections.FXCollections.observableArrayList(appRepository.getCategoriesList()));
        comboCat.getSelectionModel().select("Others");

        // Row layout
        grid.add(new Label("Application Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Executable Path:"), 0, 1);
        HBox pathBox = new HBox(10, pathField, btnBrowse);
        grid.add(pathBox, 1, 1);

        grid.add(new Label("Publisher:"), 0, 2);
        grid.add(pubField, 1, 2);

        grid.add(new Label("Version:"), 0, 3);
        grid.add(verField, 1, 3);

        grid.add(new Label("Category:"), 0, 4);
        grid.add(comboCat, 1, 4);

        Button btnSave = new Button("Save");
        btnSave.getStyleClass().add("button-primary");
        btnSave.setOnAction(e -> {
            String name = nameField.getText().trim();
            String path = pathField.getText().trim();
            if (name.isEmpty() || path.isEmpty() || !new File(path).exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a valid name and executable path.");
                alert.showAndWait();
                return;
            }

            File file = new File(path);
            ApplicationInfo app = new ApplicationInfo();
            app.setName(name);
            app.setExecutablePath(path);
            app.setInstallLocation(file.getParent());
            app.setPublisher(pubField.getText().trim());
            app.setVersion(verField.getText().trim());
            app.setCategory(comboCat.getValue());
            app.setSizeBytes(file.length());
            app.setInstallDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));

            // Extract and save icon
            String iconDir = com.launchhub.repository.DatabaseManager.getDbDirectory() + File.separator + "icons";
            String iconPath = com.launchhub.util.IconHelper.extractAndSaveIcon(path, iconDir, name);
            if (iconPath != null) {
                app.setIconPath(iconPath);
            }

            appRepository.save(app);
            dialog.close();
            loadApps();
        });

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("button-secondary");
        btnCancel.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(15, btnSave, btnCancel);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(btnBox, 1, 5);

        // Styling bindings
        for (javafx.scene.Node node : grid.getChildren()) {
            if (node instanceof Label) {
                node.setStyle("-fx-text-fill: #a1a1aa; -fx-font-weight: bold;");
            }
        }

        Scene scene = new Scene(grid);
        scene.getStylesheets().addAll(appsGrid.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void loadApps() {
        allApps = appRepository.getAllApplications();
        populateGrid(allApps);
    }

    private void populateGrid(List<ApplicationInfo> apps) {
        appsGrid.getChildren().clear();
        com.launchhub.repository.SettingsRepository settingsRepo = new com.launchhub.repository.SettingsRepository();
        String gridSize = settingsRepo.get("grid_size", "Medium");
        List<String> categories = appRepository.getCategoriesList();

        for (ApplicationInfo app : apps) {
            AppCard card = new AppCard(app, appRepository, this::showAppDetails, this::loadApps, gridSize, categories);
            appsGrid.getChildren().add(card);
        }
    }

    private void filterApps(String query) {
        if (query == null || query.isEmpty()) {
            populateGrid(allApps);
            return;
        }
        String lower = query.toLowerCase();
        List<ApplicationInfo> filtered = allApps.stream()
                .filter(app -> app.getName().toLowerCase().contains(lower) 
                        || (app.getPublisher() != null && app.getPublisher().toLowerCase().contains(lower))
                        || (app.getCategory() != null && app.getCategory().toLowerCase().contains(lower)))
                .toList();
        populateGrid(filtered);
    }

    public void showAppDetails(ApplicationInfo app) {
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
        // Inherit styling
        scene.getStylesheets().addAll(searchField.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
