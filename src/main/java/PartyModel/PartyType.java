package PartyModel;
//Тип партии как enum
public enum PartyType {

    REGULAR("REGULAR", "Партия",    true),
    ARCHIVE("ARCHIVE", "Архивная", false),
    IMPORT ("IMPORT",  "Импортная", true);

    private final String code;
    private final String displayName;
    private final boolean editable;

    PartyType(String code, String displayName, boolean editable) {
        this.code = code;
        this.displayName = displayName;
        this.editable = editable;
    }


    public String getCode() { return code; }


    public String getDisplayName() { return displayName; }

    public boolean isEditable() { return editable; }


    public static PartyType fromCode(String code) {
        if (code == null) return null;
        for (PartyType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return null;
    }
}
