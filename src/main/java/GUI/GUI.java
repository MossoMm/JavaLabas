package GUI;

import CSV.CSVLoadException;
import CSV.DataGenerator;
import PartyModel.*;
import WareHouse.WareHouse;
import WareHouse.WarehouseService;
import WareHouse.WarehouseService.LoadSummary;
import WareHouse.WarehouseService.SaveResult;
import WareHouse.WarehouseService.AddResult;
import WareHouse.WarehouseService.DeleteOutcome;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public class GUI extends Application {

    /** Единственная точка доступа к данным — сервис. */
    private final WarehouseService service = new WarehouseService(new WareHouse());

    private final ObservableList<Party> partyList = FXCollections.observableArrayList();
    private TableView<Party> table;
    private Button loadButton, saveButton, addButton, editButton, deleteButton, refreshButton;
    private Label statusLabel, statsLabel;

    /** HashTable Visual **/
    private Canvas visualCanvas;
    private HashTableVisual tableVisualiser;
    private Task<Void> generateTask;
    //** Data Generation Fields**/
    private Button generateButton;
    private Button cancelGenerateButton;
    private void drawVisualiser(int[] probedIndices)
    {
        if (tableVisualiser == null) return;
        tableVisualiser.draw(service.getSnapshot(), probedIndices != null ? probedIndices : new int[0]);

    }
    @Override
    public void start(Stage stage) {
        stage.setTitle("Склад — учёт партий");
        BorderPane root = new BorderPane();

        root.setTop(buildToolbar());

        javafx.scene.layout.VBox centerBox = new javafx.scene.layout.VBox(5);
        centerBox.getChildren().addAll(buildTable(), buildVisualizer());
        javafx.scene.layout.VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        root.setCenter(centerBox);
        root.setBottom(buildStatusBar());

        stage.setScene(new Scene(root, AppConstants.WINDOW_WIDTH, AppConstants.WINDOW_HEIGHT));
        stage.show();

        refresh();

    }

    // Сборка UI

    private ToolBar buildToolbar() {
        loadButton    = new Button("Загрузить CSV");
        saveButton    = new Button("Сохранить CSV");
        addButton     = new Button("Добавить");
        editButton    = new Button("Изменить");
        deleteButton  = new Button("Удалить");
        refreshButton = new Button("Обновить");

        loadButton   .setOnAction(e -> loadCSV());
        saveButton   .setOnAction(e -> saveCSV());
        addButton    .setOnAction(e -> addParty());
        editButton   .setOnAction(e -> editParty());
        deleteButton .setOnAction(e -> deleteParty());
        refreshButton.setOnAction(e -> refresh());
        generateButton = new Button("Генерировать 10 000");
        cancelGenerateButton = new Button("Отмена");
        cancelGenerateButton.setDisable(true);
        cancelGenerateButton.setStyle("-fx-base: #ff4444; -fx-text-fill: white;");

        generateButton.setOnAction(e -> startGeneration());
        cancelGenerateButton.setOnAction(e -> {
            if (generateTask != null) generateTask.cancel();
        });

        return new ToolBar(loadButton, saveButton, new Separator(),
                addButton, editButton, deleteButton, new Separator(),
                refreshButton, new Separator(),
                generateButton, cancelGenerateButton);
    }

    private TableView<Party> buildTable() {
        table = new TableView<>(partyList);
        table.getColumns().addAll(
                column("Артикул", "article",         AppConstants.COL_ARTICLE),
                column("Название","name",            AppConstants.COL_NAME),
                column("Кол-во",  "quantity",        AppConstants.COL_QTY),
                column("Ячейка",  "cell",            AppConstants.COL_CELL),
                column("Дата",    "dateStr",         AppConstants.COL_DATE),
                column("Тип",     "typeDisplayName", AppConstants.COL_TYPE));

        TableColumn<Party, String> extra = new TableColumn<>("Доп. поля");
        extra.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                extraInfo(c.getValue())));
        extra.setPrefWidth(AppConstants.COL_EXTRA);
        table.getColumns().add(extra);

        table.getSelectionModel().selectedItemProperty()
                .addListener((o, ov, nv) -> updateButtons());

        table.setRowFactory(tv -> {
            TableRow<Party> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) onRowDoubleClick(row.getItem());
            });
            return row;
        });
        return table;
    }

    private HBox buildStatusBar() {
        statusLabel = new Label("Готово к работе");
        statsLabel  = new Label();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(20, statusLabel, spacer, statsLabel);
        bar.setPadding(new Insets(4));
        return bar;
    }
    private Canvas buildVisualizer()
    {
        visualCanvas = new Canvas(AppConstants.WINDOW_WIDTH, 250);
        tableVisualiser = new HashTableVisual(visualCanvas);
        drawVisualiser(new int[0]);
        return visualCanvas;
    }
    private static String extraInfo(Party p) {
        if (p instanceof ImportParty ip) return "страна=" + ip.getCountry();
        if (p instanceof Editable)       return "—";
        return "read-only";
    }

    private static <T> TableColumn<Party, T> column(String title, String prop, double width) {
        TableColumn<Party, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setPrefWidth(width);
        return col;
    }

    //Состояние

    private void updateButtons() {
        Party sel = table.getSelectionModel().getSelectedItem();
        editButton  .setDisable(sel == null || !(sel instanceof Editable));
        deleteButton.setDisable(sel == null);
    }

    private void setBusy(boolean busy) {
        loadButton.setDisable(busy);
        saveButton.setDisable(busy);
        addButton.setDisable(busy);
        refreshButton.setDisable(busy);
        if (busy) {
            editButton.setDisable(true);
            deleteButton.setDisable(true);
        } else {
            updateButtons();
        }
    }

    private void onRowDoubleClick(Party p) {
        if (p instanceof Editable) editParty();
        else PartyDialogs.showInfo("Только чтение",
                "Архивная партия недоступна для редактирования.");
    }

    private void refresh() {
        partyList.setAll(service.all());
        statsLabel.setText(String.format(
                "Записей: %d | Ёмкость: %d | Заполненность: %.1f%% | ср. пробы: put=%.2f, get=%.2f, del=%.2f",
                service.size(), service.capacity(), service.loadFactor() * 100,
                service.avgPutProbes(), service.avgGetProbes(), service.avgDeleteProbes()));
    }

    // CSV: загрузка/сохранение

    private void loadCSV() {
        File file = chooseOpenFile();
        if (file == null) return;

        setBusy(true);
        Task<WarehouseService.LoadSummary> task = new Task<>() {
            @Override protected LoadSummary call() throws IOException {
                return service.loadFromFile(file);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            refresh();
            drawVisualiser(new int[0]);
            reportLoad(task.getValue());
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable ex = task.getException();
            PartyDialogs.showError("Ошибка загрузки",
                    ex == null ? "Неизвестная ошибка" : ex.getMessage());
        });
        new Thread(task, "csv-load").start();
    }

    private void reportLoad(LoadSummary s) {
        String brief = "Загружено: " + s.loaded()
                + ", дубликатов: " + s.duplicates()
                + ", ошибок: " + s.errors().size();
        statusLabel.setText(brief);

        if (s.hasIssues()) {
            String details = s.errors().stream()
                    .limit(AppConstants.MAX_ERROR_DETAILS_IN_DIALOG)
                    .map(CSVLoadException::getMessage)
                    .collect(Collectors.joining("\n"));
            PartyDialogs.showInfo("Часть строк пропущена",
                    brief + (details.isBlank() ? "" : "\n\n" + details));
        }
    }

    private void saveCSV() {
        if (service.size() == 0) {
            PartyDialogs.showInfo("Сохранение", "Список пуст.");
            return;
        }
        File file = chooseSaveFile();
        if (file == null) return;

        try {
            SaveResult r = service.saveToFile(file);
            statusLabel.setText("Сохранено: " + r.saved());
            PartyDialogs.showInfo("Сохранение", "Успешно сохранено: " + r.saved());
        } catch (IOException ex) {
            PartyDialogs.showError("Ошибка сохранения", ex.getMessage());
        }
    }

    // CRUD

    private void addParty() {
        PartyDialogs.chooseAddType().ifPresent(importMode -> {
            String title = importMode ? "Добавить импортную" : "Добавить партию";
            PartyDialogs.showPartyForm(title, null, importMode).ifPresent(p -> {
                AddResult r = service.addParty(p);
                if (r.wasDuplicate()) {
                    PartyDialogs.showError("Дубликат",
                            String.format(AppConstants.Validation.DUPLICATE_MSG_FORMAT, p.getArticle()));
                    return;
                }
                refresh();
                drawVisualiser(r.probedIndices());
                statusLabel.setText("Добавлено: " + p.getArticle() + " | пробы: " + r.probes());
            });
        });
    }

    private void editParty() {
        Party p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (!(p instanceof Editable)) {
            PartyDialogs.showError("Недоступно", "Архивная партия только для чтения.");
            return;
        }
        boolean importMode = p instanceof ImportParty;
        String title = importMode ? "Изменить импортную" : "Изменить партию";
        PartyDialogs.showPartyForm(title, p, importMode).ifPresent(updated -> {
            AddResult r = service.updateParty(updated);
            refresh();
            drawVisualiser(r.probedIndices());
            statusLabel.setText("Изменено: " + updated.getArticle() + " | пробы: " + r.probes());
        });
    }

    private void deleteParty() {
        Party p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (!PartyDialogs.confirmDelete(p.getArticle())) return;
        DeleteOutcome o = service.deleteByArticle(p.getArticle());
        refresh();
        drawVisualiser(o.probedIndices());
        statusLabel.setText("Удалено: " + o.article() + " | пробы: " + o.probes());
    }

    // FileChooser

    private File chooseOpenFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "CSV", "*" + AppConstants.CSV_EXTENSION));
        return fc.showOpenDialog(null);
    }

    private File chooseSaveFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "CSV", "*" + AppConstants.CSV_EXTENSION));
        return fc.showSaveDialog(null);
    }
    /** Data generation **/
    private void startGeneration() {
        generateButton.setDisable(true);
        cancelGenerateButton.setDisable(false);
        statusLabel.setText("Генерация...");

        generateTask = new Task<>() {
            @Override
            protected Void call() {
                int count = 10_000;
                for (int i = 0; i < count; i++) {

                    if (isCancelled()) break;

                    service.addParty(DataGenerator.generateRandomParty(i));
                }
                return null;
            }
        };

        generateTask.setOnSucceeded(e -> {
            generateButton.setDisable(false);
            cancelGenerateButton.setDisable(true);
            statusLabel.setText("Готово: 10 000 записей");
            refresh();
        });

        generateTask.setOnCancelled(e -> {
            generateButton.setDisable(false);
            cancelGenerateButton.setDisable(true);
            statusLabel.setText("Отменено. Записей: " + service.size());
            refresh();
        });

        new Thread(generateTask, "gen-thread").start();
    }
}