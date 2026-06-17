package com.launchhub.ui.controller;

import com.launchhub.model.ApplicationInfo;
import com.launchhub.repository.ApplicationRepository;
import com.launchhub.ui.component.MetricCard;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class StatisticsController {

    @FXML private GridPane metricsGrid;
    @FXML private TableView<ApplicationInfo> popularAppsTable;
    @FXML private TableColumn<ApplicationInfo, String> colPopName;
    @FXML private TableColumn<ApplicationInfo, String> colPopLaunches;
    @FXML private TableColumn<ApplicationInfo, String> colPopLastUsed;

    @FXML private ComboBox<String> comboDays;
    @FXML private TableView<ApplicationInfo> unusedAppsTable;
    @FXML private TableColumn<ApplicationInfo, String> colUnusedName;
    @FXML private TableColumn<ApplicationInfo, String> colUnusedSize;
    @FXML private TableColumn<ApplicationInfo, String> colUnusedLastUsed;

    private ApplicationRepository appRepository;

    public void init(ApplicationRepository appRepository) {
        this.appRepository = appRepository;

        setupTables();
        loadStats();
    }

    private void setupTables() {
        colPopName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        colPopLaunches.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getLaunchCount())));
        colPopLastUsed.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getLastUsed() != null ? cellData.getValue().getLastUsed() : "Never"));

        colUnusedName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        colUnusedSize.setCellValueFactory(cellData -> {
            long size = cellData.getValue().getSizeBytes();
            return new SimpleStringProperty(size > 0 ? (size / (1024 * 1024)) + " MB" : "N/A");
        });
        colUnusedLastUsed.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getLastUsed() != null ? cellData.getValue().getLastUsed() : "Never"));

        comboDays.setItems(FXCollections.observableArrayList("30 Days", "60 Days", "90 Days"));
        comboDays.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int days = Integer.parseInt(newVal.split(" ")[0]);
                loadUnusedApps(days);
            }
        });
        comboDays.getSelectionModel().select(0);
    }

    private void loadStats() {
        List<ApplicationInfo> apps = appRepository.getAllApplications();
        if (apps.isEmpty()) return;

        // Metric calculations
        int totalApps = apps.size();
        long totalSize = apps.stream().mapToLong(ApplicationInfo::getSizeBytes).sum();
        String totalSizeStr = totalSize > (1024L * 1024L * 1024L) 
                ? String.format("%.2f GB", (double) totalSize / (1024 * 1024 * 1024))
                : (totalSize / (1024 * 1024)) + " MB";

        // Category counts
        Map<String, Integer> categoryCounts = appRepository.getCategoryCounts();
        String mainCategory = categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        // Most and least used
        ApplicationInfo mostUsed = apps.stream()
                .max(Comparator.comparingInt(ApplicationInfo::getLaunchCount))
                .orElse(null);

        String mostUsedName = (mostUsed != null && mostUsed.getLaunchCount() > 0) 
                ? mostUsed.getName() + " (" + mostUsed.getLaunchCount() + ")" 
                : "None";

        metricsGrid.getChildren().clear();
        metricsGrid.add(new MetricCard("Total Installed Apps", String.valueOf(totalApps)), 0, 0);
        metricsGrid.add(new MetricCard("Disk Space Consumed", totalSizeStr), 1, 0);
        metricsGrid.add(new MetricCard("Most Popular App", mostUsedName), 2, 0);
        metricsGrid.add(new MetricCard("Top Category", mainCategory), 3, 0);

        // Populate Popular Apps
        List<ApplicationInfo> popular = apps.stream()
                .filter(a -> a.getLaunchCount() > 0)
                .sorted(Comparator.comparingInt(ApplicationInfo::getLaunchCount).reversed())
                .limit(10)
                .toList();
        popularAppsTable.setItems(FXCollections.observableArrayList(popular));
    }

    private void loadUnusedApps(int days) {
        List<ApplicationInfo> unused = appRepository.getUnusedApplications(days);
        unusedAppsTable.setItems(FXCollections.observableArrayList(unused));
    }
}
