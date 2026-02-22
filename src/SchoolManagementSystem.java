import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SchoolManagementSystem extends Application {

    // Data structures
    private ObservableList<Student> studentData = FXCollections.observableArrayList();
    private FilteredList<Student> filteredData;

    // Dashboard components
    private Label totalStudentsLabel, activeStudentsLabel, inactiveStudentsLabel, avgGPALabel;
    private PieChart programmeChart;
    private BarChart<String, Number> levelChart;

    // Students screen components - REPLACED TABLE WITH BUTTON AND LISTVIEW
    private Button viewStudentsBtn;
    private ListView<Student> studentListView;
    private VBox studentListContainer;
    private boolean isListViewVisible = false;

    // Form components
    private TextField idField, nameField, progField, levelField, gpaField, emailField, phoneField;
    private ComboBox<String> statusCombo;
    private Button addBtn, editBtn, deleteBtn, refreshBtn, clearBtn;
    private Student currentlyEditingStudent = null;

    // Search and filter components
    private TextField searchField;
    private Button clearSearchBtn;
    private ComboBox<String> programmeFilter, levelFilter, statusFilter;

    // Reports screen components
    private ComboBox<String> reportTypeCombo;
    private TableView<ReportRow> reportTable;
    private VBox reportFiltersContainer;
    private DatePicker startDatePicker, endDatePicker;
    private ComboBox<String> reportProgrammeFilter, reportLevelFilter, reportStatusFilter;
    private Label reportTitleLabel;

    // Main components
    private TabPane tabPane;
    private Label statusLabel;

    // Quick stats label
    private Label statsLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("School Management System");

        // Load sample data
        loadSampleData();

        // Debug: Print loaded data to console
        System.out.println("=== STUDENT DATA LOADED ===");
        System.out.println("Total students: " + studentData.size());
        for (int i = 0; i < studentData.size(); i++) {
            Student s = studentData.get(i);
            System.out.println((i+1) + ". " + s.toString());
        }
        System.out.println("===========================");

        // Create main layout
        BorderPane mainLayout = new BorderPane();

        // Create top toolbar
        ToolBar topToolBar = createTopToolBar();
        mainLayout.setTop(topToolBar);

        // Create tab pane
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #f5f5f5;");

        // Create tabs
        Tab dashboardTab = createDashboardTab();
        Tab studentsTab = createStudentsTab();
        Tab reportsTab = createReportsTab();

        tabPane.getTabs().addAll(dashboardTab, studentsTab, reportsTab);
        mainLayout.setCenter(tabPane);

        // Create status bar
        HBox statusBar = createStatusBar();
        mainLayout.setBottom(statusBar);

        Scene scene = new Scene(mainLayout, 1300, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

        updateDashboard();
        updateCharts();
        updateStatsLabel();
        updateStatus("Application started successfully");
    }

    // ==================== TOP TOOLBAR ====================

    private ToolBar createTopToolBar() {
        ToolBar toolBar = new ToolBar();
        toolBar.setStyle("-fx-background-color: #2c3e50; -fx-padding: 5;");

        // App Title
        Label appTitle = new Label("📚 School Management System");
        appTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Separator
        Separator separator1 = new Separator(Orientation.VERTICAL);
        separator1.setStyle("-fx-background-color: white;");

        // Quick Stats
        statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        updateStatsLabel();

        // Add listener to update stats when data changes
        studentData.addListener((javafx.collections.ListChangeListener<Student>) change -> {
            updateStatsLabel();
        });

        // Spacer to push items to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right side buttons
        Button refreshAllBtn = new Button("🔄 Refresh All");
        refreshAllBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 5 15;");
        refreshAllBtn.setOnAction(e -> {
            updateDashboard();
            updateCharts();
            if (studentListView != null) {
                studentListView.setItems(filteredData);
            }
            updateStatsLabel();
            updateStatus("All data refreshed");
        });

        Button helpBtn = new Button("❓ Help");
        helpBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 5 15;");
        helpBtn.setOnAction(e -> showHelpDialog());

        toolBar.getItems().addAll(appTitle, separator1, statsLabel, spacer, refreshAllBtn, helpBtn);

        return toolBar;
    }

    private void updateStatsLabel() {
        if (statsLabel != null) {
            long active = studentData.stream().filter(s -> "Active".equals(s.getStatus())).count();
            long inactive = studentData.stream().filter(s -> "Inactive".equals(s.getStatus())).count();
            OptionalDouble avgGPA = studentData.stream().mapToDouble(Student::getGpa).average();

            statsLabel.setText(String.format("📊 Total: %d | Active: %d | Inactive: %d | Avg GPA: %.2f",
                    studentData.size(), active, inactive, avgGPA.orElse(0.0)));
        }
    }

    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("School Management System - Quick Guide");
        alert.setContentText(
                "📊 DASHBOARD: View statistics and charts\n\n" +
                        "👥 STUDENTS: Manage student records\n" +
                        "   • View Students: Click 'View Students List' button\n" +
                        "   • Add: Fill form and click Add\n" +
                        "   • Edit: Select student from list, modify, click Update\n" +
                        "   • Delete: Select student from list, click Delete\n" +
                        "   • Search: Type in search box to filter list\n\n" +
                        "📈 REPORTS: Generate various reports\n" +
                        "   • Select report type\n" +
                        "   • Apply filters\n" +
                        "   • Click Generate\n" +
                        "   • Export to CSV"
        );
        alert.showAndWait();
    }

    // ==================== DASHBOARD TAB ====================

    private Tab createDashboardTab() {
        Tab tab = new Tab("📊 Dashboard");
        tab.setContent(createDashboardContent());
        return tab;
    }

    private ScrollPane createDashboardContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox dashboard = new VBox(25);
        dashboard.setPadding(new Insets(25));
        dashboard.setStyle("-fx-background-color: #f5f5f5;");

        // Welcome Header
        Label welcomeLabel = new Label("Student Management Dashboard");
        welcomeLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        VBox headerBox = new VBox(5, welcomeLabel, dateLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Statistics Cards
        HBox statsCards = createStatisticsCards();

        // Charts Section
        Label chartsTitle = new Label("Student Analytics");
        chartsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #34495e; -fx-padding: 10 0 0 0;");

        HBox chartsBox = createCharts();

        // Quick Actions Section
        Label actionsTitle = new Label("Quick Actions");
        actionsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #34495e; -fx-padding: 10 0 0 0;");

        GridPane quickActionsGrid = createQuickActionButtons();

        dashboard.getChildren().addAll(headerBox, statsCards, chartsTitle, chartsBox,
                actionsTitle, quickActionsGrid);

        scrollPane.setContent(dashboard);
        return scrollPane;
    }

    private HBox createStatisticsCards() {
        HBox cards = new HBox(20);
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(20, 0, 20, 0));

        // Total Students Card
        VBox totalCard = createStatCard("Total Students",
                totalStudentsLabel = new Label("0"), "#3498db", "👥", "Total enrolled students");

        // Active Students Card
        VBox activeCard = createStatCard("Active Students",
                activeStudentsLabel = new Label("0"), "#2ecc71", "✅", "Currently active students");

        // Inactive Students Card
        VBox inactiveCard = createStatCard("Inactive Students",
                inactiveStudentsLabel = new Label("0"), "#e74c3c", "❌", "Inactive students");

        // Average GPA Card
        VBox gpaCard = createStatCard("Average GPA",
                avgGPALabel = new Label("0.00"), "#f39c12", "📊", "Average GPA across all students");

        cards.getChildren().addAll(totalCard, activeCard, inactiveCard, gpaCard);
        return cards;
    }

    private VBox createStatCard(String title, Label valueLabel, String color, String icon, String tooltip) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(250);
        card.setAlignment(Pos.CENTER);

        Tooltip.install(card, new Tooltip(tooltip));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 40px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(iconLabel, titleLabel, valueLabel);

        // Hover effect
        card.setOnMouseEntered(e ->
                card.setStyle(card.getStyle() + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 8);"));
        card.setOnMouseExited(e ->
                card.setStyle(card.getStyle().replace("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 8);",
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);")));

        return card;
    }

    private HBox createCharts() {
        HBox chartsBox = new HBox(20);
        chartsBox.setAlignment(Pos.CENTER);
        chartsBox.setPadding(new Insets(20, 0, 20, 0));

        // Programme Distribution Pie Chart
        VBox pieChartBox = new VBox(10);
        pieChartBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 15;");
        pieChartBox.setPrefWidth(450);

        Label pieTitle = new Label("Students by Programme");
        pieTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        programmeChart = new PieChart();
        programmeChart.setPrefHeight(300);
        programmeChart.setLabelsVisible(true);
        programmeChart.setLegendVisible(true);
        programmeChart.setAnimated(true);

        pieChartBox.getChildren().addAll(pieTitle, programmeChart);

        // Level Distribution Bar Chart
        VBox barChartBox = new VBox(10);
        barChartBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 15;");
        barChartBox.setPrefWidth(450);

        Label barTitle = new Label("Students by Level");
        barTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Students");

        levelChart = new BarChart<>(xAxis, yAxis);
        levelChart.setPrefHeight(300);
        levelChart.setAnimated(true);
        levelChart.setLegendVisible(false);

        barChartBox.getChildren().addAll(barTitle, levelChart);

        chartsBox.getChildren().addAll(pieChartBox, barChartBox);
        return chartsBox;
    }

    private GridPane createQuickActionButtons() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 15;");

        // Row 1
        Button studentsBtn = createQuickActionButton("📋 Students",
                "Manage student records", "#3498db", e -> tabPane.getSelectionModel().select(1));

        Button reportsBtn = createQuickActionButton("📊 Reports",
                "View and generate reports", "#2ecc71", e -> tabPane.getSelectionModel().select(2));

        Button importBtn = createQuickActionButton("📥 Import",
                "Import data from CSV", "#f39c12", e -> importData());

        // Row 2
        Button exportBtn = createQuickActionButton("📤 Export",
                "Export data to CSV", "#9b59b6", e -> exportData());

        Button settingsBtn = createQuickActionButton("⚙ Settings",
                "Configure application settings", "#34495e", e -> showSettings());

        Button refreshBtn = createQuickActionButton("🔄 Refresh",
                "Refresh dashboard data", "#e67e22", e -> {
                    updateDashboard();
                    updateCharts();
                    if (studentListView != null) {
                        studentListView.setItems(filteredData);
                    }
                    updateStatsLabel();
                });

        // Add buttons to grid
        grid.add(studentsBtn, 0, 0);
        grid.add(reportsBtn, 1, 0);
        grid.add(importBtn, 2, 0);
        grid.add(exportBtn, 0, 1);
        grid.add(settingsBtn, 1, 1);
        grid.add(refreshBtn, 2, 1);

        return grid;
    }

    private Button createQuickActionButton(String text, String tooltip, String color,
                                           javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 15 25; -fx-background-radius: 8; " +
                "-fx-font-size: 14px; -fx-min-width: 150;");
        btn.setTooltip(new Tooltip(tooltip));

        // Hover effect
        btn.setOnMouseEntered(e ->
                btn.setStyle(btn.getStyle() + "-fx-opacity: 0.9; -fx-cursor: hand;"));
        btn.setOnMouseExited(e ->
                btn.setStyle(btn.getStyle().replace("-fx-opacity: 0.9; -fx-cursor: hand;", "")));

        btn.setOnAction(handler);
        return btn;
    }

    // ==================== STUDENTS TAB (with View Students Button) ====================

    private Tab createStudentsTab() {
        Tab tab = new Tab("👥 Students");

        // Create main content
        BorderPane mainPane = new BorderPane();
        mainPane.setPadding(new Insets(10));
        mainPane.setStyle("-fx-background-color: #f5f5f5;");

        // Top: View Students Button and Search/Filters
        VBox topSection = createTopSection();
        mainPane.setTop(topSection);

        // Center: Split Pane with Student List (hidden by default) and Form
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.6);

        // Left: Student List Container (initially empty)
        studentListContainer = new VBox(10);
        studentListContainer.setPadding(new Insets(5));
        studentListContainer.setVisible(false);
        studentListContainer.setManaged(false);

        Label listLabel = new Label("📋 Student Records");
        listLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Create the student list view
        studentListView = createStudentListView();

        studentListContainer.getChildren().addAll(listLabel, studentListView);

        // Right: Form
        VBox formBox = createStudentForm();

        splitPane.getItems().addAll(studentListContainer, formBox);
        mainPane.setCenter(splitPane);

        tab.setContent(mainPane);
        return tab;
    }

    private VBox createTopSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: white; -fx-border-color: #ddd; " +
                "-fx-border-radius: 5; -fx-background-radius: 5;");

        // View Students Button
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        viewStudentsBtn = new Button("📋 View Students List");
        viewStudentsBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 10 20; -fx-font-size: 14px; " +
                "-fx-background-radius: 5;");
        viewStudentsBtn.setTooltip(new Tooltip("Click to show/hide student list"));
        viewStudentsBtn.setOnAction(e -> toggleStudentListView());

        Label infoLabel = new Label("(Click button to display all students)");
        infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        buttonBox.getChildren().addAll(viewStudentsBtn, infoLabel);

        // Search bar
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("🔍 Search:");
        searchLabel.setStyle("-fx-font-weight: bold;");

        searchField = new TextField();
        searchField.setPromptText("Search by ID, Name, Email, or Programme...");
        searchField.setPrefWidth(400);
        searchField.setStyle("-fx-padding: 8; -fx-background-radius: 5;");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredData != null) {
                updateFilter();
            }
        });
        searchField.setDisable(true); // Disabled until list is visible

        clearSearchBtn = new Button("Clear");
        clearSearchBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 5 10;");
        clearSearchBtn.setOnAction(e -> {
            searchField.clear();
            updateFilter();
        });
        clearSearchBtn.setDisable(true);

        searchBox.getChildren().addAll(searchLabel, searchField, clearSearchBtn);

        // Filters
        HBox filtersBox = new HBox(15);
        filtersBox.setAlignment(Pos.CENTER_LEFT);

        // Programme filter
        VBox progFilterBox = new VBox(3);
        progFilterBox.getChildren().addAll(
                new Label("Programme:"),
                programmeFilter = new ComboBox<>()
        );
        programmeFilter.getItems().addAll("All Programmes", "Computer Science", "Engineering",
                "Business", "Medicine", "Arts", "Law");
        programmeFilter.setValue("All Programmes");
        programmeFilter.setPrefWidth(150);
        programmeFilter.setOnAction(e -> updateFilter());
        programmeFilter.setDisable(true);

        // Level filter
        VBox levelFilterBox = new VBox(3);
        levelFilterBox.getChildren().addAll(
                new Label("Level:"),
                levelFilter = new ComboBox<>()
        );
        levelFilter.getItems().addAll("All Levels", "100", "200", "300", "400", "500");
        levelFilter.setValue("All Levels");
        levelFilter.setPrefWidth(100);
        levelFilter.setOnAction(e -> updateFilter());
        levelFilter.setDisable(true);

        // Status filter
        VBox statusFilterBox = new VBox(3);
        statusFilterBox.getChildren().addAll(
                new Label("Status:"),
                statusFilter = new ComboBox<>()
        );
        statusFilter.getItems().addAll("All Status", "Active", "Inactive");
        statusFilter.setValue("All Status");
        statusFilter.setPrefWidth(100);
        statusFilter.setOnAction(e -> updateFilter());
        statusFilter.setDisable(true);

        filtersBox.getChildren().addAll(progFilterBox, levelFilterBox, statusFilterBox);

        section.getChildren().addAll(buttonBox, searchBox, filtersBox);
        return section;
    }

    private void toggleStudentListView() {
        isListViewVisible = !isListViewVisible;

        if (isListViewVisible) {
            // Show the list
            studentListContainer.setVisible(true);
            studentListContainer.setManaged(true);
            viewStudentsBtn.setText("📋 Hide Students List");
            viewStudentsBtn.setStyle(viewStudentsBtn.getStyle().replace("#3498db", "#e74c3c"));

            // Enable search and filters
            searchField.setDisable(false);
            clearSearchBtn.setDisable(false);
            programmeFilter.setDisable(false);
            levelFilter.setDisable(false);
            statusFilter.setDisable(false);

            // Update the list
            updateStudentListView();
        } else {
            // Hide the list
            studentListContainer.setVisible(false);
            studentListContainer.setManaged(false);
            viewStudentsBtn.setText("📋 View Students List");
            viewStudentsBtn.setStyle(viewStudentsBtn.getStyle().replace("#e74c3c", "#3498db"));

            // Disable search and filters
            searchField.setDisable(true);
            clearSearchBtn.setDisable(true);
            programmeFilter.setDisable(true);
            levelFilter.setDisable(true);
            statusFilter.setDisable(true);

            // Clear selection
            studentListView.getSelectionModel().clearSelection();
            clearForm();
        }
    }

    // Create Student ListView with custom cell rendering using toString() format
    private ListView<Student> createStudentListView() {
        ListView<Student> listView = new ListView<>();
        listView.setPrefWidth(500);
        listView.setPrefHeight(400);

        // Set up filtered data
        filteredData = new FilteredList<>(studentData, p -> true);
        listView.setItems(filteredData);

        // Custom cell factory to display student information using toString() format
        listView.setCellFactory(param -> new ListCell<Student>() {
            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);

                if (empty || student == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Create a custom HBox for each student item
                    HBox cellBox = new HBox(10);
                    cellBox.setPadding(new Insets(8));
                    cellBox.setAlignment(Pos.CENTER_LEFT);

                    // Status indicator (emoji)
                    Label statusIcon = new Label();
                    statusIcon.setStyle("-fx-font-size: 16px;");
                    if ("Active".equals(student.getStatus())) {
                        statusIcon.setText("✅");
                        statusIcon.setStyle(statusIcon.getStyle() + " -fx-text-fill: #27ae60;");
                    } else {
                        statusIcon.setText("❌");
                        statusIcon.setStyle(statusIcon.getStyle() + " -fx-text-fill: #e74c3c;");
                    }

                    // Student details using the exact toString() format
                    Label studentLabel = new Label(student.toString());
                    studentLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

                    // Color code based on GPA
                    if (student.getGpa() >= 3.5) {
                        studentLabel.setStyle(studentLabel.getStyle() + " -fx-text-fill: #27ae60;");
                    } else if (student.getGpa() >= 2.5) {
                        studentLabel.setStyle(studentLabel.getStyle() + " -fx-text-fill: #f39c12;");
                    } else {
                        studentLabel.setStyle(studentLabel.getStyle() + " -fx-text-fill: #e74c3c;");
                    }

                    cellBox.getChildren().addAll(statusIcon, studentLabel);

                    // Add hover effect
                    cellBox.setOnMouseEntered(e ->
                            cellBox.setStyle("-fx-background-color: #ecf0f1; -fx-cursor: hand;"));
                    cellBox.setOnMouseExited(e ->
                            cellBox.setStyle("-fx-background-color: transparent;"));

                    setGraphic(cellBox);
                }
            }
        });

        // Add selection listener
        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        loadStudentToForm(newSelection);
                    }
                });

        return listView;
    }

    private void updateStudentListView() {
        if (studentListView != null) {
            studentListView.setItems(filteredData);
            studentListView.refresh();
        }
    }

    private VBox createStudentForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(15));
        form.setPrefWidth(400);
        form.setStyle("-fx-background-color: white; -fx-border-color: #ddd; " +
                "-fx-border-radius: 5; -fx-background-radius: 5;");

        Label titleLabel = new Label("Student Information");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Form fields
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Initialize fields
        idField = new TextField();
        idField.setPromptText("Enter unique ID");
        idField.setPrefWidth(250);

        nameField = new TextField();
        nameField.setPromptText("Enter full name");

        progField = new TextField();
        progField.setPromptText("Enter programme");

        levelField = new TextField();
        levelField.setPromptText("e.g., 300");

        gpaField = new TextField();
        gpaField.setPromptText("0.0 - 4.0");

        emailField = new TextField();
        emailField.setPromptText("email@example.com");

        phoneField = new TextField();
        phoneField.setPromptText("Phone number");

        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Active", "Inactive");
        statusCombo.setValue("Active");
        statusCombo.setPrefWidth(250);

        // Add fields to grid with labels
        int row = 0;
        grid.add(new Label("ID:*"), 0, row);
        grid.add(idField, 1, row++);

        grid.add(new Label("Name:*"), 0, row);
        grid.add(nameField, 1, row++);

        grid.add(new Label("Programme:*"), 0, row);
        grid.add(progField, 1, row++);

        grid.add(new Label("Level:*"), 0, row);
        grid.add(levelField, 1, row++);

        grid.add(new Label("GPA:*"), 0, row);
        grid.add(gpaField, 1, row++);

        grid.add(new Label("Email:*"), 0, row);
        grid.add(emailField, 1, row++);

        grid.add(new Label("Phone:"), 0, row);
        grid.add(phoneField, 1, row++);

        grid.add(new Label("Status:"), 0, row);
        grid.add(statusCombo, 1, row++);

        // Required fields note
        Label requiredNote = new Label("* Required fields");
        requiredNote.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        // Buttons
        HBox buttonBox1 = new HBox(10);
        buttonBox1.setAlignment(Pos.CENTER);

        addBtn = new Button("➕ Add Student");
        addBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        editBtn = new Button("✏ Update");
        editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        deleteBtn = new Button("🗑 Delete");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        buttonBox1.getChildren().addAll(addBtn, editBtn, deleteBtn);

        HBox buttonBox2 = new HBox(10);
        buttonBox2.setAlignment(Pos.CENTER);

        clearBtn = new Button("🧹 Clear");
        clearBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        buttonBox2.getChildren().addAll(clearBtn, refreshBtn);

        // Set button actions
        addBtn.setOnAction(e -> addStudent());
        editBtn.setOnAction(e -> updateStudent());
        deleteBtn.setOnAction(e -> deleteStudent());
        clearBtn.setOnAction(e -> clearForm());
        refreshBtn.setOnAction(e -> {
            if (studentListView != null) {
                studentListView.refresh();
            }
            updateFilter();
        });

        form.getChildren().addAll(titleLabel, grid, requiredNote, buttonBox1, buttonBox2);
        return form;
    }

    // ==================== REPORTS TAB ====================

    private Tab createReportsTab() {
        Tab tab = new Tab("📈 Reports");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: #f5f5f5;");

        // Top: Report Selection and Filters
        VBox topSection = createReportControls();
        content.getChildren().add(topSection);

        // Center: Report Results
        VBox centerSection = createReportResults();
        content.getChildren().add(centerSection);

        // Bottom: Export Button
        HBox bottomSection = new HBox(10);
        bottomSection.setPadding(new Insets(10));
        bottomSection.setAlignment(Pos.CENTER_RIGHT);

        Button exportReportBtn = new Button("📤 Export Report");
        exportReportBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        exportReportBtn.setOnAction(e -> exportReport());

        bottomSection.getChildren().add(exportReportBtn);
        content.getChildren().add(bottomSection);

        tab.setContent(content);
        return tab;
    }

    private VBox createReportControls() {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: white; -fx-border-color: #ddd; " +
                "-fx-border-radius: 5; -fx-background-radius: 5;");

        // Title
        reportTitleLabel = new Label("Generate Reports");
        reportTitleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Report type selection
        HBox typeBox = new HBox(10);
        typeBox.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label("Report Type:");
        typeLabel.setStyle("-fx-font-weight: bold;");

        reportTypeCombo = new ComboBox<>();
        reportTypeCombo.getItems().addAll(
                "📋 Student List by Programme",
                "📋 Student List by Level",
                "📊 GPA Distribution",
                "✅ Active/Inactive Students",
                "📅 Students Added This Month",
                "📈 Programme-wise Statistics",
                "📊 Level-wise Statistics",
                "📉 GPA Range Analysis"
        );
        reportTypeCombo.setValue("📋 Student List by Programme");
        reportTypeCombo.setPrefWidth(300);
        reportTypeCombo.setOnAction(e -> updateReportFilters());

        typeBox.getChildren().addAll(typeLabel, reportTypeCombo);

        // Filters container
        reportFiltersContainer = new VBox(10);
        reportFiltersContainer.setPadding(new Insets(10, 0, 0, 0));

        // Generate Report Button
        Button generateBtn = new Button("Generate Report");
        generateBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;");
        generateBtn.setOnAction(e -> generateReport());

        controls.getChildren().addAll(reportTitleLabel, typeBox, reportFiltersContainer, generateBtn);

        // Initialize filters
        updateReportFilters();

        return controls;
    }

    private void updateReportFilters() {
        reportFiltersContainer.getChildren().clear();
        String reportType = reportTypeCombo.getValue();

        if (reportType.contains("Programme")) {
            HBox filterBox = new HBox(10);
            filterBox.setAlignment(Pos.CENTER_LEFT);

            Label progLabel = new Label("Select Programme:");
            progLabel.setStyle("-fx-font-weight: bold;");

            reportProgrammeFilter = new ComboBox<>();
            reportProgrammeFilter.getItems().addAll("All", "Computer Science", "Engineering",
                    "Business", "Medicine", "Arts", "Law");
            reportProgrammeFilter.setValue("All");
            reportProgrammeFilter.setPrefWidth(200);

            filterBox.getChildren().addAll(progLabel, reportProgrammeFilter);
            reportFiltersContainer.getChildren().add(filterBox);

        } else if (reportType.contains("Level") && !reportType.contains("Level-wise")) {
            HBox filterBox = new HBox(10);
            filterBox.setAlignment(Pos.CENTER_LEFT);

            Label levelLabel = new Label("Select Level:");
            levelLabel.setStyle("-fx-font-weight: bold;");

            reportLevelFilter = new ComboBox<>();
            reportLevelFilter.getItems().addAll("All", "100", "200", "300", "400", "500");
            reportLevelFilter.setValue("All");
            reportLevelFilter.setPrefWidth(200);

            filterBox.getChildren().addAll(levelLabel, reportLevelFilter);
            reportFiltersContainer.getChildren().add(filterBox);

        } else if (reportType.contains("Date") || reportType.contains("Month")) {
            HBox filterBox1 = new HBox(10);
            filterBox1.setAlignment(Pos.CENTER_LEFT);

            Label startLabel = new Label("Start Date:");
            startLabel.setStyle("-fx-font-weight: bold;");
            startDatePicker = new DatePicker();
            startDatePicker.setValue(LocalDate.now().minusMonths(1));

            Label endLabel = new Label("End Date:");
            endLabel.setStyle("-fx-font-weight: bold;");
            endDatePicker = new DatePicker();
            endDatePicker.setValue(LocalDate.now());

            filterBox1.getChildren().addAll(startLabel, startDatePicker, endLabel, endDatePicker);
            reportFiltersContainer.getChildren().add(filterBox1);

        } else if (reportType.contains("Status")) {
            HBox filterBox = new HBox(10);
            filterBox.setAlignment(Pos.CENTER_LEFT);

            Label statusLabel = new Label("Select Status:");
            statusLabel.setStyle("-fx-font-weight: bold;");

            reportStatusFilter = new ComboBox<>();
            reportStatusFilter.getItems().addAll("All", "Active", "Inactive");
            reportStatusFilter.setValue("All");
            reportStatusFilter.setPrefWidth(200);

            filterBox.getChildren().addAll(statusLabel, reportStatusFilter);
            reportFiltersContainer.getChildren().add(filterBox);
        }
    }

    private VBox createReportResults() {
        VBox results = new VBox(10);
        results.setPadding(new Insets(10));
        results.setStyle("-fx-background-color: white; -fx-border-color: #ddd; " +
                "-fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 5 5; " +
                "-fx-background-radius: 0 0 5 5;");

        Label resultsLabel = new Label("Report Results");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        reportTable = createReportTable();

        results.getChildren().addAll(resultsLabel, reportTable);
        return results;
    }

    private TableView<ReportRow> createReportTable() {
        TableView<ReportRow> table = new TableView<>();
        table.setPrefHeight(250);

        TableColumn<ReportRow, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        categoryCol.setPrefWidth(300);

        TableColumn<ReportRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        valueCol.setPrefWidth(150);
        valueCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ReportRow, String> percentageCol = new TableColumn<>("Percentage");
        percentageCol.setCellValueFactory(cellData -> cellData.getValue().percentageProperty());
        percentageCol.setPrefWidth(150);
        percentageCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(categoryCol, valueCol, percentageCol);
        return table;
    }

    private void generateReport() {
        String reportType = reportTypeCombo.getValue();
        ObservableList<ReportRow> reportData = FXCollections.observableArrayList();

        switch (reportType) {
            case "📋 Student List by Programme":
                String programme = reportProgrammeFilter != null ?
                        reportProgrammeFilter.getValue() : "All";
                reportData = generateProgrammeReport(programme);
                break;

            case "📋 Student List by Level":
                String level = reportLevelFilter != null ?
                        reportLevelFilter.getValue() : "All";
                reportData = generateLevelReport(level);
                break;

            case "📊 GPA Distribution":
                reportData = generateGPADistribution();
                break;

            case "✅ Active/Inactive Students":
                String status = reportStatusFilter != null ?
                        reportStatusFilter.getValue() : "All";
                reportData = generateStatusReport(status);
                break;

            case "📅 Students Added This Month":
                LocalDate start = startDatePicker != null ? startDatePicker.getValue() : LocalDate.now().minusMonths(1);
                LocalDate end = endDatePicker != null ? endDatePicker.getValue() : LocalDate.now();
                reportData = generateDateRangeReport(start, end);
                break;

            case "📈 Programme-wise Statistics":
                reportData = generateProgrammeStatistics();
                break;

            case "📊 Level-wise Statistics":
                reportData = generateLevelStatistics();
                break;

            case "📉 GPA Range Analysis":
                reportData = generateGPARangeAnalysis();
                break;
        }

        reportTable.setItems(reportData);
        reportTitleLabel.setText("Report: " + reportType);
    }

    private ObservableList<ReportRow> generateProgrammeReport(String programme) {
        ObservableList<ReportRow> data = FXCollections.observableArrayList();

        Map<String, Long> counts;
        if ("All".equals(programme)) {
            counts = studentData.stream()
                    .collect(Collectors.groupingBy(Student::getProgramme, Collectors.counting()));
        } else {
            counts = new HashMap<>();
            counts.put(programme, studentData.stream()
                    .filter(s -> s.getProgramme().equals(programme))
                    .count());
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String prog = entry.getKey();
                    long count = entry.getValue();
                    double percentage = total > 0 ? (count * 100.0 / total) : 0;
                    data.add(new ReportRow(prog, String.valueOf(count), String.format("%.1f%%", percentage)));
                });

        if (total > 0) {
            data.add(new ReportRow("TOTAL", String.valueOf(total), "100%"));
        }

        return data;
    }

    private ObservableList<ReportRow> generateLevelReport(String level) {
        ObservableList<ReportRow> data = FXCollections.observableArrayList();

        Map<String, Long> counts;
        if ("All".equals(level)) {
            counts = studentData.stream()
                    .collect(Collectors.groupingBy(Student::getLevel,
                            () -> new TreeMap<>(Comparator.comparingInt(Integer::parseInt)),
                            Collectors.counting()));
        } else {
            counts = new TreeMap<>(Comparator.comparingInt(Integer::parseInt));
            counts.put(level, studentData.stream()
                    .filter(s -> s.getLevel().equals(level))
                    .count());
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        counts.forEach((lvl, count) -> {
            double percentage = total > 0 ? (count * 100.0 / total) : 0;
            data.add(new ReportRow("Level " + lvl, String.valueOf(count), String.format("%.1f%%", percentage)));
        });

        if (total > 0) {
            data.add(new ReportRow("TOTAL", String.valueOf(total), "100%"));
        }

        return data;
    }

    private ObservableList<ReportRow> generateGPADistribution() {
        ObservableList<ReportRow> data = FXCollections.observableArrayList();

        long excellent = studentData.stream().filter(s -> s.getGpa() >= 3.5).count();
        long good = studentData.stream().filter(s -> s.getGpa() >= 3.0 && s.getGpa() < 3.5).count();
        long average = studentData.stream().filter(s -> s.getGpa() >= 2.5 && s.getGpa() < 3.0).count();
        long fair = studentData.stream().filter(s -> s.getGpa() >= 2.0 && s.getGpa() < 2.5).count();
        long poor = studentData.stream().filter(s -> s.getGpa() < 2.0).count();

        long total = studentData.size();

        if (total > 0) {
            data.add(new ReportRow("Excellent (3.5 - 4.0)", String.valueOf(excellent),
                    String.format("%.1f%%", excellent * 100.0 / total)));
            data.add(new ReportRow("Good (3.0 - 3.49)", String.valueOf(good),
                    String.format("%.1f%%", good * 100.0 / total)));
            data.add(new ReportRow("Average (2.5 - 2.99)", String.valueOf(average),
                    String.format("%.1f%%", average * 100.0 / total)));
            data.add(new ReportRow("Fair (2.0 - 2.49)", String.valueOf(fair),
                    String.format("%.1f%%", fair * 100.0 / total)));
            data.add(new ReportRow("Poor (Below 2.0)", String.valueOf(poor),
                    String.format("%.1f%%", poor * 100.0 / total)));
            data.add(new ReportRow("TOTAL", String.valueOf(total), "100%"));
        }

        return data;
    }

    private ObservableList<ReportRow> generateStatusReport(String status) {
        ObservableList<ReportRow> data = FXCollections.observableArrayList();

        long total = studentData.size();

        if ("All".equals(status)) {
            long active = studentData.stream().filter(s -> "Active".equals(s.getStatus())).count();
            long inactive = studentData.stream().filter(s -> "Inactive".equals(s.getStatus())).count();

            data.add(new ReportRow("Active Students", String.valueOf(active),
                    String.format("%.1f%%", total > 0 ? active * 100.0 / total : 0)));
            data.add(new ReportRow("Inactive Students", String.valueOf(inactive),
                    String.format("%.1f%%", total > 0 ? inactive * 100.0 / total : 0)));
        } else {
            long count = studentData.stream().filter(s -> status.equals(s.getStatus())).count();
            data.add(new ReportRow(status + " Students", String.valueOf(count),
                    String.format("%.1f%%", total > 0 ? count * 100.0 / total : 0)));
        }

        if (total > 0) {
            data.add(new ReportRow("TOTAL", String.valueOf(total), "100%"));
        }

        return data;
    }

    private ObservableList<ReportRow> generateDateRangeReport(LocalDate start, LocalDate end) {
        ObservableList<ReportRow> data = FXCollections.observableArrayList();

        if (start == null) start = LocalDate.now().minusMonths(1);
        if (end == null) end = LocalDate.now();

        LocalDate finalStart = start;
        LocalDate finalEnd = end;

        Map<LocalDate, Long> dailyCounts = studentData.stream()
                .filter(s -> {
                    try {
                        LocalDate added = LocalDate.parse(s.getDateAdded().substring(0, 10));
                        return !added.isBefore(finalStart) && !added.isAfter(finalEnd);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(
                        s -> {
                            try {
                                return LocalDate.parse(s.getDateAdded().substring(0, 10));
                            } catch (Exception e) {
                                return LocalDate.now();
                            }
                        },
                        TreeMap::new,
                        Collectors.counting()
                ));

        long total = dailyCounts.values().stream().mapToLong(Long::longValue).sum();

        dailyCounts.forEach((date, count) ->
                data.add(new ReportRow(date.toString(), String.valueOf(count),
                        String.format("%.1f%%", total > 0 ? count * 100.0 / total : 0))));

        if (total > 0) {
            data.add(new ReportRow("TOTAL (" + finalStart + " to " + finalEnd + ")",
                    String.valueOf(total), "100%"));
        }

        return data;
    }

    private ObservableList<ReportRow> generateProgrammeStatistics() {
        ObservableList<ReportRow> data = FXCollections.observableArrayList();

        Map<String, DoubleSummaryStatistics> stats = studentData.stream()
                .collect(Collectors.groupingBy(
                        Student::getProgramme,
                        Collectors.summarizingDouble(Student::getGpa)
                ));

        stats.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String programme = entry.getKey();
                    DoubleSummaryStatistics stat = entry.getValue();
                    data.add(new ReportRow(programme + " - Count", String.valueOf((long) stat.getCount()), ""));
                    data.add(new ReportRow(programme + " - Avg GPA",
                            String.format("%.2f", stat.getAverage()), ""));
                    data.add(new ReportRow(programme + " - Max GPA",
                            String.format("%.2f", stat.getMax()), ""));
                    data.add(new ReportRow(programme + " - Min GPA",
                            String.format("%.2f", stat.getMin()), ""));
                });

        return data;
    }

    private ObservableList<ReportRow> generateLevelStatistics() {
        ObservableList<ReportRow> data = FXCollections.observableArrayList();

        Map<String, Long> counts = studentData.stream()
                .collect(Collectors.groupingBy(Student::getLevel,
                        () -> new TreeMap<>(Comparator.comparingInt(Integer::parseInt)),
                        Collectors.counting()));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        counts.forEach((level, count) -> {
            double percentage = total > 0 ? (count * 100.0 / total) : 0;
            data.add(new ReportRow("Level " + level, String.valueOf(count),
                    String.format("%.1f%%", percentage)));
        });

        return data;
    }

    private ObservableList<ReportRow> generateGPARangeAnalysis() {
        ObservableList<ReportRow> data = FXCollections.observableArrayList();

        double[] ranges = {4.0, 3.5, 3.0, 2.5, 2.0, 1.5, 1.0, 0.5, 0.0};
        long total = studentData.size();

        for (int i = 0; i < ranges.length - 1; i++) {
            double high = ranges[i];
            double low = ranges[i + 1];
            double finalLow = low;
            double finalHigh = high;
            long count = studentData.stream()
                    .filter(s -> s.getGpa() <= finalHigh && s.getGpa() > finalLow)
                    .count();

            String range = String.format("%.1f - %.1f", low, high);
            double percentage = total > 0 ? count * 100.0 / total : 0;
            data.add(new ReportRow(range, String.valueOf(count),
                    total > 0 ? String.format("%.1f%%", percentage) : ""));
        }

        return data;
    }

    // ==================== CRUD OPERATIONS ====================

    private void addStudent() {
        if (!validateForm()) return;

        // Check for duplicate ID
        boolean duplicate = studentData.stream()
                .anyMatch(s -> s.getStudentId().equalsIgnoreCase(idField.getText().trim()));

        if (duplicate) {
            showAlert(Alert.AlertType.ERROR, "Duplicate ID",
                    "Student ID already exists! Please use a unique ID.");
            return;
        }

        Student student = new Student(
                idField.getText().trim(),
                nameField.getText().trim(),
                progField.getText().trim(),
                levelField.getText().trim(),
                Double.parseDouble(gpaField.getText().trim()),
                emailField.getText().trim(),
                phoneField.getText().trim(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                statusCombo.getValue()
        );

        studentData.add(student);
        clearForm();
        updateDashboard();
        updateCharts();
        updateStatsLabel();
        updateStatus("Student added successfully!");

        // Update list view if visible
        if (studentListView != null) {
            studentListView.setItems(filteredData);
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Student added successfully!");
    }

    private void updateStudent() {
        if (currentlyEditingStudent == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a student to update.");
            return;
        }

        if (!validateForm()) return;

        currentlyEditingStudent.setStudentId(idField.getText().trim());
        currentlyEditingStudent.setFullName(nameField.getText().trim());
        currentlyEditingStudent.setProgramme(progField.getText().trim());
        currentlyEditingStudent.setLevel(levelField.getText().trim());
        currentlyEditingStudent.setGpa(Double.parseDouble(gpaField.getText().trim()));
        currentlyEditingStudent.setEmail(emailField.getText().trim());
        currentlyEditingStudent.setPhoneNumber(phoneField.getText().trim());
        currentlyEditingStudent.setStatus(statusCombo.getValue());

        if (studentListView != null) {
            studentListView.refresh();
        }
        clearForm();
        updateDashboard();
        updateCharts();
        updateStatsLabel();
        updateStatus("Student updated successfully!");

        showAlert(Alert.AlertType.INFORMATION, "Success", "Student updated successfully!");
    }

    private void deleteStudent() {
        Student selected = studentListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a student to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Student");
        confirm.setContentText("Are you sure you want to delete " + selected.getFullName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            studentData.remove(selected);
            clearForm();
            updateDashboard();
            updateCharts();
            updateStatsLabel();
            updateStatus("Student deleted successfully!");

            // Update list view
            if (studentListView != null) {
                studentListView.setItems(filteredData);
            }
        }
    }

    private void loadStudentToForm(Student student) {
        currentlyEditingStudent = student;

        idField.setText(student.getStudentId());
        nameField.setText(student.getFullName());
        progField.setText(student.getProgramme());
        levelField.setText(student.getLevel());
        gpaField.setText(String.valueOf(student.getGpa()));
        emailField.setText(student.getEmail());
        phoneField.setText(student.getPhoneNumber());
        statusCombo.setValue(student.getStatus());

        addBtn.setDisable(true);
        editBtn.setDisable(false);
        deleteBtn.setDisable(false);

        // Disable ID field when editing
        idField.setDisable(true);
    }

    private void clearForm() {
        idField.clear();
        nameField.clear();
        progField.clear();
        levelField.clear();
        gpaField.clear();
        emailField.clear();
        phoneField.clear();
        statusCombo.setValue("Active");

        currentlyEditingStudent = null;
        studentListView.getSelectionModel().clearSelection();

        addBtn.setDisable(false);
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        // Enable ID field
        idField.setDisable(false);
    }

    private boolean validateForm() {
        if (idField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Student ID is required!");
            return false;
        }

        if (nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Full Name is required!");
            return false;
        }

        if (progField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Programme is required!");
            return false;
        }

        if (levelField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Level is required!");
            return false;
        }

        if (gpaField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "GPA is required!");
            return false;
        }

        if (emailField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Email is required!");
            return false;
        }

        try {
            double gpa = Double.parseDouble(gpaField.getText().trim());
            if (gpa < 0 || gpa > 4.0) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "GPA must be between 0.0 and 4.0!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "GPA must be a valid number!");
            return false;
        }

        // Email validation (simple)
        String email = emailField.getText().trim();
        if (!email.contains("@") || !email.contains(".")) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid email address!");
            return false;
        }

        return true;
    }

    private void updateFilter() {
        if (filteredData == null) return;

        filteredData.setPredicate(student -> {
            // Search filter
            if (searchField != null && !searchField.getText().isEmpty()) {
                String searchTerm = searchField.getText().toLowerCase();
                boolean matches = student.getStudentId().toLowerCase().contains(searchTerm) ||
                        student.getFullName().toLowerCase().contains(searchTerm) ||
                        student.getEmail().toLowerCase().contains(searchTerm) ||
                        student.getProgramme().toLowerCase().contains(searchTerm);
                if (!matches) return false;
            }

            // Programme filter
            if (programmeFilter != null && !"All Programmes".equals(programmeFilter.getValue())) {
                if (!student.getProgramme().equals(programmeFilter.getValue())) {
                    return false;
                }
            }

            // Level filter
            if (levelFilter != null && !"All Levels".equals(levelFilter.getValue())) {
                if (!student.getLevel().equals(levelFilter.getValue())) {
                    return false;
                }
            }

            // Status filter
            if (statusFilter != null && !"All Status".equals(statusFilter.getValue())) {
                if (!student.getStatus().equals(statusFilter.getValue())) {
                    return false;
                }
            }

            return true;
        });

        // Update filter count
        long filteredCount = filteredData.size();
        updateStatus("Showing " + filteredCount + " of " + studentData.size() + " students");
    }

    // ==================== UTILITY METHODS ====================

    private void updateDashboard() {
        if (totalStudentsLabel != null) {
            int total = studentData.size();
            long active = studentData.stream().filter(s -> "Active".equals(s.getStatus())).count();
            long inactive = studentData.stream().filter(s -> "Inactive".equals(s.getStatus())).count();

            totalStudentsLabel.setText(String.valueOf(total));
            activeStudentsLabel.setText(String.valueOf(active));
            inactiveStudentsLabel.setText(String.valueOf(inactive));

            OptionalDouble avgGPA = studentData.stream().mapToDouble(Student::getGpa).average();
            avgGPALabel.setText(String.format("%.2f", avgGPA.orElse(0.0)));
        }
    }

    private void updateCharts() {
        if (programmeChart != null && levelChart != null) {
            // Update Programme Pie Chart
            Map<String, Long> programmeCounts = studentData.stream()
                    .collect(Collectors.groupingBy(Student::getProgramme, Collectors.counting()));

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            programmeCounts.forEach((prog, count) ->
                    pieData.add(new PieChart.Data(prog + " (" + count + ")", count)));
            programmeChart.setData(pieData);

            // Update Level Bar Chart
            Map<String, Long> levelCounts = studentData.stream()
                    .collect(Collectors.groupingBy(Student::getLevel,
                            () -> new TreeMap<>(Comparator.comparingInt(Integer::parseInt)),
                            Collectors.counting()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Students by Level");

            levelCounts.forEach((level, count) ->
                    series.getData().add(new XYChart.Data<>(level, count)));

            levelChart.getData().clear();
            levelChart.getData().add(series);
        }
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #34495e;");

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label();
        timeLabel.setStyle("-fx-text-fill: white;");
        timeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Update time every second
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            timeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        Label recordCountLabel = new Label();
        recordCountLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        recordCountLabel.setText("Total Records: " + studentData.size());

        // Update count when data changes
        studentData.addListener((javafx.collections.ListChangeListener<Student>) change -> {
            recordCountLabel.setText("Total Records: " + studentData.size());
        });

        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setStyle("-fx-background-color: white;");

        statusBar.getChildren().addAll(statusLabel, spacer, recordCountLabel, separator, timeLabel);
        return statusBar;
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(message));
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ==================== IMPORT/EXPORT ====================

    private void importData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Student Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int count = 0;
                int skipped = 0;

                // Read all lines
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    try {
                        Student student = parseCSVLine(line);
                        if (student != null) {
                            // Check for duplicate ID
                            boolean duplicate = studentData.stream()
                                    .anyMatch(s -> s.getStudentId().equalsIgnoreCase(student.getStudentId()));

                            if (!duplicate) {
                                studentData.add(student);
                                count++;
                            } else {
                                skipped++;
                            }
                        } else {
                            skipped++;
                        }
                    } catch (Exception e) {
                        skipped++;
                    }
                }

                updateDashboard();
                updateCharts();
                updateStatsLabel();

                if (studentListView != null) {
                    studentListView.setItems(filteredData);
                }

                String message = count + " students imported successfully.";
                if (skipped > 0) {
                    message += " " + skipped + " entries skipped (duplicates or errors).";
                }

                showAlert(Alert.AlertType.INFORMATION, "Import Successful", message);
                updateStatus(message);

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Import Error",
                        "Error importing file: " + e.getMessage());
            }
        }
    }

    private Student parseCSVLine(String line) {
        String[] data = line.split(",");
        if (data.length >= 8) {
            try {
                String id = data[0].trim();
                String name = data[1].trim();
                String programme = data[2].trim();
                String level = data[3].trim();
                double gpa = Double.parseDouble(data[4].trim());
                String email = data[5].trim();
                String phone = data[6].trim();
                String date = data.length > 7 ? data[7].trim() :
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String status = data.length > 8 ? data[8].trim() : "Active";

                // Validate status
                if (!status.equals("Active") && !status.equals("Inactive")) {
                    status = "Active";
                }

                return new Student(id, name, programme, level, gpa, email, phone, date, status);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void exportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Student Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("students_export_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Write header
                writer.write("ID,Name,Programme,Level,GPA,Email,Phone,Date Added,Status");
                writer.newLine();

                for (Student student : studentData) {
                    writer.write(student.toFileString());
                    writer.newLine();
                }

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        studentData.size() + " students exported successfully to:\n" + file.getAbsolutePath());

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Error",
                        "Error exporting file: " + e.getMessage());
            }
        }
    }

    private void exportReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Report");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("report_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("Report: " + reportTypeCombo.getValue());
                writer.newLine();
                writer.write("Generated: " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.newLine();
                writer.newLine();
                writer.write("Category,Value,Percentage");
                writer.newLine();

                for (ReportRow row : reportTable.getItems()) {
                    writer.write(row.getCategory() + "," +
                            row.getValue() + "," +
                            row.getPercentage());
                    writer.newLine();
                }

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Report exported successfully to:\n" + file.getAbsolutePath());

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Error",
                        "Error exporting report: " + e.getMessage());
            }
        }
    }

    private void showSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Application Settings");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setPrefWidth(400);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TitledPane generalPane = new TitledPane("General Settings", new VBox(10) {{
            setPadding(new Insets(10));
            CheckBox autoSaveCheck = new CheckBox("Auto-save on exit");
            CheckBox confirmDeleteCheck = new CheckBox("Confirm before delete");
            CheckBox showWelcomeCheck = new CheckBox("Show welcome message on startup");
            getChildren().addAll(autoSaveCheck, confirmDeleteCheck, showWelcomeCheck);
        }});
        generalPane.setExpanded(true);

        TitledPane displayPane = new TitledPane("Display Settings", new VBox(10) {{
            setPadding(new Insets(10));
            CheckBox darkModeCheck = new CheckBox("Dark Mode");
            CheckBox compactViewCheck = new CheckBox("Compact View");
            getChildren().addAll(darkModeCheck, compactViewCheck);
        }});

        TitledPane exportPane = new TitledPane("Export Settings", new VBox(10) {{
            setPadding(new Insets(10));
            RadioButton csvFormat = new RadioButton("CSV Format");
            RadioButton excelFormat = new RadioButton("Excel Format");
            csvFormat.setSelected(true);
            ToggleGroup group = new ToggleGroup();
            csvFormat.setToggleGroup(group);
            excelFormat.setToggleGroup(group);
            getChildren().addAll(csvFormat, excelFormat);
        }});

        content.getChildren().addAll(generalPane, displayPane, exportPane);
        dialogPane.setContent(content);

        dialog.showAndWait();
    }

    private void loadSampleData() {
        studentData.add(new Student("S001", "John Doe", "Computer Science", "300", 3.8,
                "john.doe@email.com", "123-456-7890", "2024-01-15 10:30:00", "Active"));
        studentData.add(new Student("S002", "Jane Smith", "Engineering", "200", 3.5,
                "jane.smith@email.com", "234-567-8901", "2024-01-20 14:20:00", "Active"));
        studentData.add(new Student("S003", "Bob Johnson", "Business", "400", 3.2,
                "bob.johnson@email.com", "345-678-9012", "2024-02-01 09:15:00", "Active"));
        studentData.add(new Student("S004", "Alice Brown", "Medicine", "500", 3.9,
                "alice.brown@email.com", "456-789-0123", "2024-02-10 11:45:00", "Active"));
        studentData.add(new Student("S005", "Charlie Wilson", "Arts", "100", 2.8,
                "charlie.wilson@email.com", "567-890-1234", "2024-02-15 16:30:00", "Inactive"));
        studentData.add(new Student("S006", "Diana Prince", "Computer Science", "200", 3.7,
                "diana.prince@email.com", "678-901-2345", "2024-02-20 13:15:00", "Active"));
        studentData.add(new Student("S007", "Bruce Wayne", "Business", "300", 3.1,
                "bruce.wayne@email.com", "789-012-3456", "2024-03-01 10:00:00", "Active"));
        studentData.add(new Student("S008", "Clark Kent", "Engineering", "400", 3.4,
                "clark.kent@email.com", "890-123-4567", "2024-03-05 15:30:00", "Inactive"));
        studentData.add(new Student("S009", "Peter Parker", "Computer Science", "100", 3.6,
                "peter.parker@email.com", "901-234-5678", "2024-03-10 09:45:00", "Active"));
        studentData.add(new Student("S010", "Tony Stark", "Engineering", "500", 3.2,
                "tony.stark@email.com", "012-345-6789", "2024-03-15 14:00:00", "Active"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// Enhanced Student class
class Student {
    private String studentId;
    private String fullName;
    private String programme;
    private String level;
    private double gpa;
    private String email;
    private String phoneNumber;
    private String dateAdded;
    private String status;

    public Student(String studentId, String fullName, String programme,
                   String level, double gpa, String email, String phoneNumber,
                   String dateAdded, String status) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.programme = programme;
        this.level = level;
        this.gpa = gpa;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateAdded = dateAdded;
        this.status = status;
    }

    // Getters
    public String getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getProgramme() { return programme; }
    public String getLevel() { return level; }
    public double getGpa() { return gpa; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDateAdded() { return dateAdded; }
    public String getStatus() { return status; }

    // Setters
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setProgramme(String programme) { this.programme = programme; }
    public void setLevel(String level) { this.level = level; }
    public void setGpa(double gpa) { this.gpa = gpa; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format(
                "ID: %s | Name: %s | Programme: %s | Level: %s | GPA: %.2f | Email: %s | Status: %s",
                studentId, fullName, programme, level, gpa, email, status
        );
    }

    public String toFileString() {
        return String.join(",",
                studentId, fullName, programme, level,
                String.valueOf(gpa), email, phoneNumber, dateAdded, status
        );
    }
}

// Report Row class for table display
class ReportRow {
    private final SimpleStringProperty category;
    private final SimpleStringProperty value;
    private final SimpleStringProperty percentage;

    public ReportRow(String category, String value, String percentage) {
        this.category = new SimpleStringProperty(category);
        this.value = new SimpleStringProperty(value);
        this.percentage = new SimpleStringProperty(percentage);
    }

    public String getCategory() { return category.get(); }
    public String getValue() { return value.get(); }
    public String getPercentage() { return percentage.get(); }

    public SimpleStringProperty categoryProperty() { return category; }
    public SimpleStringProperty valueProperty() { return value; }
    public SimpleStringProperty percentageProperty() { return percentage; }
}