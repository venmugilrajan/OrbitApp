package com.launchhub.ui.component;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MetricCard extends VBox {

    public MetricCard(String title, String value) {
        initUI(title, value, null);
    }

    public MetricCard(String title, String value, String subtitle) {
        initUI(title, value, subtitle);
    }

    private void initUI(String title, String value, String subtitle) {
        getStyleClass().add("metric-card");
        setPadding(new Insets(20));
        setSpacing(10);
        setPrefWidth(220);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("metric-value");

        getChildren().addAll(titleLabel, valueLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            Label subLabel = new Label(subtitle);
            subLabel.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-size: 11px;");
            getChildren().add(subLabel);
        }
    }
}
