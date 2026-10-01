package CSV;

import PartyModel.Party;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

//Утилита для загрузки и сохранения партий в CSV.

/*Формат строки CSV (тип указан в первой колонке):
  REGULAR,article,name,quantity,cell,date
  ARCHIVE,article,name,quantity,cell,date
  IMPORT,article,name,quantity,cell,date,country,customsCode

Битые строки (неверный тип, неверное число полей, ошибка парсинга
количества или даты) ПРОПУСКАЮТСЯ — не прерывают загрузку.

Поддерживаются кавычки и экранирование «» в стиле Excel/CSV.
*/
public final class CSVUtil {

    private CSVUtil() { /* утилита */ }

    // Результат загрузки.
    public static final class LoadResult {
        public final List<Party> parties;
        public final int totalLines;
        public final int skipped;

        LoadResult(List<Party> parties, int totalLines, int skipped) {
            this.parties = parties;
            this.totalLines = totalLines;
            this.skipped = skipped;
        }
    }

    /*
     Загружает партии из CSV-файла. Битые строки пропускаются
     (подсчитываются в LoadResult.skipped).
     */
    public static LoadResult load(String filePath) throws IOException {
        List<Party> parties = new ArrayList<>();
        int totalLines = 0;
        int skipped = 0;

        Path path = Paths.get(filePath);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                if (line.trim().isEmpty()) {
                    skipped++;
                    continue;
                }
                String[] parts = parseLine(line);
                Party p;
                try {
                    p = Party.fromCsvRow(parts);
                } catch (Exception ex) {
                    p = null;
                }
                if (p == null) {
                    skipped++;
                    continue;
                }
                parties.add(p);
            }
        }
        return new LoadResult(parties, totalLines, skipped);
    }

    /* Сохраняет список партий в CSV-файл. */
    public static void save(String filePath, List<Party> parties) throws IOException {
        Path path = Paths.get(filePath);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (Party p : parties) {
                String[] row = p.toCsvRow();
                writer.write(toLine(row));
                writer.write(System.lineSeparator());
            }
        }
    }

    /* Простой CSV-парсер с поддержкой кавычек и запятых внутри поля. */
    static String[] parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); // экранированная кавычка
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

    /* Сериализует массив полей в одну CSV-строку, экранируя при необходимости. */
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