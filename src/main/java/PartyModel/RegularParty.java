package PartyModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//Базовая редактируемая партия.
//Реализует PartyModel.Editable: может быть создана через «Добавить» и изменена через «Изменить».

public class RegularParty extends Party implements Editable {

    public RegularParty(String article, String name, int quantity, String cell, LocalDate date) {
        super(article, name, quantity, cell, date);
    }

    @Override
    public String getTypeCode() {
        return "REGULAR";
    }

    @Override
    public String getTypeDisplayName() {
        return "Партия";
    }

    @Override
    public String[] toCsvRow() {
        return concatRow(getTypeCode(), baseCsv());
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>(super.validate()); // базовые проверки
        return errors;
    }
}
