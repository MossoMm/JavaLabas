package CSV;

//Собственные исключения
public class CSVLoadException extends Exception {

    /* Перечисление кодов ошибок при разборе CSV-строки в партию. */
    public enum ErrorCode {
        EMPTY_LINE          ("Пустая строка"),
        WRONG_FIELD_COUNT   ("Неверное число полей"),
        UNKNOWN_TYPE        ("Неизвестный тип партии"),
        EMPTY_ARTICLE       ("Пустой артикул"),
        EMPTY_NAME          ("Пустое название"),
        EMPTY_CELL          ("Пустая ячейка"),
        BAD_NUMBER          ("Неверный формат количества (ожидалось целое)"),
        NEGATIVE_QUANTITY   ("Отрицательное количество"),
        BAD_DATE            ("Неверный формат даты (ГГГГ-ММ-ДД)"),
        MISSING_COUNTRY     ("Пустая страна (для импортной партии)"),
        MISSING_CUSTOMS     ("Пустой таможенный код (для импортной партии)"),
        INTERNAL_ERROR      ("Внутренняя ошибка парсинга");

        private final String description;

        ErrorCode(String description) {
            this.description = description;
        }

        /* Человекочитаемое описание ошибки (для диалогов GUI). */
        public String getDescription() {
            return description;
        }
    }

    private final ErrorCode code;
    private final int lineNumber;
    private final String rawLine;

    public CSVLoadException(ErrorCode code, int lineNumber, String rawLine) {
        super(String.format("Строка %d: %s", lineNumber, code.getDescription()));
        this.code = code;
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
    }

    public CSVLoadException(ErrorCode code, int lineNumber, String rawLine, Throwable cause) {
        super(String.format("Строка %d: %s", lineNumber, code.getDescription()), cause);
        this.code = code;
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
    }

    public ErrorCode getCode()       { return code; }
    public int      getLineNumber()  { return lineNumber; }
    public String   getRawLine()     { return rawLine; }

    @Override
    public String toString() {
        String preview = rawLine != null && rawLine.length() > 80
            ? rawLine.substring(0, 77) + "..." : rawLine;
        return "CSVLoadException{code=" + code +
               ", lineNumber=" + lineNumber +
               ", rawLine='" + preview + "'}";
    }
}
