package WareHouse;

import PartyModel.Party;

import java.util.Collection;
import java.util.Objects;

/**
 * Склад — обёртка над хеш-таблицей «артикул, партия».
 * Предоставляет операции put / get / delete, возвращающие результаты
 * со статистикой проб, а также общую сводку по ёмкости и заполненности.
 */
public class WareHouse {

    private final HashTable<Object, Party> table = new HashTable<Object, Party>();

    public HashTable.PutResult put(Party p) {
        Objects.requireNonNull(p, "party");
        String article = p.getArticle();
        if (article == null || article.isBlank()) {
            throw new IllegalArgumentException("Артикул обязателен");
        }
        return table.put(article, p);
    }

    public HashTable.GetResult<Party> get(String article) {
        return table.get(article);
    }

    public HashTable.DeleteResult delete(String article) {
        return table.delete(article);
    }

    public boolean contains(String article) {
        return table.containsKey(article);
    }

    public Collection<Party> all() {
        return table.values();
    }

    public void clear() {
        table.clear();
    }

    public int size() {
        return table.size();
    }

    public int capacity() {
        return table.capacity();
    }

    public double loadFactor() {
        return table.loadFactor();
    }

    public double avgPutProbes() {
        return table.avgPutProbes();
    }

    public double avgGetProbes() {
        return table.avgGetProbes();
    }

    public double avgDeleteProbes() {
        return table.avgDeleteProbes();
    }

    @Override
    public String toString() {
        return "Warehouse{size=" + size() + "}";
    }
}