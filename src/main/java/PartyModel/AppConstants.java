package PartyModel;

public final class AppConstants {

    private AppConstants() {}

    // --- Hash table ---
    public static final double HASH_LOAD_FACTOR_THRESHOLD = 0.7;
    public static final int    HASH_INITIAL_CAPACITY      = 16;

    // --- GUI ---
    public static final double WINDOW_WIDTH  = 950;
    public static final double WINDOW_HEIGHT = 600;

    public static final double COL_ARTICLE = 100;
    public static final double COL_NAME    = 200;
    public static final double COL_QTY     = 80;
    public static final double COL_CELL    = 80;
    public static final double COL_DATE    = 100;
    public static final double COL_TYPE    = 120;
    public static final double COL_EXTRA   = 200;

    public static final int MAX_ERROR_DETAILS_IN_DIALOG = 20;

    // --- File ---
    public static final String CSV_EXTENSION = ".csv";

    // --- Validation messages ---
    public static final class Validation {
        private Validation() {}

        public static final String ARTICLE_BLANK_MSG  = "Артикул обязателен";
        public static final String NAME_BLANK_MSG     = "Название обязательно";
        public static final String CELL_BLANK_MSG     = "Ячейка обязательна";
        public static final String QTY_NEGATIVE_MSG   = "Количество < 0";
        public static final String DATE_REQUIRED_MSG  = "Дата обязательна";
        public static final String COUNTRY_BLANK_MSG  = "Страна обязательна";
        public static final String CUSTOMS_BLANK_MSG  = "Таможенный код обязателен";

        public static final String QTY_NOT_INTEGER_MSG   = "Количество должно быть целым числом";
        public static final String DATE_FORMAT_MSG       = "Дата должна быть в формате ГГГГ-ММ-ДД";
        public static final String DUPLICATE_MSG_FORMAT  = "Партия «%s» уже существует";
    }
}