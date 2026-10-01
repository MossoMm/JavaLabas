package PartyModel;

import java.time.LocalDate;


//Архивная партия — read-only тип.
//НЕ реализует PartyModel.Editable: кнопка «Изменить» в GUI недоступна,
//через «Добавить» создать нельзя — только из CSV.

public final class ArchiveParty extends Party {

    public ArchiveParty(String article, String name, int quantity, String cell, LocalDate date) {
        super(article, name, quantity, cell, date);
    }

    @Override
    public String getTypeCode() {
        return "ARCHIVE";
    }

    @Override
    public String getTypeDisplayName() {
        return "Архивная";
    }

    @Override
    public String[] toCsvRow() {
        return concatRow(getTypeCode(), baseCsv());
    }

    //ead-only
    @Override
    public void setArticle(String article) {
        throw new UnsupportedOperationException("Архивная партия — только для чтения");
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Архивная партия — только для чтения");
    }

    @Override
    public void setQuantity(int quantity) {
        throw new UnsupportedOperationException("Архивная партия — только для чтения");
    }

    @Override
    public void setCell(String cell) {
        throw new UnsupportedOperationException("Архивная партия — только для чтения");
    }

    @Override
    public void setDate(LocalDate date) {
        throw new UnsupportedOperationException("Архивная партия — только для чтения");
    }
}