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
    import java.util.stream.Collectors;

    import static fiftyone.pipeline.util.Check.getNotNull;
    import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Abstract base class for {@link AspectData} which overrides the
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#aspect-engine">Specification</a>
 */
public abstract class AspectDataBase extends ElementDataBase implements AspectData {

    private final MissingPropertyService missingPropertyService;

    private final List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> engines;

    private final Map<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>, Future<?>> processFutures;

    /**
     * Constructs a new instance with a non-thread-safe, case-insensitive
     * {@link Map} as the underlying storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     */
    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        this(logger, flowData, engine, null);
    }

    /**
     * Constructs a new instance with a non-thread-safe, case-insensitive
     * {@link Map} as the underlying storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     * @param missingPropertyService service used to determine the reason for
     *                               a property value being missing
     */
    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine,
        MissingPropertyService missingPropertyService) {
        super(logger, flowData);
        this.engines = new ArrayList<>();
        this.engines.add(getNotNull(engine, "Engine must not be null"));
        this.processFutures = new HashMap<>();
        this.missingPropertyService = missingPropertyService;
    }

    /**
     * Constructs a new instance with a custom {@link Map} as the underlying
     * storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     * @param missingPropertyService service used to determine the reason for
     *                               a property value being missing
     * @param map the custom {@link Map} implementation to use as the underlying
     *            storage
     */
    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine,
        MissingPropertyService missingPropertyService,
        Map<String, Object> map) {
        super(logger, flowData, map);
        this.engines = new ArrayList<>();
        this.engines.add(getNotNull(engine, "Engine must not be null"));
        this.processFutures = new HashMap<>();
        this.missingPropertyService = missingPropertyService;
    }

    @Override
    public List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> getEngines() {
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
            public Object get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
                for (Future<?> future : processFutures.values()) {
                    future.get(timeout, unit);
                }
                return null;
            }
        };
    }

    /**
     * Add an engine to the list of engines which have generated the data within
     * this instance.
     * @param engine engine adding data
     */
    public void addEngine(AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine) {
        engines.add(engine);
    }

    /**
     * Add a callable which will run a {@link AspectEngine#process(FlowData)}
     * method to populate this instance. The property accessors will only
     * complete once all such tasks have completed.
     * @param runnable processing runnable
     */
    public void addProcessCallable(ProcessCallable runnable) {
        Future<?> future = runnable.engine.getExecutor().submit(runnable);
        processFutures.put(
            runnable.engine,
            future);
    }

    @Override
    public Map<String, Object> asKeyMap() {
        waitOnAllProcessFutures();
        return super.asKeyMap();
    }

    /**
     * Gets the value stored using the specified key with full checks
     * against the {@link MissingPropertyService}.
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
        getNotNull(key, "Key cannot be null.");
        if (logger.isDebugEnabled()) {
            logger.debug("AspectData '" + getClass().getSimpleName() +
                "'-'" + hashCode() + "' property value requested for key '" +
                key + "'.");
        }
        TryGetResult<T> result;
        List<FlowError> errors;
        if (anyLazyLoaded(engines) == false ||
            (errors = waitOnAllProcessFutures()).size() == 0) {
            result = tryGetValue(key, type, parameterisedTypes);
            if (result.hasValue() == false &&
                missingPropertyService != null) {
                // If there was no entry for the key then use the missing
                // property service to find out why.
                MissingPropertyResult missingReason = missingPropertyService
                    .getMissingPropertyReason(key, engines);
                logger.debug("Property '" + key + "' missing from " +
                    "aspect data '" + getClass().getName() + "'-'" +
                    hashCode() + "'. " + missingReason.getReason());
                throw new PropertyMissingException(
                    missingReason.getReason(),
                    key,
                    missingReason.getDescription());
            }
        } else {
            Throwable e;
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
                    errors.stream()
                    .map(FlowError::getThrowable)
                    .collect(Collectors.toList()));
            }
        }
        return result.getValue();

    }

    /**
     * Get the value associated with the specified key. Inheriting classes can
     * override this method where they access data in different ways.
     * @param key the string key to retrieve the value for
     * @param type will be populated with the value for the specified key
     * @param parameterisedTypes any parameterised types the value has
     * @param <T> the type of the value to be returned
     * @return a 'true' {@link TryGetResult} if the key is present in the data
     * store, a 'false' {@link TryGetResult} if not
     */
    protected <T> TryGetResult<T> tryGetValue(
        String key,
        Class<T> type,
        Class<?>... parameterisedTypes) {
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

    /**
     * Returns true if any of the engines added have lazy loading configured.
     * @param engines2 the engines to check
     * @return true if any engines have lazy loading
     */
    private static boolean anyLazyLoaded(List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> engines2) {
        for (AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine : engines2) {
            if (engine.getLazyLoadingConfiguration() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Waits for all the process futures in {@link #processFutures} to finish
     * then returns a list containing any errors which were thrown by the
     * futures.
     * @return list of errors
     */
    private List<FlowError> waitOnAllProcessFutures() {
        List<FlowError> errors = new ArrayList<>();
        for (Map.Entry<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>, Future<?>> entry :
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

    /**
     * Get a list of all the unique class names of the engines contained in the
     * {@link #processFutures} map.
     * @return list of distinct engine classes
     */
    private List<String> getDistinctEngineNames() {
        List<String> strings = new ArrayList<>();
        for (AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine : processFutures.keySet()) {
            if (strings.contains(engine.getClass().getName()) == false) {
                strings.add(engine.getClass().getName());
            }
        }
        return strings;
    }
}
