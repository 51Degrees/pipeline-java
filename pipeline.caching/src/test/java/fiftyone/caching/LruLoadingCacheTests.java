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

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class LruLoadingCacheTests {

    @Test
    @SuppressWarnings("unchecked")
    public void LruLoadingCache_Get() throws IOException {
        ValueLoader<Integer, String> loader = mock(ValueLoader.class);
        // Configure the loader to return 'test' for key '1'.
        when(loader.load(eq((Integer) 1))).thenReturn("test");
        // Create the cache, passing in the loader.
        LruLoadingCache<Integer, String> cache =
            new LruLoadingCache<>(2, loader);

        String result = cache.get(1);

        assertEquals("test", result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void LruLoadingCache_Get2() throws IOException {
        ValueLoader<Integer, String> loader = mock(ValueLoader.class);
        // Configure the loader to return 'test' for key '1'.
        when(loader.load(eq((Integer) 1))).thenReturn("test");
        LruLoadingCache<Integer, String> cache =
            new LruLoadingCache<>(2);

        // Access the cache, passing in the loader.
        String result = cache.get(1, loader);

        assertEquals("test", result);
    }
}
