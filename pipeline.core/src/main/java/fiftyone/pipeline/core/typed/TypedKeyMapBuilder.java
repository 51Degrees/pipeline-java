/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.pipeline.core.typed;

import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.exceptions.PipelineDataException;

import java.io.Closeable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Public builder for instances of {@link TypedKeyMap}. This follows the fluent
 * builder pattern.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class TypedKeyMapBuilder {

    private final TypedKeyMap _map;

    /**
     * Construct a new instance.
     * @param isConcurrent true if the resulting {@link TypedKeyMap} should be
     *                     thread-safe to handle concurrent access
     */
    public TypedKeyMapBuilder(boolean isConcurrent) {
        if (isConcurrent) {
            _map = new TypedKeyMapConcurrent();
        } else {
            _map = new TypedKeyMapInternal();
        }
    }

    /**
     * Add an item to the map which will be built.
     * @param typedKey key to add the value with
     * @param value value to add
     * @param <T> type of value
     * @return this builder
     */
    public <T> TypedKeyMapBuilder put(TypedKey<T> typedKey, T value) {
        _map.put(typedKey, value);
        return this;
    }

    /**
     * Build the {@link TypedKeyMap} instance.
     * @return map instance
     */
    public TypedKeyMap build() {
        return this._map;
    }

    /**
     * Extends the internal implementation to add thread-safety to the
     * {@link #get(Object)} and {@link #put(String, Object)} methods.
     */
    private static class TypedKeyMapConcurrent extends TypedKeyMapInternal {
        @Override
        public <T> void put(TypedKey<T> typedKey, T value) {
            synchronized (this) {
                super.put(typedKey, value);
            }
        }

        @Override
        public <T> T removeIfExists(TypedKey<T> typedKey) {
            synchronized (this) {
                return super.removeIfExists(typedKey);
            }
        }
    }

    /**
     * Implementation of {@link TypedKeyMap} in which the underlying map is
     * provided by a singly linked list.
     */
    private static class TypedKeyMapInternal implements TypedKeyMap, Map<String, Object>, Closeable {

        private static final Entry[] emptyEntryPair = new Entry[]{null, null};
        private Entry _head = null;

        private Entry[] findWithPrevious(String typedKeyName) {
            Entry current = _head;
            if (current != null) {
                if (current._key.getName().equals(typedKeyName)) {
                    return new Entry[]{null, current};
                }
                while (current._next != null) {
                    if (current._next._key.getName().equals(typedKeyName)) {
                        return new Entry[]{current, current._next};
                    }
                    current = current._next;
                }
            }
            return emptyEntryPair;
        }

        private Entry find(String typedKeyName) {
            return findWithPrevious(typedKeyName)[1];
        }

        private String getKeyMissingMessage(String key) {
            return "There is no data for '" + key + "' against this " +
                "instance of '" + getClass().getSimpleName() +
                "'. Available keys are: " +
                stringJoin(getKeys(), ", ");
        }

        @Override
        public <T> T get(TypedKey<T> typedKey) {
            Entry current = find(typedKey.getName());
            if (current != null) {
                // Note - can throw java equivalent of invalid cast exception.
                return typedKey.getType().cast(current._value);
            }
            throw new NoSuchElementException(getKeyMissingMessage(typedKey.getName()));
        }

        @Override
        public <T> T get(Class<T> type) {

            List<Entry> matches = new ArrayList<>();
            Entry current = _head;
            while (current != null) {
                if (current._value.getClass().equals(type)) {
                    matches.add(current);
                }
                current = current._next;
            }

            if (matches.isEmpty()) {
                current = _head;
                while (current != null) {
                    if (type.isAssignableFrom(current._value.getClass())) {
                        matches.add(current);
                    }
                    current = current._next;
                }
            }

            if (matches.size() == 1) {
                return type.cast(matches.get(0)._value);
            } else if (matches.size() == 0) {
                throw new PipelineDataException(
                    "This map contains no data matching type " +
                        "'" + type.getSimpleName() + "'");
            } else {
                throw new PipelineDataException("This map contains " +
                    "multiple data instances matching type '" +
                    type.getSimpleName() + "'");
            }
        }

        @Override
        public <T> TryGetResult<T> tryGet(TypedKey<T> key) {

            TryGetResult<T> result = new TryGetResult<>();

            try {
                Entry current = find(key.getName());
                if (current != null) {
                    T value = key.getType().cast(current._value);
                    result.setValue(value);
                }
            } catch (ClassCastException e) {
                // Don't do anything here.
            }

            return result;
        }

        @Override
        public <T> T removeIfExists(TypedKey<T> typedKey) {
            Entry[] current = findWithPrevious(typedKey.getName());
            if (current[1] != null) {
                if (current[0] != null) {
                    current[0]._next = current[1]._next;
                }
                if (current[1] == _head) {
                    _head = current[1]._next;
                }
                T value = (T) current[1]._value;
                current[1].closeValue();
                current[1]._value = null;
                return value;
            }
            return null;
        }

        @Override
        public <T> void put(TypedKey<T> typedKey, T value) {
            Entry current = find(typedKey.getName());
            if (current != null) {
                // A key already exists so overwrite the existing value with
                // the new one.
                if (current._value != null) {
                    current.closeValue();
                }
                current._value = value;
            } else {
                // The key does not exist so add it to the head of the linked
                // list.
                Entry<T> first = new Entry<>(typedKey, value);
                first._next = _head;
                _head = first;
            }
        }

        @Override
        public int size() {
            int count = 0;
            Entry current = _head;
            while (current != null) {
                count++;
                current = current._next;
            }
            return count;
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean containsKey(Object key) {
            return containsKey((String) key);
        }

        @Override
        public boolean containsValue(Object value) {
            return values().contains(value);
        }

        @Override
        public Object get(Object key) {
            return this.get(new TypedKeyDefault<>((String) key, Object.class));
        }

        @Override
        public Object put(String key, Object value) {
            put(new TypedKeyDefault<>(key, Object.class), value);
            return null;
        }

        @Override
        public Object remove(Object key) {
            removeIfExists(new TypedKeyDefault<>((String) key, Object.class));
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            for (Map.Entry<? extends String, ?> entry : m.entrySet()) {
                this.put(new TypedKeyDefault<>(entry.getKey(), Object.class), entry.getValue());
            }
        }

        @Override
        public void clear() {
            for (String key : keySet()) {
                remove(key);
            }
        }

        @Override
        public Set<String> keySet() {
            Entry current = _head;
            Set<String> set = new HashSet<>();
            while (current != null) {
                set.add(current._key.getName());
                current = current._next;
            }
            return set;

        }

        @Override
        public Collection<Object> values() {
            Entry current = _head;
            Set<Object> set = new HashSet<>();
            while (current != null) {
                set.add(current._value);
                current = current._next;
            }
            return set;
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            Entry current = _head;
            Set<Map.Entry<String, Object>> set = new HashSet<>();
            while (current != null) {
                set.add(new AbstractMap.SimpleEntry<>(current._key.getName(), current._value));
                current = current._next;
            }
            return set;
        }

        @Override
        public List<String> getKeys() {
            Entry current = _head;
            List<String> keys = new ArrayList<>();
            while (current != null) {
                keys.add(current._key.getName());
                current = current._next;
            }
            return keys;
        }

        @Override
        public Map<String, Object> asStringKeyMap() {
            return this;
        }

        @Override
        public boolean containsKey(String key) {
            return find(key) != null;
        }

        @Override
        public <T> boolean containsKey(TypedKey<T> typedKey) {
            boolean result = false;
            Entry value = find(typedKey.getName());
            if (value != null &&
                typedKey.getType().isAssignableFrom(value._value.getClass())) {
                result = true;
            }
            return result;
        }

        @Override
        public void close() {
            Entry current = _head;
            while (current != null) {
                current.closeValue();
                current = current._next;
            }
        }

        private static class Entry<T> {
            final TypedKey<T> _key;
            T _value;
            Entry _next = null;

            Entry(TypedKey<T> key, T value) {
                _key = key;
                _value = value;
            }

            public void closeValue() {
                try {
                    if (_value instanceof AutoCloseable) {
                        ((AutoCloseable) _value).close();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(TypedKeyMapBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}