package PartyModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public abstract class Party {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private String article;
    private String name;
    private int quantity;
    private String cell;
    private LocalDate date;

    protected Party(String article, String name, int quantity, String cell, LocalDate date) {
        this.article = article;
        this.name = name;
        this.quantity = quantity;
        this.cell = cell;
        this.date = date;
    }

    public String getArticle()  { return article; }
    public String getName()     { return name; }
    public int getQuantity()    { return quantity; }
    public String getCell()     { return cell; }
    public LocalDate getDate()  { return date; }
    public String getDateStr()  { return date != null ? date.format(DATE_FMT) : ""; }

    public abstract String getTypeCode();
    public abstract String getTypeDisplayName();
    public abstract String[] toCsvRow();

    public void setArticle(String article)   { this.article = article; }
    public void setName(String name)         { this.name = name; }
    public void setQuantity(int quantity)    { this.quantity = quantity; }
    public void setCell(String cell)         { this.cell = cell; }
    public void setDate(LocalDate date)      { this.date = date; }

    /** Базовая валидация. Подклассы расширяют через super.validate(). */
    protected List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (article == null || article.isBlank()) errors.add(AppConstants.Validation.ARTICLE_BLANK_MSG);
        if (name == null || name.isBlank())       errors.add(AppConstants.Validation.NAME_BLANK_MSG);
        if (quantity < 0)                         errors.add(AppConstants.Validation.QTY_NEGATIVE_MSG);
        if (cell == null || cell.isBlank())       errors.add(AppConstants.Validation.CELL_BLANK_MSG);
        if (date == null)                         errors.add(AppConstants.Validation.DATE_REQUIRED_MSG);
        return errors;
    }

    protected String[] baseCsv() {
        return new String[] { article, name, String.valueOf(quantity), cell, getDateStr() };
    }

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
                getTypeCode(), article, name, quantity, cell, getDateStr());
    }
}