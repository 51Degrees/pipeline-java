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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class LruPutCacheTests {

    @Test
    public void LruPutCache_Get() {
        PutCache<Integer, String> cache = null;
        cache = new LruPutCache.Builder().build(cache, 2);

        cache.put(1, "test");
        String result = cache.get(1);

        assertEquals("test", result);
    }

    @Test
    public void LruPutCache_NoValue() {
        PutCache<Integer, String> cache = null;
        cache = new LruPutCache.Builder().build(cache, 2);

        String result = cache.get(1);

        assertNull(result);
    }

    @Test
    public void LruPutCache_LruPolicyCheck() {
        // Set cache size to 2 with only 1 list
        PutCache<Integer, String> cache = null;
        cache = new LruPutCache.Builder()
            .setConcurrency(1)
            .build(cache, 2);

        // Add three items in a row.
        cache.put(1, "test1");
        cache.put(2, "test2");
        cache.put(3, "test3");
        String result1 = cache.get(1);
        String result2 = cache.get(2);
        String result3 = cache.get(3);

        // The oldest item should have been evicted.
        assertNull(result1);
        assertEquals("test2", result2);
        assertEquals("test3", result3);
    }

    @Test
    public void LruPutCache_LruPolicyCheck2() {
        // Set cache size to 2 with only 1 list
        PutCache<Integer, String> cache = null;
        cache = new LruPutCache.Builder()
            .setConcurrency(1)
            .build(cache, 2);

        // Add two items.
        cache.put(1, "test1");
        cache.put(2, "test2");
        // Access the first one.
        cache.get(1);
        // Add a third item.
        cache.put(3, "test3");
        String result1 = cache.get(1);
        String result2 = cache.get(2);
        String result3 = cache.get(3);

        // The second item should have been evicted.
        assertEquals("test1", result1);
        assertNull(result2);
        assertEquals("test3", result3);
    }

    @Test
    public void LruPutCache_HighConcurrency() throws ExecutionException, InterruptedException {

        ExecutorService service = Executors.newFixedThreadPool(50);

        // Create a cache that can hold 100 items
        PutCache<Integer, String> cache = null;
        cache = new LruPutCache.Builder()
            .build(cache, 100);

        // Create a queue with 1 million random key values from 0 to 199.
        Random rnd = new Random();
        final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
        int totalRequests = 1000000;
        for (int i = 0; i < totalRequests; i++) {
            queue.add(rnd.nextInt(200));
        }

        // Create 50 tasks that will read from the queue of keys and
        // query the cache to retrieve the data.
        List<Runnable> runnables = new ArrayList<>();
        final AtomicInteger hits = new AtomicInteger(0);
        for (int i = 0; i < 50; i++) {
            final PutCache<Integer, String> finalCache = cache;
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    Integer key = queue.poll();
                    while (key != null) {
                        String result = finalCache.get(key);
                        if (result == null) {
                            // If the data is not present then add it.
                            finalCache.put(key, "test" + key);
                        } else {
                            // If the data is present then make sure
                            // it's correct.
                            assertEquals("test" + key, result);
                            hits.incrementAndGet();
                        }
                        key = queue.poll();
                    }
                }
            });
        }

        // Start all the tasks as simultaneously as we can.
        List<Future<?>> futures = new ArrayList<>();
        for (Runnable runnable : runnables) {
            futures.add(service.submit(runnable));
        }

        // Wait for all the tasks to finish.
        // Check that all the tasks completed successfully.
        for (Future<?> future : futures) {
            future.get();
            assertTrue(future.isDone() && future.isCancelled() == false);
        }
        // Check that there were a reasonable number of hits.
        // It should be approx 50% but is random so we leave a large
        // margin of error and go for 10%.
        // If it's below this then something is definitely wrong.
        assertTrue(
            "Expected number of cache hits to be at least 10% but was " +
                "actually " + (((float) hits.get() / totalRequests) * 100) + "%",
            hits.get() > totalRequests / 10);

        service.shutdown();
    }

    /**
     * Check that a cache configured to not replace existing items does not do
     * it if an item with an existing key is added.
     */
    @Test
    public void LruPutCache_DontReplace() {
        // Create a cache. Use a size of two to rule out the case where the
        // second add removes the first by the LRU rules
        PutCache<Integer, String> cache = null;
        cache = new LruPutCache.Builder()
            .setUpdateExisting(false)
            .build(cache, 2);
        // Add an item to the cache
        cache.put(1, "test");
        // Add another item to the cache using the same key
        cache.put(1,  "replacement");
        // Get
        String result = cache.get(1);
        // Check
        assertEquals(
            "The existing value was overwritten in the cache",
            "test", result);
    }

    /**
     * Check that a cache configured to not replace existing items does so if an
     * item with an existing key is added.
     */
    @Test
    public void LruPutCache_Replace() {
        // Create a cache. Use a size of two to rule out the case where the
        // second add removes the first by the LRU rules
        PutCache<Integer, String> cache = null;
        cache = new LruPutCache.Builder()
            .setUpdateExisting(true)
            .build(cache, 2);
        // Add an item to the cache
        cache.put(1, "test");
        // Add another item to the cache using the same key
        cache.put(1,  "replacement");
        // Get
        String result = cache.get(1);
        // Check
        assertEquals(
            "The existing value was not overwritten in the cache",
            "replacement", result);
    }
}
