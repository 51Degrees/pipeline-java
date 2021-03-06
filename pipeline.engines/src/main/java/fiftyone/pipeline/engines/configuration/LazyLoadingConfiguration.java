/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
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

package fiftyone.pipeline.engines.configuration;

import fiftyone.pipeline.engines.exceptions.LazyLoadTimeoutException;

import java.util.concurrent.ExecutorService;

/**
 * Used to store configuration values relating to lazy loading.
 */
public class LazyLoadingConfiguration {
    private final int propertyTimeoutMillis;

    private final ExecutorServiceFactory factory;

    /**
     * Construct a new instance using the default {@link ExecutorServiceFactory}.
     * @param propertyTimeoutMillis the timeout in milliseconds to use when
     *                              waiting for processing to complete in order
     *                              to retrieve property values. If the timeout
     *                              is exceeded then a
     *                              {@link LazyLoadTimeoutException}
     *                              will be thrown
     */
    public LazyLoadingConfiguration(int propertyTimeoutMillis) {
        this(propertyTimeoutMillis, new ExecutorServiceFactoryDefault());
    }

    /**
     * Construct a new instance.
     * @param propertyTimeoutMillis the timeout in milliseconds to use when
     *                              waiting for processing to complete in order
     *                              to retrieve property values. If the timeout
     *                              is exceeded then a
     *                              {@link LazyLoadTimeoutException}
     *                              will be thrown
     * @param factory the {@link ExecutorServiceFactory} to use when processing
     *                in order to retrieve property values
     */
    public LazyLoadingConfiguration(
        int propertyTimeoutMillis,
        ExecutorServiceFactory factory) {
        this.propertyTimeoutMillis = propertyTimeoutMillis;
        this.factory = factory;
    }

    /**
     * Get the timeout in milliseconds to use when waiting for processing to
     * complete in order to retrieve property values. If the timeout is exceeded
     * then a {@link LazyLoadTimeoutException} will be thrown.
     * @return timeout in milliseconds
     */
    public int getPropertyTimeoutMillis() {
        return propertyTimeoutMillis;
    }

    /**
     * Get an {@link ExecutorService} to use when processing in order to
     * retrieve property values.
     * @return new {@link ExecutorService}
     */
    public ExecutorService getExecutorService() {
        return factory.create();
    }
}
