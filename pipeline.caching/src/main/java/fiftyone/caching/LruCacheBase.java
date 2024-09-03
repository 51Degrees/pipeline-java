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

package fiftyone.caching;

import java.io.Closeable;
import java.lang.reflect.Array;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is a Least Recently Used (LRU) cache with multiple linked lists
 * in place of the usual single linked list.
 *
 * The linked list to use is assigned at random and stored in the cached
 * item. This will generate an even set of results across the different
 * linked lists. The approach reduces the probability of the same linked
 * list being locked when used in a environments with a high degree of
 * concurrency. If the feature is not required then the constructor should be
 * provided with a concurrency value of 1 so that a single linked list
 * is used.
 * 
 * Although this cache is written to be very generic, the primary use-case
 * is to provide a result cache for Aspect Engines in the Pipeline API.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/caching.md#cache-implementation">Specification</a>
 * 
 * @param <K> key for the cache items
 * @param <V> value for the cache items
 */
@SuppressWarnings("unused")
public abstract class LruCacheBase<K, V> implements Cache<K,V>, Closeable {

    /**
     * A array of doubly linked lists. Not marked private so that the unit
     * test can guard the elements.
     */
    final CacheLinkedList[] linkedLists;
    /**
     * Random number generator used to select the linked list to use with
     * the new item being added to the cache.
     */
    final Random random = new Random();
    /**
     * Hash map of keys to item values.
     */
    private final ConcurrentHashMap<K, CachedItem> hashMap;
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong requests = new AtomicLong(0);
    private boolean closed = false;
    private final Object lock = new Object();
    private int cacheSize;
    private final boolean updateExisting;

    /**
     * Constructs a new instance of the cache.
     *
     * @param cacheSize the number of items to store in the cache
     * @param concurrency the expected concurrent accesses to the cache
     * @param updateExisting true if existing items should be replaced
     */
    LruCacheBase(int cacheSize, int concurrency, boolean updateExisting) {
        if (concurrency <= 0) {
            throw new IllegalArgumentException(
                "Concurrency must be a positive integer greater than 0.");
        }
        this.cacheSize = cacheSize;
        this.updateExisting = updateExisting;
        this.hashMap = new ConcurrentHashMap<>(cacheSize);
        @SuppressWarnings("unchecked")
        CacheLinkedList[] linkedListsUnchecked =
            (CacheLinkedList[]) Array.newInstance(
                CacheLinkedList.class,
                concurrency);
        linkedLists = linkedListsUnchecked;
        for (int i = 0; i < linkedLists.length; i++) {
            linkedLists[i] = new CacheLinkedList(this);
        }
    }

    /**
     * The number of items the cache lists should have capacity for.
     *
     * @return capacity of the cache.
     */
    public long getCacheSize() {
        return cacheSize;
    }

    /**
     * @return number of cache misses.
     */
    public long getCacheMisses() {
        return misses.get();
    }

    /**
     * @return number of requests received by the cache.
     */
    public long getCacheRequests() {
        return requests.get();
    }

    /**
     * @return the percentage of times cache request did not return a result.
     */
    public double getPercentageMisses() {
        return misses.doubleValue() / requests.doubleValue();
    }

    @Override
    public V get(K key) {
        requests.incrementAndGet();
        // First, try to get the item from the hashMap
        CachedItem node = hashMap.get(key);
        if (node == null) {
            misses.incrementAndGet();
            return null;
        }
        else {
            // The item is in the dictionary.
            // Move the item to the head of it's LRU list.
            node.list.moveFirst(node);
        }

        return node.value;
    }

    protected CachedItem add(K key, V value) {
        // Get a randomly selected linked list to add
        // the item to.
        CachedItem newNode = new CachedItem(
            getRandomLinkedList(),
            key,
            value);

        // If the node has already been added to the dictionary
        // then get it, otherwise add the one just fetched.
        CachedItem node = hashMap.putIfAbsent(key, newNode);

        // If the node was absent and was added to the dictionary (node == null)
        // then it needs to be added to the linked list.
        if (node == null) {
            newNode.list.addNew(newNode);
            node = newNode;
        }
        else {
            if (updateExisting) {
                newNode.list.replace(node, newNode);
                node = newNode;
            }
            // The item is in the dictionary.
            // Move the item to the head of it's LRU list.
            node.list.moveFirst(node);
        }
        return node;
    }

    /**
     * Resets the 'stats' for the cache.
     */
    public void resetCache() {
        this.hashMap.clear();
        misses.set(0);
        requests.set(0);
        for (CacheLinkedList linkedList : linkedLists) {
            linkedList.clear();
        }
    }

/*
    @SuppressWarnings("deprecation")
    @Override
    public void finalize() {
        close(false);
    }
*/

    @Override
    public void close() {
        close(true);
    }

