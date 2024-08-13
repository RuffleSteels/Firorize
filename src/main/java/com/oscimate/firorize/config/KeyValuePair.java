package com.oscimate.firorize.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class KeyValuePair<K, V> implements Serializable {
    private K key;
    private V value;

    public KeyValuePair() {}

    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        KeyValuePair<?, ?> that = (KeyValuePair<?, ?>) obj;


        return areValuesEqual(this.key, that.key) && areValuesEqual(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }


    private boolean areValuesEqual(Object value1, Object value2) {
        if (value1 instanceof int[] && value2 instanceof int[]) {
            return Arrays.equals((int[]) value1, (int[]) value2);
        } else if (value1 instanceof ArrayList<?> && value2 instanceof ArrayList<?>) {
            return Objects.equals(value1, value2);
        } else if (value1 instanceof KeyValuePair<?, ?> && value2 instanceof KeyValuePair<?, ?>) {
            return value1.equals(value2);
        }

        return Objects.equals(value1, value2);
    }
    public KeyValuePair(KeyValuePair<K, V> keyValuePair) {
        this.key = keyValuePair.getLeft();
        this.value = keyValuePair.getRight();
    }

    public static <K, V> KeyValuePair<K, V> of(K key, V value) {
        return new KeyValuePair<>(key, value);
    }

    public K getLeft() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getRight() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "KeyValuePair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}