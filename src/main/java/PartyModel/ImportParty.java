package PartyModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Импортная партия — редактируемый тип с дополнительными полями.
//Реализует PartyModel.Editable: доступна через «Добавить» и «Изменить».

/* Дополнительные поля:
 *   - страна происхождения
 *   - таможенный код
 */
public class ImportParty extends Party implements Editable {

    private String country;       // страна
    private String customsCode;   // таможенный код

    public ImportParty(String article, String name, int quantity, String cell,
                       LocalDate date, String country, String customsCode) {
        super(article, name, quantity, cell, date);
        this.country = country;
        this.customsCode = customsCode;
    }

    public String getCountry() { return country; }
    public String getCustomsCode() { return customsCode; }

    public void setCountry(String country) { this.country = country; }
    public void setCustomsCode(String customsCode) { this.customsCode = customsCode; }

    @Override
    public String getTypeCode() {
        return "IMPORT";
    }

    @Override
    public String getTypeDisplayName() {
        return "Импортная";
    }

    @Override
    public String[] toCsvRow() {
        return concatRow(getTypeCode(), baseCsv(), country, customsCode);
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>(super.validate()); // базовые проверки
        if (country == null || country.isBlank()) {
            errors.add("Страна обязательна");
        }
        if (customsCode == null || customsCode.isBlank()) {
            errors.add("Таможенный код обязателен");
        }
        return errors;
    }
}