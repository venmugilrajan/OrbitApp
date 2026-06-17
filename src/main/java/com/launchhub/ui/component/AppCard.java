package com.launchhub.ui.component;

import com.launchhub.model.ApplicationInfo;
import com.launchhub.repository.ApplicationRepository;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class AppCard extends VBox {

    private final ApplicationInfo appInfo;
    private final ApplicationRepository repository;
    private final Consumer<ApplicationInfo> onDetailsRequested;
    private final Runnable onFavoriteChanged;
    private final String gridSize;
    private final java.util.List<String> categories;

    public AppCard(ApplicationInfo appInfo, ApplicationRepository repository,
                   Consumer<ApplicationInfo> onDetailsRequested, Runnable onFavoriteChanged,
                   String gridSize, java.util.List<String> categories) {
        this.appInfo = appInfo;
        this.repository = repository;
        this.onDetailsRequested = onDetailsRequested;
        this.onFavoriteChanged = onFavoriteChanged;
        this.gridSize = gridSize;
        this.categories = categories;

        initUI();
    }

    private void initUI() {
        getStyleClass().add("app-card");

        String sizeSetting = this.gridSize != null ? this.gridSize : "Medium";

        int width = 180;
        int height = 220;
        int iconSize = 64;

        if ("Small".equalsIgnoreCase(sizeSetting)) {
            width = 130;
            height = 170;
            iconSize = 48;
        } else if ("Large".equalsIgnoreCase(sizeSetting)) {
            width = 230;
            height = 270;
            iconSize = 96;
        }

        setPrefWidth(width);
        setMinWidth(width);
        setMaxWidth(width);
        setPrefHeight(height);
        setMinHeight(height);
        setMaxHeight(height);

        setPadding(new Insets(10));
        setSpacing(8);
        setAlignment(Pos.CENTER);

        // Icon
        ImageView iconView = new ImageView();
        iconView.setFitWidth(iconSize);
        iconView.setFitHeight(iconSize);
        iconView.setPreserveRatio(true);

        if (appInfo.getIconPath() != null && new File(appInfo.getIconPath()).exists()) {
            iconView.setImage(new Image(new File(appInfo.getIconPath()).toURI().toString()));
        } else {
            // Default generic fallback (we can resolve it or use system default)
            iconView.setImage(new Image(getClass().getResourceAsStream("/com/launchhub/css/default_app.png")));
        }

        // Title
        Label titleLabel = new Label(appInfo.getName());
        titleLabel.getStyleClass().add("app-card-title");
        titleLabel.setWrapText(false);
        titleLabel.setMaxWidth(width - 30);

        // Publisher
        String pub = appInfo.getPublisher();
        if (pub == null || pub.isEmpty()) {
            pub = "Unknown Publisher";
        }
        Label pubLabel = new Label(pub);
        pubLabel.getStyleClass().add("app-card-publisher");
        pubLabel.setMaxWidth(width - 30);

        // Category Badge
        Label catBadge = new Label(appInfo.getCategory());
        catBadge.getStyleClass().add("app-card-category");

        getChildren().addAll(iconView, titleLabel, pubLabel, catBadge);

        // Hover scale animation
        ScaleTransition hoverAnim = new ScaleTransition(Duration.millis(120), this);
        setOnMouseEntered(e -> {
            hoverAnim.setToX(1.04);
            hoverAnim.setToY(1.04);
            hoverAnim.playFromStart();
        });
        setOnMouseExited(e -> {
            hoverAnim.setToX(1.0);
            hoverAnim.setToY(1.0);
            hoverAnim.playFromStart();
        });

        // Double click to launch
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                launchApplication();
            }
        });

        // Context menu
        ContextMenu contextMenu = createContextMenu();
        setOnContextMenuRequested(e -> contextMenu.show(this, e.getScreenX(), e.getScreenY()));
    }

    private ContextMenu createContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e -> launchApplication());

        MenuItem folderItem = new MenuItem("Open Installation Folder");
        folderItem.setOnAction(e -> openFolder());
        if (appInfo.getInstallLocation() == null || appInfo.getInstallLocation().isEmpty()) {
            folderItem.setDisable(true);
        }

        MenuItem detailsItem = new MenuItem("View Details");
        detailsItem.setOnAction(e -> onDetailsRequested.accept(appInfo));

        String favText = appInfo.isFavorite() ? "Remove from Favorites" : "Add to Favorites";
        MenuItem favoriteItem = new MenuItem(favText);
        favoriteItem.setOnAction(e -> toggleFavorite());

        // Dynamic Change Category Submenu
        Menu changeCategoryMenu = new Menu("Change Category");
        if (this.categories != null) {
            for (String cat : this.categories) {
                MenuItem catItem = new MenuItem(cat);
                catItem.setOnAction(e -> {
                    appInfo.setCategory(cat);
                    repository.update(appInfo);
                    if (onFavoriteChanged != null) {
                        onFavoriteChanged.run();
                    }
                });
                changeCategoryMenu.getItems().add(catItem);
            }
        }

        MenuItem removeItem = new MenuItem("Remove from Dashboard");
        removeItem.setOnAction(e -> {
            repository.delete(appInfo.getId());
            if (onFavoriteChanged != null) {
                onFavoriteChanged.run();
            }
        });

        menu.getItems().addAll(openItem, folderItem, detailsItem, favoriteItem, changeCategoryMenu, removeItem);
        return menu;
    }

    private void launchApplication() {
        new Thread(() -> {
            try {
                String exe = appInfo.getExecutablePath();
                if (exe != null) {
                    if (exe.startsWith("shell:AppsFolder\\")) {
                        // Update SQLite stats
                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        repository.recordLaunch(appInfo.getId(), timestamp);
                        appInfo.setLaunchCount(appInfo.getLaunchCount() + 1);
                        appInfo.setLastUsed(timestamp);

                        ProcessBuilder pb = new ProcessBuilder("explorer.exe", exe);
                        pb.start();
                    } else if (new File(exe).exists()) {
                        // Update SQLite stats
                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        repository.recordLaunch(appInfo.getId(), timestamp);
                        appInfo.setLaunchCount(appInfo.getLaunchCount() + 1);
                        appInfo.setLastUsed(timestamp);

                        // Launch Process
                        if (exe.toLowerCase().endsWith(".lnk")) {
                            Desktop.getDesktop().open(new File(exe));
                        } else {
                            ProcessBuilder pb = new ProcessBuilder(exe);
                            pb.directory(new File(exe).getParentFile());
                            pb.start();
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void openFolder() {
        try {
            String path = appInfo.getInstallLocation();
            if (path != null) {
                File dir = new File(path.replace("\"", "").trim());
                if (dir.exists()) {
                    Desktop.getDesktop().open(dir);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toggleFavorite() {
        boolean newVal = !appInfo.isFavorite();
        appInfo.setFavorite(newVal);
        repository.setFavorite(appInfo.getId(), newVal);
        if (onFavoriteChanged != null) {
            onFavoriteChanged.run();
        }
    }
}
