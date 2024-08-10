package com.oscimate.oscimate_soulflame.config;

import java.io.Serializable;

public class KeyValuePair<K, V> implements Serializable {
    private K key;
    private V value;

    public KeyValuePair() {}

    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
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