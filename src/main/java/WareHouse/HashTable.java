package WareHouse;

import PartyModel.AppConstants;

import java.util.ArrayList;
import java.util.List;

public class HashTable<K, V> {

    private static final int    DEFAULT_CAPACITY         = AppConstants.HASH_INITIAL_CAPACITY;
    private static final double LOAD_FACTOR_THRESHOLD    = AppConstants.HASH_LOAD_FACTOR_THRESHOLD;

    private Object[] keys;
    private Object[] values;
    private boolean[] deleted;
    private int size;

    private long putProbesTotal;    private long putOps;
    private long getProbesTotal;    private long getOps;
    private long deleteProbesTotal; private long deleteOps;

    public HashTable() { this(DEFAULT_CAPACITY); }

    public HashTable(int capacity) {
        if (capacity < 1) capacity = DEFAULT_CAPACITY;
        keys = new Object[capacity];
        values = new Object[capacity];
        deleted = new boolean[capacity];
    }

    public record PutResult(boolean isNew, int probes, int[] probedIndices) {}
    public record GetResult<V>(boolean found, V value, int probes, int[ ]probedIndices) {}
    public record DeleteResult(boolean found, int probes, int[] probedIndices) {}

    private int indexFor(Object key, int capacity) {
        int h = key.hashCode();
        h ^= (h >>> 16);
        return (h & 0x7fffffff) % capacity;
    }

    public synchronized PutResult put(K key, V value) {
        if (key == null) throw new NullPointerException("key");
        if ((double) (size + 1) / keys.length > LOAD_FACTOR_THRESHOLD) {
            resize(keys.length * 2);
        }
        int capacity = keys.length;
        int idx = indexFor(key, capacity);
        int [] tempProbed  = new int[capacity];
        int probes = 0;
        int firstTombstone = -1;

        for (int i = 0; i < capacity; i++) {
            int pos = (idx + i) % capacity;
            tempProbed[++probes] = pos;
            if (keys[pos] == null && !deleted[pos]) {
                int insertPos = firstTombstone != -1 ? firstTombstone : pos;
                keys[insertPos] = key;
                values[insertPos] = value;
                deleted[insertPos] = false;
                size++;
                putProbesTotal += probes;
                putOps++;

                int[] finalProbed = new int[probes];
                System.arraycopy(tempProbed, 0, finalProbed, 0, probes);
                return new PutResult(true, probes, finalProbed);
            }
            if (deleted[pos]) {
                if (firstTombstone == -1) firstTombstone = pos;
                continue;
            }
            if (keys[pos].equals(key)) {
                values[pos] = value;
                putProbesTotal += probes;
                putOps++;
                int[] finalProbed = new int[probes];
                return new PutResult(false, probes, finalProbed);
            }
        }
        throw new IllegalStateException("HashTable is full, but load factor check failed."); //Если дошли - таблица переполнена
    }

    @SuppressWarnings("unchecked")
    public synchronized GetResult<V> get(K key) {
        if (key == null) return new GetResult<>(false, null, 0, new int [0]);
        int capacity = keys.length;
        int idx = indexFor(key, capacity);
        int probes = 0;
        boolean found = false;
        V foundValue = null;

        int[] tempProbed = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            int pos = (idx + i) % capacity;
            tempProbed[probes] = pos;
            probes++;


            if (keys[pos] == null && !deleted[pos]) {
                break;
            }
            if (!deleted[pos] && keys[pos].equals(key)) {
                found = true;
                foundValue = (V) values[pos];
                break;
            }
        }
        getProbesTotal += probes; getOps++;
        int[] finalProbed = new int[probes];
        System.arraycopy(tempProbed, 0, finalProbed, 0, probes);
        return new GetResult<>(found, foundValue, probes, finalProbed);
    }

    public synchronized DeleteResult delete(K key) {
        if (key == null) return new DeleteResult(false, 0, new int [0]);
        boolean found = false;
        int capacity = keys.length;
        int idx = indexFor(key, capacity);
        int probes = 0;
        int[] tempProbed = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            probes++;
            int pos = (idx + i) % capacity;
            tempProbed[probes] = pos;
            if (keys[pos] == null && !deleted[pos]) {
                break;
            }
            if (!deleted[pos] && keys[pos].equals(key)) {
                keys[pos] = null;
                values[pos] = null;
                deleted[pos] = true;
                size--;
                found = true;
                deleteProbesTotal += probes; deleteOps++;
                break;
            }
        }
        deleteProbesTotal += probes; deleteOps++;
        int[] finalProbed = new int[probes];
        System.arraycopy(tempProbed, 0, finalProbed, 0, probes);
        return new DeleteResult(found, probes, finalProbed);
    }

    public synchronized boolean containsKey(K key) { return get(key).found(); }

    @SuppressWarnings("unchecked")
    private synchronized void resize(int newCapacity) {
        Object[] oldKeys = keys;
        Object[] oldValues = values;

        keys = new Object[newCapacity];
        values = new Object[newCapacity];
        deleted = new boolean[newCapacity];
        size = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null) put((K) oldKeys[i], (V) oldValues[i]);
        }
    }

    public synchronized int capacity()   { return keys.length; }
    public synchronized int size()       { return size; }
    public synchronized double loadFactor() { return capacity() == 0 ? 0 : (double) size / capacity(); }

    public synchronized void clear() {
        keys = new Object[DEFAULT_CAPACITY];
        values = new Object[DEFAULT_CAPACITY];
        deleted = new boolean[DEFAULT_CAPACITY];
        size = 0;
        putProbesTotal = putOps = getProbesTotal = getOps = deleteProbesTotal = deleteOps = 0;
    }

    public synchronized double avgPutProbes()    { return putOps    == 0 ? 0 : (double) putProbesTotal    / putOps; }
    public synchronized double avgGetProbes()    { return getOps    == 0 ? 0 : (double) getProbesTotal    / getOps; }
    public synchronized double avgDeleteProbes() { return deleteOps == 0 ? 0 : (double) deleteProbesTotal / deleteOps; }

    @SuppressWarnings("unchecked")
    public synchronized List<V> values() {
        List<V> result = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && !deleted[i]) result.add((V) values[i]);
        }
        return result;
    }
    //Immutable snapshot для отрисовки
    public static class TableSnapshot {
        public final Object[] keys;
        public final Object[] values;
        public final boolean[] deleted;
        public final int size;
        public final int capacity;

        public TableSnapshot(Object[] k, Object[] v, boolean[] d, int s, int c) {
            // .clone() создает поверхностную копию массива. Это безопасно для примитивов (boolean)
            // и ссылок (Object), так как мы только читаем состояние для отрисовки.
            this.keys = k.clone();
            this.values = v.clone();
            this.deleted = d.clone();
            this.size = s;
            this.capacity = c;
        }
    }

    // Метод в HashTable для получения снимка
    public synchronized TableSnapshot getSnapshot() {
        return new TableSnapshot(keys, values, deleted, size, keys.length);
    }
}