package PartyModel;

import java.time.LocalDate;
import java.util.List;

public class RegularParty extends Party implements Editable {

    public RegularParty(String article, String name, int quantity, String cell, LocalDate date) {
        super(article, name, quantity, cell, date);
    }

    @Override public String getTypeCode()         { return PartyType.REGULAR.getCode(); }
    @Override public String getTypeDisplayName()  { return PartyType.REGULAR.getDisplayName(); }

    @Override
    public String[] toCsvRow() {
        return concatRow(getTypeCode(), baseCsv());
    }

    @Override
    public List<String> validate() {
        return super.validate();
    }
}