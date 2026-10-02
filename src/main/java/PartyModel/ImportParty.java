package PartyModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ImportParty extends Party implements Editable {

    private String country;
    private String customsCode;

    public ImportParty(String article, String name, int quantity, String cell,
                       LocalDate date, String country, String customsCode) {
        super(article, name, quantity, cell, date);
        this.country = country;
        this.customsCode = customsCode;
    }

    public String getCountry()      { return country; }
    public String getCustomsCode()  { return customsCode; }

    public void setCountry(String country)          { this.country = country; }
    public void setCustomsCode(String customsCode)  { this.customsCode = customsCode; }

    @Override public String getTypeCode()         { return PartyType.IMPORT.getCode(); }
    @Override public String getTypeDisplayName()  { return PartyType.IMPORT.getDisplayName(); }

    @Override
    public String[] toCsvRow() {
        return concatRow(getTypeCode(), baseCsv(), country, customsCode);
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>(super.validate());
        if (country == null || country.isBlank())
            errors.add(AppConstants.Validation.COUNTRY_BLANK_MSG);
        if (customsCode == null || customsCode.isBlank())
            errors.add(AppConstants.Validation.CUSTOMS_BLANK_MSG);
        return errors;
    }
}