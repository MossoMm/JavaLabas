package PartyModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public abstract class Party {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private String article;   // артикул — ключ в хеш-таблице
    private String name;      // название
    private int quantity;     // количество
    private String cell;      // ячейка
    private LocalDate date;   // дата завоза

    protected Party(String article, String name, int quantity, String cell, LocalDate date) {
        this.article = article;
        this.name = name;
        this.quantity = quantity;
        this.cell = cell;
        this.date = date;
    }

    public String getArticle() { return article; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public String getCell() { return cell; }
    public LocalDate getDate() { return date; }
    public String getDateStr() { return date != null ? date.format(DATE_FMT) : "No date"; }
    public abstract String getTypeCode(); //name of party type
    public abstract String getTypeDisplayName();   // имя для GUI
    public abstract String[] toCsvRow();            // полная CSV-строка с типом

    public void setArticle(String article) { this.article = article; }
    public void setName(String name) { this.name = name; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setCell(String cell) { this.cell = cell; }
    public void setDate(LocalDate date) { this.date = date; }


    //errors validation
    protected List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (article == null || article.isBlank()) errors.add("Артикул обязателен");
        if (name == null || name.isBlank()) errors.add("Название обязательно");
        if (quantity < 0) errors.add("Количество < 0");
        if (cell == null || cell.isBlank()) errors.add("Ячейка обязательна");
        if (date == null) errors.add("Дата обязательна");
        return errors;
    }
    /*
     Factory-метод: парсит одну CSV-строку в конкретный подкласс Party.
     Возвращает null, если строка битая (пропускается при загрузке).
     */
    public static Party fromCsvRow(String[] parts) {
        if (parts == null || parts.length < 6) return null;
        try {
            String type = parts[0].trim();
            String article = parts[1].trim();
            String name = parts[2].trim();
            int qty = Integer.parseInt(parts[3].trim());
            String cell = parts[4].trim();
            LocalDate date = LocalDate.parse(parts[5].trim(), DATE_FMT);

            switch (type) {
                case "REGULAR":
                    if (parts.length != 6) return null;
                    return new RegularParty(article, name, qty, cell, date);
                case "ARCHIVE":
                    if (parts.length != 6) return null;
                    return new ArchiveParty(article, name, qty, cell, date);
                case "IMPORT":
                    if (parts.length != 8) return null;
                    String country = parts[6];
                    String customs = parts[7];
                    return new ImportParty(article, name, qty, cell, date, country, customs);
                default:
                    return null; // неизвестный тип
            }
        } catch (Exception e) {
            return null; // любое исключение → строка битая
        }
    }

    // vars in CSV-columns
    protected String[] baseCsv() {
        return new String[] {
                article, name, String.valueOf(quantity), cell, getDateStr()
        };
    }

    //сборка строки из массива
    protected static String[] concatRow(String typeCode, String[] base, String... extras) {
        String[] row = new String[1 + base.length + extras.length];
        row[0] = typeCode;
        System.arraycopy(base, 0, row, 1, base.length);
        System.arraycopy(extras, 0, row, 1 + base.length, extras.length);
        return row;
    }



    @Override
    public String toString() {
        return String.format("[%s] арт=%s название=%s кол-во=%d ячейка=%s дата=%s",
                article, name, quantity, cell, getDateStr());
    }
}