package WareHouse;

import CSV.CSVLoadException;
import CSV.CSVUtil;
import PartyModel.AppConstants;
import PartyModel.Party;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервисный слой. Вся бизнес-логика — здесь, GUI её не дублирует.
 * Тяжёлые методы (loadFromFile / saveToFile) вызывать НЕ на UI-потоке.
 */
public class WarehouseService {

    private final WareHouse warehouse;

    public WarehouseService(WareHouse warehouse) {
        this.warehouse = warehouse;
    }

    public record LoadSummary(int loaded, int duplicates,
                              List<CSVLoadException> errors, String filePath) {
        public int totalSkipped()  { return errors.size() + duplicates; }
        public boolean hasIssues() { return !errors.isEmpty() || duplicates > 0; }

        public Map<CSVLoadException.ErrorCode, Integer> errorBreakdown() {
            return errors.stream().collect(Collectors.groupingBy(
                    CSVLoadException::getCode,
                    Collectors.summingInt(e -> 1)));
        }
    }

    public record AddResult(Party party, int probes, boolean wasDuplicate) {}
    public record SaveResult(int saved, String filePath) {}
    public record DeleteOutcome(String article, boolean deleted, int probes) {}

    public LoadSummary loadFromFile(File file) throws IOException {
        // Сначала грузим, только потом трогаем склад — если файл битый,
        // текущее состояние остаётся нетронутым.
        CSVUtil.LoadResult result = CSVUtil.load(file.getAbsolutePath());

        warehouse.clear();
        int dups = 0;
        for (Party p : result.parties()) {
            if (warehouse.get(p.getArticle()).found()) {
                dups++;
            } else {
                warehouse.put(p);
            }
        }
        return new LoadSummary(warehouse.size(), dups, result.errors(), file.getAbsolutePath());
    }

    public SaveResult saveToFile(File file) throws IOException {
        String path = ensureCsvExtension(file.getAbsolutePath());
        CSVUtil.save(path, new ArrayList<>(warehouse.all()));
        return new SaveResult(warehouse.size(), path);
    }

    public AddResult addParty(Party party) {
        if (warehouse.get(party.getArticle()).found()) {
            return new AddResult(party, 0, true);
        }
        var r = warehouse.put(party);
        return new AddResult(party, r.probes(), false);
    }

    public AddResult updateParty(Party party) {
        var r = warehouse.put(party);
        return new AddResult(party, r.probes(), false);
    }

    public DeleteOutcome deleteByArticle(String article) {
        var r = warehouse.delete(article);
        return new DeleteOutcome(article, r.found(), r.probes());
    }

    public List<Party> all()  { return new ArrayList<>(warehouse.all()); }
    public int size()         { return warehouse.size(); }
    public int capacity()     { return warehouse.capacity(); }
    public double loadFactor(){ return warehouse.loadFactor(); }
    public double avgPutProbes()    { return warehouse.avgPutProbes(); }
    public double avgGetProbes()    { return warehouse.avgGetProbes(); }
    public double avgDeleteProbes() { return warehouse.avgDeleteProbes(); }

    private String ensureCsvExtension(String path) {
        return path.toLowerCase(Locale.ROOT).endsWith(AppConstants.CSV_EXTENSION)
                ? path : path + AppConstants.CSV_EXTENSION;
    }
}