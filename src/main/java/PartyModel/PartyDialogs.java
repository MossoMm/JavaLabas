package PartyModel;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Все модальные диалоги GUI — в одном месте.
 * Бизнес-логики здесь нет: только UI и валидация формата ввода.
 */
public final class PartyDialogs {

    private PartyDialogs() {}

    public static void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.show();
    }

    public static void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.show();
    }

    public static boolean confirmDelete(String article) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить «" + article + "»?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    /** Выбор типа для «Добавить». true — импортная, false — обычная. */
    public static Optional<Boolean> chooseAddType() {
        ChoiceDialog<String> dlg = new ChoiceDialog<>(
                "Партия (базовая)", "Партия (базовая)", "Импортная партия");
        dlg.setTitle("Добавить партию");
        dlg.setHeaderText("Выберите тип:");
        return dlg.showAndWait().map(s -> s.contains("Импортная"));
    }

    /** Форма создания/редактирования партии. */
    public static Optional<Party> showPartyForm(String title, Party existing, boolean importMode) {
        Dialog<Party> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        TextField article = new TextField();
        TextField name    = new TextField();
        TextField qty     = new TextField();
        TextField cell    = new TextField();
        TextField date    = new TextField();
        TextField country = new TextField();
        TextField customs = new TextField();

        if (existing != null) {
            article.setText(existing.getArticle());
            article.setEditable(false);
            name.setText(existing.getName());
            qty.setText(String.valueOf(existing.getQuantity()));
            cell.setText(existing.getCell());
            date.setText(existing.getDateStr());
            if (existing instanceof ImportParty ip) {
                country.setText(ip.getCountry());
                customs.setText(ip.getCustomsCode());
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;
        grid.add(new Label("Артикул:"),    0, row); grid.add(article, 1, row++);
        grid.add(new Label("Название:"),   0, row); grid.add(name,    1, row++);
        grid.add(new Label("Количество:"), 0, row); grid.add(qty,     1, row++);
        grid.add(new Label("Ячейка:"),     0, row); grid.add(cell,    1, row++);
        grid.add(new Label("Дата:"),       0, row); grid.add(date,    1, row++);
        if (importMode) {
            grid.add(new Label("Страна:"),      0, row); grid.add(country, 1, row++);
            grid.add(new Label("Тамож. код:"),  0, row); grid.add(customs, 1, row++);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn != ButtonType.OK ? null :
                buildParty(article, name, qty, cell, date, country, customs, importMode));

        return dialog.showAndWait();
    }

    private static Party buildParty(TextField article, TextField name, TextField qty,
                                    TextField cell, TextField date,
                                    TextField country, TextField customs,
                                    boolean importMode) {
        try {
            String a = article.getText().trim();
            if (a.isEmpty())
                throw new IllegalArgumentException(AppConstants.Validation.ARTICLE_BLANK_MSG);

            int q;
            try {
                q = Integer.parseInt(qty.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(AppConstants.Validation.QTY_NOT_INTEGER_MSG);
            }
            if (q < 0) throw new IllegalArgumentException(AppConstants.Validation.QTY_NEGATIVE_MSG);

            LocalDate d;
            try {
                d = LocalDate.parse(date.getText().trim(), Party.DATE_FMT);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(AppConstants.Validation.DATE_FORMAT_MSG);
            }

            String c  = country.getText().trim();
            String cc = customs.getText().trim();
            if (importMode) {
                if (c.isEmpty())  throw new IllegalArgumentException(AppConstants.Validation.COUNTRY_BLANK_MSG);
                if (cc.isEmpty()) throw new IllegalArgumentException(AppConstants.Validation.CUSTOMS_BLANK_MSG);
            }

            return importMode
                    ? new ImportParty(a, name.getText().trim(), q, cell.getText().trim(), d, c, cc)
                    : new RegularParty(a, name.getText().trim(), q, cell.getText().trim(), d);
        } catch (IllegalArgumentException ex) {
            showError("Ошибка ввода", ex.getMessage());
            return null;
        }
    }
}