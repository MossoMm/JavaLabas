package PartyModel;

import CSV.CSVLoadException;
import WareHouse.WareHouse;
import WareHouse.WarehouseService;
import WareHouse.WarehouseService.AddResult;
import WareHouse.WarehouseService.DeleteOutcome;
import WareHouse.WarehouseService.LoadSummary;
import WareHouse.WarehouseService.SaveResult;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

public class Main extends Application {

    /** Единственная точка доступа к данным — сервис. */
    private final WarehouseService service = new WarehouseService(new WareHouse());

    private final ObservableList<Party> partyList = FXCollections.observableArrayList();
    private TableView<Party> table;
    private Button loadButton, saveButton, addButton, editButton, deleteButton, refreshButton;
    private Label statusLabel, statsLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Склад — учёт партий");
        BorderPane root = new BorderPane();

        root.setTop(buildToolbar());
        root.setCenter(buildTable());
        root.setBottom(buildStatusBar());

        stage.setScene(new Scene(root, AppConstants.WINDOW_WIDTH, AppConstants.WINDOW_HEIGHT));
        stage.show();

        refresh();
    }

    // ---------- Сборка UI ----------

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

        return new ToolBar(loadButton, saveButton, new Separator(),
                addButton, editButton, deleteButton, new Separator(), refreshButton);
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

    // ---------- Состояние ----------

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

    // ---------- CSV: загрузка/сохранение ----------

    private void loadCSV() {
        File file = chooseOpenFile();
        if (file == null) return;

        setBusy(true);
        Task<LoadSummary> task = new Task<>() {
            @Override protected LoadSummary call() throws IOException {
                return service.loadFromFile(file);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            refresh();
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

    // ---------- CRUD ----------

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
            statusLabel.setText("Изменено: " + updated.getArticle() + " | пробы: " + r.probes());
        });
    }

    private void deleteParty() {
        Party p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (!PartyDialogs.confirmDelete(p.getArticle())) return;
        DeleteOutcome o = service.deleteByArticle(p.getArticle());
        refresh();
        statusLabel.setText("Удалено: " + o.article() + " | пробы: " + o.probes());
    }

    // ---------- FileChooser ----------

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

    public static void main(String[] args) {
        launch(args);
    }
}