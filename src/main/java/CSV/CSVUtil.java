package CSV;

import PartyModel.ArchiveParty;
import PartyModel.ImportParty;
import PartyModel.Party;
import PartyModel.PartyType;
import PartyModel.RegularParty;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV-загрузчик/сохранятель.
 * Формат строки:
 *   REGULAR,article,name,quantity,cell,date
 *   ARCHIVE,article,name,quantity,cell,date
 *   IMPORT ,article,name,quantity,cell,date,country,customsCode
 *
 * Битые строки НЕ прерывают загрузку — попадают в LoadResult.errors().
 */
public final class CSVUtil {

    private CSVUtil() { }

    /** Сводка загрузки. */
    public record LoadResult(
            List<Party> parties,
            List<CSVLoadException> errors,
            int totalLines
    ) {
        public int loaded()  { return parties.size(); }
        public int skipped() { return errors.size(); }
    }

    public static LoadResult load(String filePath) throws IOException {
        List<Party> parties = new ArrayList<>();
        List<CSVLoadException> errors = new ArrayList<>();
        int totalLines = 0;

        Path path = Paths.get(filePath);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                totalLines++;

                if (line.trim().isEmpty()) {
                    errors.add(new CSVLoadException(
                            CSVLoadException.ErrorCode.EMPTY_LINE, lineNo, line));
                    continue;
                }

                try {
                    parties.add(parseRow(parseLine(line), lineNo, line));
                } catch (CSVLoadException ex) {
                    errors.add(ex);
                } catch (RuntimeException ex) {
                    errors.add(new CSVLoadException(
                            CSVLoadException.ErrorCode.INTERNAL_ERROR,
                            lineNo, line, ex));
                }
            }
        }
        return new LoadResult(parties, errors, totalLines);
    }

    public static void save(String filePath, List<Party> parties) throws IOException {
        Path path = Paths.get(filePath);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (Party p : parties) {
                writer.write(toLine(p.toCsvRow()));
                writer.write(System.lineSeparator());
            }
        }
    }

    // ---------- Разбор строки ----------

    private static Party parseRow(String[] parts, int lineNo, String rawLine) throws CSVLoadException {
        if (parts.length == 0 || parts[0].isBlank()) {
            throw new CSVLoadException(CSVLoadException.ErrorCode.UNKNOWN_TYPE, lineNo, rawLine);
        }
        PartyType type = PartyType.fromCode(parts[0].trim().toUpperCase());
        if (type == null) {
            throw new CSVLoadException(CSVLoadException.ErrorCode.UNKNOWN_TYPE, lineNo, rawLine);
        }
        return switch (type) {
            case REGULAR -> buildRegular(parts, lineNo, rawLine);
            case ARCHIVE -> buildArchive(parts, lineNo, rawLine);
            case IMPORT  -> buildImport (parts, lineNo, rawLine);
        };
    }

    private static Party buildRegular(String[] p, int lineNo, String rawLine) throws CSVLoadException {
        requireCount(p, 6, lineNo, rawLine);
        return new RegularParty(
                requireNonBlank(p[1], CSVLoadException.ErrorCode.EMPTY_ARTICLE, lineNo, rawLine),
                requireNonBlank(p[2], CSVLoadException.ErrorCode.EMPTY_NAME,    lineNo, rawLine),
                parseQuantity  (p[3], lineNo, rawLine),
                requireNonBlank(p[4], CSVLoadException.ErrorCode.EMPTY_CELL,    lineNo, rawLine),
                parseDate      (p[5], lineNo, rawLine));
    }

    private static Party buildArchive(String[] p, int lineNo, String rawLine) throws CSVLoadException {
        requireCount(p, 6, lineNo, rawLine);
        return new ArchiveParty(
                requireNonBlank(p[1], CSVLoadException.ErrorCode.EMPTY_ARTICLE, lineNo, rawLine),
                requireNonBlank(p[2], CSVLoadException.ErrorCode.EMPTY_NAME,    lineNo, rawLine),
                parseQuantity  (p[3], lineNo, rawLine),
                requireNonBlank(p[4], CSVLoadException.ErrorCode.EMPTY_CELL,    lineNo, rawLine),
                parseDate      (p[5], lineNo, rawLine));
    }

    private static Party buildImport(String[] p, int lineNo, String rawLine) throws CSVLoadException {
        requireCount(p, 8, lineNo, rawLine);
        return new ImportParty(
                requireNonBlank(p[1], CSVLoadException.ErrorCode.EMPTY_ARTICLE,   lineNo, rawLine),
                requireNonBlank(p[2], CSVLoadException.ErrorCode.EMPTY_NAME,      lineNo, rawLine),
                parseQuantity  (p[3], lineNo, rawLine),
                requireNonBlank(p[4], CSVLoadException.ErrorCode.EMPTY_CELL,      lineNo, rawLine),
                parseDate      (p[5], lineNo, rawLine),
                requireNonBlank(p[6], CSVLoadException.ErrorCode.MISSING_COUNTRY, lineNo, rawLine),
                requireNonBlank(p[7], CSVLoadException.ErrorCode.MISSING_CUSTOMS, lineNo, rawLine));
    }

    private static String requireNonBlank(String s, CSVLoadException.ErrorCode code,
                                          int lineNo, String rawLine) throws CSVLoadException {
        if (s == null || s.isBlank()) throw new CSVLoadException(code, lineNo, rawLine);
        return s.trim();
    }

    private static void requireCount(String[] p, int expected, int lineNo, String rawLine)
            throws CSVLoadException {
        if (p.length != expected) {
            throw new CSVLoadException(CSVLoadException.ErrorCode.WRONG_FIELD_COUNT, lineNo, rawLine);
        }
    }

    private static int parseQuantity(String s, int lineNo, String rawLine) throws CSVLoadException {
        int q;
        try {
            q = Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new CSVLoadException(CSVLoadException.ErrorCode.BAD_NUMBER, lineNo, rawLine, e);
        }
        if (q < 0) throw new CSVLoadException(CSVLoadException.ErrorCode.NEGATIVE_QUANTITY, lineNo, rawLine);
        return q;
    }

    private static LocalDate parseDate(String s, int lineNo, String rawLine) throws CSVLoadException {
        try {
            return LocalDate.parse(s.trim(), Party.DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new CSVLoadException(CSVLoadException.ErrorCode.BAD_DATE, lineNo, rawLine, e);
        }
    }

    // ---------- Низкоуровневый CSV ----------

    static String[] parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    static String toLine(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(',');
            String p = (parts[i] == null) ? "" : parts[i];
            boolean needsQuoting = p.contains(",") || p.contains("\"") || p.contains("\n");
            if (needsQuoting) {
                sb.append('"').append(p.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(p);
            }
        }
        return sb.toString();
    }
}