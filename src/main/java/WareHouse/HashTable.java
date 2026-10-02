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

    public record PutResult(boolean isNew, int probes) {}
    public record GetResult<V>(boolean found, V value, int probes) {}
    public record DeleteResult(boolean found, int probes) {}

    private int indexFor(Object key, int capacity) {
        int h = key.hashCode();
        h ^= (h >>> 16);
        return (h & 0x7fffffff) % capacity;
    }

    public PutResult put(K key, V value) {
        if (key == null) throw new NullPointerException("key");
        if ((double) (size + 1) / keys.length > LOAD_FACTOR_THRESHOLD) {
            resize(keys.length * 2);
        }

        int capacity = keys.length;
        int idx = indexFor(key, capacity);
        int probes = 0;
        int firstTombstone = -1;

        for (int i = 0; i < capacity; i++) {
            probes++;
            int pos = (idx + i) % capacity;

            if (keys[pos] == null && !deleted[pos]) {
                int insertPos = firstTombstone != -1 ? firstTombstone : pos;
                keys[insertPos] = key;
                values[insertPos] = value;
                deleted[insertPos] = false;
                size++;
                putProbesTotal += probes;
                putOps++;
                return new PutResult(true, probes);
            }
            if (deleted[pos]) {
                if (firstTombstone == -1) firstTombstone = pos;
                continue;
            }
            if (keys[pos].equals(key)) {
                values[pos] = value;
                putProbesTotal += probes;
                putOps++;
                return new PutResult(false, probes);
            }
        }
        resize(capacity * 2);
        return put(key, value);
    }

    @SuppressWarnings("unchecked")
    public GetResult<V> get(K key) {
        if (key == null) return new GetResult<>(false, null, 0);
        int capacity = keys.length;
        int idx = indexFor(key, capacity);
        int probes = 0;

        for (int i = 0; i < capacity; i++) {
            probes++;
            int pos = (idx + i) % capacity;

            if (keys[pos] == null && !deleted[pos]) {
                getProbesTotal += probes; getOps++;
                return new GetResult<>(false, null, probes);
            }
            if (!deleted[pos] && keys[pos].equals(key)) {
                getProbesTotal += probes; getOps++;
                return new GetResult<>(true, (V) values[pos], probes);
            }
        }
        getProbesTotal += probes; getOps++;
        return new GetResult<>(false, null, probes);
    }

    public DeleteResult delete(K key) {
        if (key == null) return new DeleteResult(false, 0);
        int capacity = keys.length;
        int idx = indexFor(key, capacity);
        int probes = 0;

        for (int i = 0; i < capacity; i++) {
            probes++;
            int pos = (idx + i) % capacity;

            if (keys[pos] == null && !deleted[pos]) {
                deleteProbesTotal += probes; deleteOps++;
                return new DeleteResult(false, probes);
            }
            if (!deleted[pos] && keys[pos].equals(key)) {
                keys[pos] = null;
                values[pos] = null;
                deleted[pos] = true;
                size--;
                deleteProbesTotal += probes; deleteOps++;
                return new DeleteResult(true, probes);
            }
        }
        deleteProbesTotal += probes; deleteOps++;
        return new DeleteResult(false, probes);
    }

    public boolean containsKey(K key) { return get(key).found(); }

    @SuppressWarnings("unchecked")
    private void resize(int newCapacity) {
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

    public int capacity()   { return keys.length; }
    public int size()       { return size; }
    public double loadFactor() { return capacity() == 0 ? 0 : (double) size / capacity(); }

    public void clear() {
        keys = new Object[DEFAULT_CAPACITY];
        values = new Object[DEFAULT_CAPACITY];
        deleted = new boolean[DEFAULT_CAPACITY];
        size = 0;
        putProbesTotal = putOps = getProbesTotal = getOps = deleteProbesTotal = deleteOps = 0;
    }

    public double avgPutProbes()    { return putOps    == 0 ? 0 : (double) putProbesTotal    / putOps; }
    public double avgGetProbes()    { return getOps    == 0 ? 0 : (double) getProbesTotal    / getOps; }
    public double avgDeleteProbes() { return deleteOps == 0 ? 0 : (double) deleteProbesTotal / deleteOps; }

    @SuppressWarnings("unchecked")
    public List<V> values() {
        List<V> result = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && !deleted[i]) result.add((V) values[i]);
        }
        return result;
    }
}