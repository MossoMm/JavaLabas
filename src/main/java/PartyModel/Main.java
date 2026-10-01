package PartyModel;
import CSV.CSVUtil;
import PartyModel.Editable;
import PartyModel.ImportParty;
import PartyModel.Party;
import PartyModel.RegularParty;
import WareHouse.HashTable;
import WareHouse.WareHouse;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class Main extends Application {
    private final WareHouse warehouse = new WareHouse();
    private TableView<Party> table;
    private ObservableList<Party> partyList = FXCollections.observableArrayList();
    private Button editButton, deleteButton;
    private Label statusLabel, statsLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Склад — учёт партий");
        BorderPane root = new BorderPane();

        // Toolbar
        ToolBar toolbar = new ToolBar();
        Button loadBtn = new Button("Загрузить CSV");
        Button saveBtn = new Button("Сохранить CSV");
        Button addBtn = new Button("Добавить");
        editButton = new Button("Изменить");
        deleteButton = new Button("Удалить");
        Button refreshBtn = new Button("Обновить");

        loadBtn.setOnAction(e -> loadCSV());
        saveBtn.setOnAction(e -> saveCSV());
        addBtn.setOnAction(e -> addParty());
        editButton.setOnAction(e -> editParty());
        deleteButton.setOnAction(e -> deleteParty());
        refreshBtn.setOnAction(e -> refresh());

        toolbar.getItems().addAll(loadBtn, saveBtn, new Separator(), addBtn, editButton, deleteButton, new Separator(), refreshBtn);
        root.setTop(toolbar);

        // Table
        table = new TableView<>(partyList);
        table.getColumns().addAll(
                createColumn("Артикул", "article", 100),
                createColumn("Название", "name", 200),
                createColumn("Кол-во", "quantity", 80),
                createColumn("Ячейка", "cell", 80),
                createColumn("Дата", "dateStr", 100),
                createColumn("Тип", "typeDisplayName", 120)
        );

        TableColumn<Party, String> extraCol = new TableColumn<>("Доп. поля");
        extraCol.setCellValueFactory(c -> {
            Party p = c.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    p instanceof ImportParty ? "страна=" + ((ImportParty)p).getCountry() : (p instanceof Editable ? "—" : "read-only")
            );
        });
        extraCol.setPrefWidth(200);
        table.getColumns().add(extraCol);

        table.getSelectionModel().selectedItemProperty().addListener((o, old, val) -> updateButtons());
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                Party p = table.getSelectionModel().getSelectedItem();
                if (p instanceof Editable) editParty();
                else showAlert("Только чтение", "Архивная партия недоступна для редактирования.");
            }
        });
        root.setCenter(table);

        // Status bar
        statusLabel = new Label("Готово к работе");
        statsLabel = new Label();
        HBox status = new HBox(20, statusLabel, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, statsLabel);
        status.setPadding(new Insets(4));
        root.setBottom(status);

        stage.setScene(new Scene(root, 950, 600));
        stage.show();
        refresh();
    }

    private <T> TableColumn<Party, T> createColumn(String title, String prop, double width) {
        TableColumn<Party, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setPrefWidth(width);
        return col;
    }

    private void updateButtons() {
        Party sel = table.getSelectionModel().getSelectedItem();
        editButton.setDisable(sel == null || !(sel instanceof Editable));
        deleteButton.setDisable(sel == null);
    }

    private void refresh() {
        partyList.setAll(warehouse.all());
        statsLabel.setText(String.format("Записей: %d | Ёмкость: %d | Заполненность: %.1f%% | ср. пробы: put=%.2f, get=%.2f, del=%.2f",
                warehouse.size(), warehouse.capacity(), warehouse.loadFactor() * 100,
                warehouse.avgPutProbes(), warehouse.avgGetProbes(), warehouse.avgDeleteProbes()));
    }

    private void loadCSV() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            try {
                CSVUtil.LoadResult result = CSVUtil.load(file.getAbsolutePath());
                warehouse.clear();
                int dups = 0;
                for (Party p : result.parties) {
                    if (warehouse.get(p.getArticle()).found) dups++;
                    else warehouse.put(p);
                }
                refresh();
                statusLabel.setText("Загружено: " + warehouse.size() + ", пропущено: " + result.skipped + (dups > 0 ? " (дубликатов: " + dups + ")" : ""));
                if (result.skipped > 0 || dups > 0) showAlert("Часть строк пропущена", "Загружено: " + warehouse.size() + "\nПропущено: " + result.skipped + "\nДубликатов: " + dups);
            } catch (Exception ex) {
                showError("Ошибка загрузки", ex.getMessage());
            }
        }
    }

    private void saveCSV() {
        if (warehouse.size() == 0) {
            showAlert("Сохранение", "Список пуст.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fc.showSaveDialog(null);
        if (file != null) {
            try {
                String path = file.getAbsolutePath();
                if (!path.toLowerCase().endsWith(".csv")) path += ".csv";
                CSVUtil.save(path, (List<Party>) warehouse.all());
                statusLabel.setText("Сохранено: " + warehouse.size());
                showAlert("Сохранение", "Успешно сохранено: " + warehouse.size());
            } catch (Exception ex) {
                showError("Ошибка сохранения", ex.getMessage());
            }
        }
    }

    private void addParty() {
        ChoiceDialog<String> dlg = new ChoiceDialog<>("Партия (базовая)", "Партия (базовая)", "Импортная партия");
        dlg.setTitle("Добавить партию");
        dlg.setHeaderText("Выберите тип:");
        dlg.showAndWait().ifPresent(type -> {
            boolean importMode = type.contains("Импортная");
            showPartyDialog(importMode ? "Добавить импортную" : "Добавить партию", null, importMode).ifPresent(p -> {
                if (warehouse.get(p.getArticle()).found) {
                    showError("Дубликат", "Партия «" + p.getArticle() + "» уже существует.");
                    return;
                }
                HashTable.PutResult res = warehouse.put(p);
                refresh();
                statusLabel.setText("Добавлено: " + p.getArticle() + " | пробы: " + res.probes);
            });
        });
    }

    private void editParty() {
        Party p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (!(p instanceof Editable)) {
            showError("Недоступно", "Архивная партия только для чтения.");
            return;
        }
        boolean importMode = p instanceof ImportParty;
        showPartyDialog(importMode ? "Изменить импортную" : "Изменить партию", p, importMode).ifPresent(updated -> {
            warehouse.put(updated);
            refresh();
            statusLabel.setText("Изменено: " + updated.getArticle());
        });
    }

    private void deleteParty() {
        Party p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить «" + p.getArticle() + "»?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
            HashTable.DeleteResult res = warehouse.delete(p.getArticle());
            refresh();
            statusLabel.setText("Удалено: " + p.getArticle() + " | пробы: " + res.probes);
        });
    }

    private Optional<Party> showPartyDialog(String title, Party existing, boolean importMode) {
        Dialog<Party> dialog = new Dialog<>();
        dialog.setTitle(title);

        TextField article = new TextField(), name = new TextField(), qty = new TextField(), cell = new TextField(), date = new TextField();
        TextField country = new TextField(), customs = new TextField();

        if (existing != null) {
            article.setText(existing.getArticle());
            article.setEditable(false);
            name.setText(existing.getName());
            qty.setText(String.valueOf(existing.getQuantity()));
            cell.setText(existing.getCell());
            date.setText(existing.getDateStr());
            if (existing instanceof ImportParty) {
                country.setText(((ImportParty)existing).getCountry());
                customs.setText(((ImportParty)existing).getCustomsCode());
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        int row = 0;
        grid.add(new Label("Артикул:"), 0, row); grid.add(article, 1, row++);
        grid.add(new Label("Название:"), 0, row); grid.add(name, 1, row++);
        grid.add(new Label("Количество:"), 0, row); grid.add(qty, 1, row++);
        grid.add(new Label("Ячейка:"), 0, row); grid.add(cell, 1, row++);
        grid.add(new Label("Дата:"), 0, row); grid.add(date, 1, row++);

        if (importMode) {
            grid.add(new Label("Страна:"), 0, row); grid.add(country, 1, row++);
            grid.add(new Label("Тамож. код:"), 0, row); grid.add(customs, 1, row++);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                if (article.getText().trim().isEmpty()) throw new RuntimeException("Артикул пуст");
                int q = Integer.parseInt(qty.getText().trim());
                if (q < 0) throw new RuntimeException();
                LocalDate.parse(date.getText().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                if (importMode && (country.getText().trim().isEmpty() || customs.getText().trim().isEmpty())) {
                    throw new RuntimeException("Заполните страну и код");
                }
                return importMode ? new ImportParty(article.getText().trim(), name.getText().trim(), q, cell.getText().trim(),
                        LocalDate.parse(date.getText().trim(), DateTimeFormatter.ISO_LOCAL_DATE), country.getText().trim(), customs.getText().trim())
                        : new RegularParty(article.getText().trim(), name.getText().trim(), q, cell.getText().trim(),
                        LocalDate.parse(date.getText().trim(), DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception e) {
                showError("Ошибка ввода", e.getMessage().isEmpty() ? "Неверный формат" : e.getMessage());
                return null;
            }
        });

        return dialog.showAndWait();
    }

    private void showAlert(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).show();
    }

    private void showError(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}