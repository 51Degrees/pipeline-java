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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.engines.exceptions.LazyLoadTimeoutException;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyResult;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import fiftyone.pipeline.exceptions.AggregateException;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;

import static fiftyone.pipeline.util.CheckArgument.checkNotNull;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Abstract base class for {@link AspectData} which overrides the
 * {@link AspectData#get(String) get} method to add null checks and determine
 * the reason for a value not being present.
 */
public abstract class AspectDataBase extends ElementDataBase implements AspectData {

    private MissingPropertyService missingPropertyService;

    private List<AspectEngine> engines;

    private Map<AspectEngine, Future<?>> processFutures;

    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine engine) {
        this(logger, flowData, engine, null);
    }

    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine engine,
        MissingPropertyService missingPropertyService) {
        super(logger, flowData);
        this.engines = new ArrayList<>();
        this.engines.add(checkNotNull(engine, "Engine must not be null"));
        this.processFutures = new HashMap<>();
        this.missingPropertyService = missingPropertyService;
    }

    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine engine,
        MissingPropertyService missingPropertyService,
        Map<String, Object> map) {
        super(logger, flowData, map);
        this.engines = new ArrayList<>();
        this.engines.add(checkNotNull(engine, "Engine must not be null"));
        this.processFutures = new HashMap<>();
        this.missingPropertyService = missingPropertyService;
    }

    @Override
    public List<AspectEngine> getEngines() {
        return Collections.unmodifiableList(engines);
    }

    @Override
    public Future<?> getProcessFuture() {
        return new Future<Object>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean result = true;
                for (Future<?> future : processFutures.values()) {
                    result = future.cancel(mayInterruptIfRunning) && result;
                }
                return result;
            }

            @Override
            public boolean isCancelled() {
                for (Future<?> future : processFutures.values()) {
                    if (future.isCancelled()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean isDone() {
                for (Future<?> future : processFutures.values()) {
                    if (future.isDone() == false) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                for (Future<?> future : processFutures.values()) {
                    future.get();
                }
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                for (Future<?> future : processFutures.values()) {
                    future.get(timeout, unit);
                }
                return null;
            }
        };
    }

    public void addEngine(AspectEngine engine) {
        engines.add(engine);
    }

    public void addProcessCallable(ProcessCallable runnable) {
        Future<?> future = runnable.engine.getExecutor().submit(runnable);
        processFutures.put(
            runnable.engine,
            future);
    }

    /**
     * Gets the value stored using the specified key with full checks
     * against the {@link MissingPropertyService}.
     *
     * @param propertyName to get the value for
     * @return value of the property
     * @throws PropertyMissingException the property was not found
     */
    @Override
    public Object get(String propertyName) throws PropertyMissingException {
        return getAs(propertyName, Object.class);
    }

    @Override
    protected <T> T getAs(String key, Class<T> type, Class<?>... parameterisedTypes) {
        checkNotNull(key, "Key cannot be null.");
        if (logger.isDebugEnabled()) {
            logger.debug("AspectData '" + getClass().getSimpleName() +
                "'-'" + hashCode() + "' property value requested for key '" +
                key + "'.");
        }
        TryGetResult<T> result;
        List<FlowError> errors = null;
        if (anyLazyLoaded(engines) == false ||
            (errors = waitOnAllProcessFutures()).size() == 0) {
            result = tryGetValue(key, type, parameterisedTypes);
            if (result.hasValue() == false &&
                missingPropertyService != null) {
                // If there was no entry for the key then use the missing
                // property service to find out why.
                MissingPropertyResult missingReason = missingPropertyService
                    .getMissingPropertyReason(key, engines);
                logger.warn("Property '" + key + "' missing from " +
                    "aspect data '" + getClass().getName() + "'-'" +
                    hashCode() + "'. " + missingReason.getReason());
                throw new PropertyMissingException(
                    missingReason.getReason(),
                    key,
                    missingReason.getDescription());
            }
        } else {
            Throwable e = null;
            if (errors.size() == 1) {
                e = errors.get(0).getThrowable();
                if (e instanceof CancellationException) {
                    // The property is being lazy loaded but been canceled, so
                    // pass the exception up.
                    throw (CancellationException)e;
                }
                else if (e instanceof TimeoutException) {
                    // The property is being lazy loaded but has timed out
                    // or been canceled so throw the appropriate exception.
                    throw new LazyLoadTimeoutException(
                        "Failed to retrieve property '" + key + "' " +
                            "because the processing for engine(s) " +
                            stringJoin(getDistinctEngineNames(), ", ") +
                            " took longer than the specified timeout.",
                        e);
                }
                else {
                    // The property is being lazy loaded but an error
                    // occurred in the engine's process method
                    throw new RuntimeException(
                        "Failed to retrieve property '" + key + "' " +
                        "because processing threw an exception in engine(s) " +
                        stringJoin(getDistinctEngineNames(), ", ") + ".",
                        e);
                }
            }
            else {
                // The property is being lazy loaded but multiple errors have
                // occurred in the engine's process method
                throw new AggregateException(
                    "Failed to retrieve property '" + key + "' " +
                    "because processing threw multiple exceptions in engine(s) " +
                    stringJoin(getDistinctEngineNames(), ", ") + ".",
                    errors);
            }
        }
        return result.getValue();

    }

    protected <T> TryGetResult<T> tryGetValue(String key, Class<T> type, Class<?>... parameterisedTypes) {
        TryGetResult<T> result = new TryGetResult<>();
        Map<String, Object> map = asKeyMap();
        if (map.containsKey(key)) {
            Object obj = asKeyMap().get(key);

            try {
                T value = type.cast(obj);
                result.setValue(value);
            } catch (ClassCastException e) {
                throw new ClassCastException("Expected property '" + key +
                    "' to be of type '" + type.getSimpleName() +
                    "' but it is '" + obj.getClass().getSimpleName() + "'");
            }
        }
        return result;
    }

    private static boolean anyLazyLoaded(List<AspectEngine> engines) {
        for (AspectEngine engine : engines) {
            if (engine.getLazyLoadingConfiguration() != null) {
                return true;
            }
        }
        return false;
    }

    private List<FlowError> waitOnAllProcessFutures() {
        List<FlowError> errors = new ArrayList<>();
        for (Map.Entry<AspectEngine, Future<?>> entry :
            processFutures.entrySet()) {
            try {
                entry.getValue().get(
                    entry.getKey().getLazyLoadingConfiguration().getPropertyTimeoutMillis(),
                    TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                errors.add(new FlowError.Default(e, entry.getKey()));
            }
        }
        return errors;
    }

    private List<String> getDistinctEngineNames() {
        List<String> strings = new ArrayList<>();
        for (AspectEngine engine : processFutures.keySet()) {
            if (strings.contains(engine.getClass().getName()) == false) {
                strings.add(engine.getClass().getName());
            }
        }
        return strings;
    }
}