    protected void close(boolean closing) {
        // Clear the map and linked lists.
        if (closed == false) {
            synchronized (lock) {
                if (closed == false) {
                    hashMap.clear();
                    for (CacheLinkedList list : linkedLists) {
                        list.clear();
                    }
                }
                closed = true;
            }
        }
    }

    /**
     * Returns a random linked list.
     */
    private CacheLinkedList getRandomLinkedList() {
        return linkedLists[random.nextInt(linkedLists.length)];
    }

    /**
     * An item stored in the cache along with references to the next and
     * previous items.
     */
    class CachedItem {

        /**
         * Key associated with the cached item.
         */
        final K key;

        /**
         * Value of the cached item.
         */
        final V value;
        /**
         * The linked list the item is part of.
         */
        final CacheLinkedList list;
        /**
         * The next item in the linked list.
         */
        CachedItem next;
        /**
         * The previous item in the linked list.
         */
        CachedItem previous;
        /**
         * Indicates that the item is valid and added to the linked list.
         * It is not in the process of being manipulated by another thread
         * either being added to the list or being removed.
         */
        boolean isValid;

        public CachedItem(CacheLinkedList list, K key, V value) {
            this.list = list;
            this.key = key;
            this.value = value;
        }
    }

    /**
     * A linked list used in the LruCache implementation.
     * This linked list implementation enables items to be moved
     * within the linked list.
     */
    class CacheLinkedList {

        /**
         * The cache that the list is part of.
         */
        LruCacheBase<K, V> cache = null;

        /**
         * The first item in the list.
         */
        CachedItem first = null;

        /**
         * The last item in the list.
         */
        CachedItem last = null;

        /**
         * Constructs a new instance of the CacheLinkedList.
         */
        public CacheLinkedList(LruCacheBase<K,V> cache) {
            this.cache = cache;
        }

        /**
         * Adds a new cache item to the linked list.
         */
        void addNew(CachedItem item) {
            boolean added = false;
            if (item != first) {
                synchronized (this) {
                    if (item != first) {
                        if (first == null) {
                            // First item to be added to the queue.
                            first = item;
                            last = item;
                        } else {
                            // Add this item to the head of the linked list.
                            item.next = first;
                            first.previous = item;
                            first = item;

                            // Set flag to indicate an item was added and if
                            // the cache is full an item should be removed.
                            added = true;
                        }

                        // Indicate the item is now ready for another thread
                        // to manipulate and is fully added to the linked list.
                        item.isValid = true;
                    }
                }
            }

            // Check if the linked list needs to be trimmed as the cache
            // size has been exceeded.
            if (added && cache.hashMap.size() > cache.cacheSize) {
                synchronized (this) {
                    if (cache.hashMap.size() > cache.cacheSize) {
                        // Indicate that the last item is being removed from
                        // the linked list.
                        last.isValid = false;

                        // Remove the item from the dictionary before
                        // removing from the linked list.
                        cache.hashMap.remove(last.key);
                        last = last.previous;
                        last.next = null;
                    }
                }
            }
        }

        /**
         * Set the first item in the linked list to the item provided.
         */
        void moveFirst(CachedItem item) {
            if (item != first && item.isValid == true) {
                synchronized (this) {
                    if (item != first && item.isValid == true) {
                        if (item == last) {
                            // The item is the last one in the list so is
                            // easy to remove. A new last will need to be
                            // set.
                            last = item.previous;
                            last.next = null;
                        } else {
                            // The item was not at the end of the list.
                            // Remove it from it's current position ready
                            // to be added to the top of the list.
                            item.previous.next = item.next;
                            item.next.previous = item.previous;
                        }

                        // Add this item to the head of the linked list.
                        item.next = first;
                        item.previous = null;
                        first.previous = item;
                        first = item;
                    }
                }
            }
        }

        /**
         * Replace an existing item in the cache with a new value. The new
         * item must have the same key as the existing item.
         * @param oldItem existing item to replace
         * @param newItem new item to replace it with
         */
        void replace(CachedItem oldItem, CachedItem newItem) {
            if (oldItem.isValid) {
                synchronized (this) {
                    if (oldItem.isValid) {
                        newItem.previous = oldItem.previous;
                        newItem.next = oldItem.next;

                        if (newItem.previous == null) {
                            first = newItem;
                        }
                        else {
                            newItem.previous.next = newItem;
                        }

                        if (newItem.next == null) {
                            last = newItem;
                        }
                        else {
                            newItem.next.previous = newItem;
                        }

                        // Indicate the item is now ready for another thread
                        // to manipulate and is fully added to the linked list.
                        newItem.isValid = true;
                        oldItem.isValid = false;

                        cache.hashMap.replace(newItem.key, newItem);
                    }
                }
            }
        }

        /**
         * Clears all items from the linked list.
         */
        void clear() {
            first = null;
            last = null;
        }
    }
}