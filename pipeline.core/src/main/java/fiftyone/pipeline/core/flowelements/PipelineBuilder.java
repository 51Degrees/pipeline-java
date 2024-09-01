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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.annotations.AlternateName;
import fiftyone.pipeline.annotations.BuildArg;
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.services.PipelineService;
import fiftyone.pipeline.util.FiftyOneLookup;
import org.apache.commons.text.StringSubstitutor;
import org.reflections8.Reflections;
import org.reflections8.util.ConfigurationBuilder;
import org.slf4j.ILoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.*;

import static fiftyone.pipeline.util.Check.getNotNull;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;
import static fiftyone.pipeline.util.Types.getPrimitiveTypeMap;

/**
 * The PipelineBuilder follows the fluent builder pattern. It is used to
 * construct instances of the {@link Pipeline}. 
 * 
 * A PipelineBuilder is intended to be used once only and once {@link #build()}
 * has been called it cannot be used further.
 * 
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#pipeline-builder">Specification</a>
 */
@SuppressWarnings("rawtypes")
public class PipelineBuilder
    extends PipelineBuilderBase<PipelineBuilder>
    implements PipelineBuilderFromConfiguration {

    private final Map<Class<?>, Class<?>> primitiveTypes = getPrimitiveTypeMap();
    private final Set<Class<?>> elementBuilders;

    /**
     * Construct a new builder.
     */
    public PipelineBuilder() {
        super();
        elementBuilders = getAvailableElementBuilders();
    }

    /**
     * Construct a new instance
     * @param loggerFactory logger factory to use when passing loggers to any
     *                      instances created by the builder
     */
    public PipelineBuilder(ILoggerFactory loggerFactory) {
        super(loggerFactory);
        elementBuilders = getAvailableElementBuilders();
    }

    @Override
    public Pipeline buildFromConfiguration(PipelineOptions options)
        throws Exception {

        getNotNull(options, "Options cannot be null");

        // Clear the list of flow elements ready to be populated
        // from the configuration options.
        flowElements.clear();
        int counter = 0;

        try {
            for (ElementOptions elementOptions : options.elements) {
                if (elementOptions.subElements != null &&
                    elementOptions.subElements.size() > 0) {
                    // The configuration has sub elements so create
                    // a ParallelElements instance.
                    addParallelElementsToList(
                        flowElements,
                        elementOptions,
                        counter);
                } else {
                    // The configuration has no sub elements so create
                    // a flow element.
                    addElementToList(flowElements, elementOptions,
                        "element " + counter);
                }
                counter++;
            }

            // Process any additional parameters for the pipeline
            // builder itself.
            List<String> builderParams = processBuildParameters(
                options.pipelineBuilderParameters,
                getClass(),
                this,
                "pipeline");
            if (builderParams.size() != 0) {
                throw new PipelineConfigurationException(
                        "The following builder parameters could not be processed: "
                                + stringJoin(builderParams, ","));
            }
        } catch (PipelineConfigurationException ex) {
            logger.debug("Problem with pipeline configuration, " +
                "failed to create pipeline.", ex);
            throw ex;
        }
        // As the elements are all created within the builder, the user
        // will not be handling disposal so make sure the pipeline is
        // configured to do so.
        setAutoCloseElements(true);

        // Build and return the pipeline using the list of flow elements
        // that have been created from the configuration options.
        return build();
    }

    /**
     * Uses reflection to populate {@link #elementBuilders} with all classes
     * which are annotated with the {@link ElementBuilder} annotation.
     *
     * @return
     */
    public static Set<Class<?>> getAvailableElementBuilders() {

        ConfigurationBuilder config = ConfigurationBuilder.build();

        config.setInputsFilter(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.toLowerCase().contains("flowelements");
            }
        });
        config.setExpandSuperTypes(false);

        // Get all the classes annotated with '@ElementBuilder'.
        Reflections reflections = new Reflections(config);

        return reflections.getTypesAnnotatedWith(ElementBuilder.class);
    }

    /**
     * Create a new {@link FlowElement} using the specified
     * {@link ElementOptions} and add it to the supplied list of elements.
     * @param elements the list to add the new {@link FlowElement} to
     * @param elementOptions  instance to use when creating the
     *                        {@link FlowElement}
     * @param elementLocation the string description of the element's location
     *                        within the {@link PipelineOptions} instance
     */
    private void addElementToList(
        List<FlowElement> elements,
        ElementOptions elementOptions,
        String elementLocation) {
        // Check that a builder name is set
        if (elementOptions.builderName == null ||
            elementOptions.builderName.isEmpty()) {
            throw new PipelineConfigurationException(
                "A BuilderName must be specified for " +
                    elementLocation + ".");
        }

        // Try to get the builder to use
        Class<?> builderType = getBuilderType(elementOptions.builderName);
        if (builderType == null) {
            List<String> names = new ArrayList<>();
            for (Class<?> builder : elementBuilders) {
                names.add(builder.getSimpleName());
            }
            throw new PipelineConfigurationException(
                "Could not find builder matching '" +
                    elementOptions.builderName + "' for " +
                    elementLocation + ". Available builders are: " +
                    stringJoin(names, ","));
        }

        // Get the methods on the builder
        List<Method> buildMethods = new ArrayList<>();
        for (Method method : builderType.getMethods()) {
            if (method.getName().equals("build") &&
                // We need to exclude methods on base classes that have been 
                // overridden. (see https://stackoverflow.com/questions/6204339/java-class-getmethods-behavior-on-overridden-methods)
                method.isBridge() == false) {
                buildMethods.add(method);
            }
        }

        // If there are no 'build' methods or if there is no default
        // constructor then throw an error.
        if (buildMethods.size() == 0) {
            throw new PipelineConfigurationException(
                "Builder '" + builderType.getName() + "' for " +
                    elementLocation + " has no 'build' methods.");
        }


        // A service collection does not exist in the builder, so try
        // to construct a builder instance from the assemblies
        // currently loaded.
        Object builderInstance = getBuilder(builderType);
        if (builderInstance == null) {
            throw new PipelineConfigurationException(
                "Builder '" + builderType.getName() + "' for " +
                    elementLocation + " does not have a default constructor. " +
                    "i.e. One that takes no parameters. Or a constructor " +
                    "which takes an ILoggerFactory parameter.");
        }
        // Holds a list of the names of parameters to pass to the
        // build method when we're ready.
        List<String> buildParameterList = new ArrayList<>();

        if (elementOptions.buildParameters != null) {
            // Call any non-build methods on the builder to set optional
            // parameters.
            buildParameterList = processBuildParameters(
                elementOptions.buildParameters,
                builderType,
                builderInstance,
                elementLocation);
        }

        // At this point, all the optional methods on the builder
        // have been called and we're ready to call the Build method.
        // If there are no matching build methods or multiple possible
        // build methods based on our parameters then throw an exception.
        List<Method> possibleBuildMethods = new ArrayList<>();
        for (Method method : buildMethods) {
            if (method.getParameterTypes().length == buildParameterList.size()) {
                possibleBuildMethods.add(method);
            }
        }
        if (possibleBuildMethods.size() != 1) {
            // the assumption was that anything left over from the "optional" parameter
            // processing must be a build method parameter, so we need a build method
            // with the right number of parameters to match. If there is one, and only one,
            // then processing will continue and errors in the options will be picked up in
            // subsequent processing. At this point it's actually more likely that an option
            // has been misspelled or that it just doesn't exist.
            if (buildParameterList.size() > 0) {
                String exceptionMessage = String.format(
                        "Build parameters \"%s\" were not matched for Builder \"%s\".",
                        stringJoin(buildParameterList, ","), builderType.getName());
                throw new PipelineConfigurationException(exceptionMessage);
            }
            // otherwise we can't find a build() method
            throw new PipelineConfigurationException(
                "Builder '" + builderType.getName() + "' for " +
                    elementLocation + " has " +
                    (possibleBuildMethods.size() == 0 ? "no" : "multiple") +
                    "'Build' methods that take " + buildParameterList.size() +
                    " parameters. The supplied parameters must match one of " +
                    "the following signatures: " +
                    stringJoin(buildMethods, ", "));
        }

        // Get the build method parameters and add the configured
        // values to the parameter list.
        List<Object> parameters = new ArrayList<>();
        Method buildMethod = possibleBuildMethods.get(0);
        Class<?>[] types = buildMethod.getParameterTypes();
        Annotation[][] annotations = buildMethod.getParameterAnnotations();
        for (int i = 0; i < types.length; i++) {
            Map<String, Object> caseInsensitiveParameters =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveParameters.putAll(elementOptions.buildParameters);
            Object paramValue;
            Class<?> paramType = types[i];
            Annotation[] paramAnnotations = annotations.length > i ?
                annotations[i] : new Annotation[0];
            BuildArg paramAnnotation = null;
            for (Annotation annotation : paramAnnotations) {
                if (annotation instanceof BuildArg) {
                    paramAnnotation = (BuildArg) annotation;
                    break;
                }
            }
            if (paramAnnotation == null) {
                throw new PipelineConfigurationException(
                    "Method 'build' on builder '" + builderType.getName() +
                        "' for " + elementLocation + " is not annotated with " +
                        "the name of the parameter.");
            }
            if (paramType.equals(String.class)) {
                paramValue = caseInsensitiveParameters.get(paramAnnotation.value());
                // replace properties with values
                if (paramValue instanceof String) {
                    paramValue = evaluate((String) paramValue);
                }
            } else {
                paramValue = parseToType(
                    paramType,
                    caseInsensitiveParameters.get(paramAnnotation.value()).toString(),
                    "Method 'build' on builder '" +
                        builderType.getName() + "' for " +
                        elementLocation + " expects a parameter of type " +
                        "'" + paramType.getName() + "'");
            }
            parameters.add(paramValue);
        }

        // Call the build method with the parameters we set up above.
        Object result;
        try {
            result = buildMethod.invoke(builderInstance, parameters.toArray());
        } catch (Exception e) {
            throw new PipelineConfigurationException(
                "Failed to build " + elementLocation + " using " +
                    "'" + builderType.getName() + "'.",
                e);
        }
        if (result == null) {
            throw new PipelineConfigurationException(
                "Failed to build " + elementLocation + " using " +
                    "'" + builderType.getName() + "', reason unknown.");
        }

        try {
            FlowElement element = FlowElement.class.cast(result);
            // Add the element to the list.
            elements.add(element);
        }
        catch (ClassCastException e){
            String message = "Failed to cast '" + result.getClass().getName() +
                "' to 'FlowElement' for " + elementLocation;
            logger.debug(message);
            throw new PipelineConfigurationException(message, e);
        }
    }

    /**
     * Create a {@link ParallelElements} from the specified configuration and
     * add it to the {@link #flowElements} list.
     * @param elements the list to add the new {@link ParallelElements} to
     * @param elementOptions the {@link ElementOptions} instance to use when
     *                       creating the {@link ParallelElements}
     * @param elementIndex the index of the element within the
     *                     {@link PipelineOptions}
     */
    private void addParallelElementsToList(
        List<FlowElement> elements,
        ElementOptions elementOptions,
        int elementIndex) {
        // Element contains further sub elements, this is not allowed.
        if ((elementOptions.builderName != null &&
            elementOptions.builderName.isEmpty() == false) ||
            (elementOptions.buildParameters != null &&
                elementOptions.buildParameters.size() > 0)) {
            throw new PipelineConfigurationException(
                "ElementOptions " + elementIndex + " contains both " +
                    "SubElements and other settings values. " +
                    "This is invalid");
        }
        List<FlowElement> parallelElements = new ArrayList<>();

        // Iterate through the sub elements, creating them and
        // adding them to the list.
        int subCounter = 0;
        for (ElementOptions subElement : elementOptions.subElements) {
            if (subElement.subElements != null &&
                subElement.subElements.size() > 0) {
                throw new PipelineConfigurationException(
                    "ElementOptions " + elementIndex + " contains nested " +
                        "sub elements. This is not supported.");
            } else {
                addElementToList(parallelElements, subElement,
                    "element " + subCounter + " in element " + elementIndex);
            }
            subCounter++;
        }
        // Now we've created all the elements, create the
        // ParallelElements instance and add it to the pipeline's
        // elements.
        ParallelElements parallelInstance = new ParallelElements(
            loggerFactory.getLogger(ParallelElements.class.getName()),
            parallelElements);
        flowElements.add(parallelInstance);
    }

    /**
     * Determines whether the parameters of the constructor are requirements
     * which can be met by the builder. This does not check that the services
     * are available, only that they are either services or a logger factory.
     * @param constructor the constructor to check the parameters of
     * @return true if all parameters are services or logger factory
     */
    private boolean paramsAreServices(Constructor<?> constructor) {
        for (Class<?> type : constructor.getParameterTypes()) {
            if (type.equals(ILoggerFactory.class) == false &&
                PipelineService.class.isAssignableFrom(type) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the service from the service collection if it exists, otherwise
     * return null.
     *
     * Note that if more than one instance implementing the same service is
     * added to the services, the first will be returned.
     * @param serviceType the service class to be returned
     * @param <T> the service class to be returned
     * @return service of type {@link T}, or null
     */
    @SuppressWarnings("unchecked")
    private <T extends PipelineService> T getService(Class<T> serviceType) {
        for (PipelineService service : services) {
            if (serviceType.isAssignableFrom(service.getClass())) {
                return (T)service;
            }
        }
        return null;
    }

    /**
     * Get the best constructor for the list of constructors. Best meaning
     * the constructor with the most parameters which can be fulfilled.
     * @param constructors constructors to get the best of
     * @return best constructor or null if none have parameters that can be
     * fulfilled
     */
    @SuppressWarnings("unchecked")
    private Constructor<?> getBestConstructor(List<Constructor<?>> constructors) {
        Constructor<?> bestConstructor = null;
        for (Constructor<?> constructor : constructors) {
            if (bestConstructor == null ||
                    constructor.getParameterTypes().length >
                        bestConstructor.getParameterTypes().length) {
                boolean hasServices = true;
                for (Class type : constructor.getParameterTypes()) {
                    if (type.equals(ILoggerFactory.class) == false &&
                        getService(type) == null) {
                        // The parameter is not a logger factory, and the
                        // service cannot be found, so this cannot be used.
                        hasServices = false;
                        break;
                    }
                }
                if (hasServices == true) {
                    bestConstructor = constructor;
                }
            }
        }
        return bestConstructor;
    }

    /**
     * Get the services required for the constructor, and call it with them.
     * @param constructor the constructor to call
     * @return instance returned by the constructor
     * @throws PipelineConfigurationException
     */
    @SuppressWarnings("unchecked")
    private Object callConstructorWithServices(Constructor<?> constructor, Class<?> builderType)
        throws PipelineConfigurationException {
        Class[] types = constructor.getParameterTypes();
        Object[] services = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(ILoggerFactory.class)) {
                services[i] = loggerFactory;
            }
            else {
                services[i] = getService(types[i]);
            }
        }
        try {
            return constructor.newInstance(services);        
        } catch (Exception e) {
            throw new PipelineConfigurationException(
                "Failed to create builder '" + builderType.getName() + 
                "'. See inner, 'cause' exception for more details", e);
        }
    }

    /**
     * Get a new instance of the builder type provided. This tries to call the
     * constructor with the most arguments which can be fulfilled. Potential
     * constructor parameters are {@link ILoggerFactory} and anything
     * implementing {@link PipelineService}.
     * @param builderType type of builder to get an instance of
     * @return new builder instance
     * @throws PipelineConfigurationException
     */
    @SuppressWarnings("deprecation")
    private Object getBuilder(Class<?> builderType)
        throws PipelineConfigurationException {
        // Get the valid constructors. This means either a default
        // constructor, or a constructor taking a logger factory as an
        // argument.
        List<Constructor<?>> defaultConstructors = new ArrayList<>();
        List<Constructor<?>> loggerConstructors = new ArrayList<>();
        List<Constructor<?>> serviceConstructors = new ArrayList<>();
        for (Constructor<?> constructor : builderType.getConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                defaultConstructors.add(constructor);
            } else if (constructor.getParameterTypes().length == 1 &&
                constructor.getParameterTypes()[0].equals(ILoggerFactory.class)) {
                loggerConstructors.add(constructor);
            } else if (constructor.getParameterTypes().length > 1 &&
                paramsAreServices(constructor)){
                serviceConstructors.add(constructor);
            }
        }
        if (defaultConstructors.size() == 0 &&
            loggerConstructors.size() == 0 &&
            serviceConstructors.size() == 0) {
            return null;
        }

        // Create the builder instance using the constructor with a logger
        // factory, or the default constructor if one taking a logger
        // factory is not available.
        if (serviceConstructors.size() != 0 &&
            getBestConstructor(serviceConstructors) != null) {
            return callConstructorWithServices(
                getBestConstructor(serviceConstructors), builderType);
        }

        try {
            if (loggerConstructors.size() != 0) {
                return loggerConstructors.get(0).newInstance(loggerFactory);
            } else {
                return builderType.newInstance();
            }
        } catch (Exception e) {
            throw new PipelineConfigurationException(
                "Failed to create builder '" + builderType.getName() + 
                "'. See inner, 'cause' exception for more details", e);
        }
    }

    /**
     * Call the non-build methods on the builder that configuration options have
     * been supplied for.
     *
     * Each method must take only one parameter and the parameter type
     * must either be a string or have a 'parse' method available.
     * @param buildParameters a map containing the names of the methods to call
     *                        and the value to pass as a parameters
     * @param builderType the {@link Class} of the builder that is being used to
     *                   create the {@link FlowElement}
     * @param builderInstance the instance of the builder that is being used to
     *                        create the
     * @param elementConfigLocation a string containing a description of the
     *                              location of the configuration for this
     *                              element. This will be added to error
     *                              messages to help the user identify any
     *                              problems
     * @return a list of the names of the entries from buildParameters that are
     * to be used as mandatory parameters to the Build method rather than
     * optional builder methods
     */
    private List<String> processBuildParameters(
        Map<String, Object> buildParameters,
        Class<?> builderType,
        Object builderInstance,
        String elementConfigLocation) {
        // Holds a list of the names of parameters to pass to the
        // build method when we're ready.
        List<String> buildParameterList = new ArrayList<>();

        for (Map.Entry<String, Object> parameter : buildParameters.entrySet()) {
            // Check if the build parameter corresponds to a method
            // on the builder.
            List<Method> methods = getMethods(
                parameter.getKey(),
                builderType.getMethods());
            if (methods.size() == 0) {
                // If not then add the parameter to the list of parameters
                // to pass to the Build method instead.
                buildParameterList.add(parameter.getKey().toLowerCase());
            } else {
                boolean methodCalled = false;
                int counter = 0;
                while (methodCalled == false && counter < methods.size()) {
                    Method method = methods.get(counter);
                    counter++;

                    // The parameter corresponds to a method on the builder
                    // so get the parameters associated with that method.
                    Class<?>[] methodParams = method.getParameterTypes();
                    if (methodParams.length != 1) {
                        throw new PipelineConfigurationException(
                            "Method '" + method.getName() + "' on builder " +
                                "'" + builderType.getName() + "' for " +
                                elementConfigLocation + " takes " +
                                (methodParams.length == 0 ? "no parameters " : "more than one parameter. ") +
                                "This is not supported.");
                    }
                    // Call any methods which relate to the build parameters
                    // supplied in the configuration.
                    try {
                        if (parameter.getValue().getClass().isArray() == true) {
                            // The parameter is an array, so try each value
                            // individually.
                            for (Object value : (Object[]) parameter.getValue()) {
                                tryParseAndCallMethod(
                                    value,
                                    method,
                                    builderType,
                                    builderInstance,
                                    elementConfigLocation);
                            }
                        } else {
                            // Not an array, so just call the method.
                            Object paramValue = parameter.getValue();
                            // replace properties with values
                            if (paramValue instanceof String) {
                                paramValue = evaluate((String) paramValue);
                            }

                            tryParseAndCallMethod(
                                paramValue,
                                method,
                                builderType,
                                builderInstance,
                                elementConfigLocation);
                        }
                        methodCalled = true;
                    } catch (PipelineConfigurationException e) {
                        if (counter == methods.size()) {
                            throw e;
                        }
                    }
                }
            }
        }

        return buildParameterList;
    }

    /**
     * Attempt to call a method on the builder using the parameter value
     * provided. The value can be parsed to basic types (e.g. string or int) but
     * complex types are not supported.
     * @param paramValue value of the parameter to call the method with
     * @param method method to attempt to cal
     * @param builderType the {@link Class} of the builder that is being used to
     *                   create the {@link FlowElement}
     * @param builderInstance the instance of the builder that is being used to
     *                        create the {@link FlowElement}
     * @param elementConfigLocation a string containing a description of the
     *                              configuration for this element. This will be
     *                              added to error messages to help the user
     *                              identify any problems
     */
    private void tryParseAndCallMethod(
        Object paramValue,
        Method method,
        Class<?> builderType,
        Object builderInstance,
        String elementConfigLocation) {
        Class<?> paramType = method.getParameterTypes()[0];

        // If the method takes a string then we can just pass it
        // in. If not, we'll have to parse it to the required
        // type.

        if (paramType.equals(String.class) == false) {
            paramValue = parseToType(paramType, paramValue.toString(),
                "Method '" + method.getName() + "' on builder " +
                    "'" + builderType.getName() + "' for " +
                    elementConfigLocation + " expects a parameter of type " +
                    "'" + paramType.getName() + "'");
        }

        // Invoke the method on the builder, passing the parameter
        // value that was defined in the configuration.
        try {
            method.invoke(builderInstance, paramValue);
        } catch (Exception e) {
            throw new PipelineConfigurationException(
                "Exception while calling the method '" + method.getName() +
                    "' on builder '" + builderType.getName() + "'.",
                e);
        }
    }

    /**
     * Get the method associated with the given name.
     * @param methodName The name of the method to get. This is case insensitive
     *                   and can be:
     *                   1. The exact method name
     *                   2. The method name with the text 'set' removed from
     *                   the start.
     *                   3. An alternate name, as defined by an
     *                   {@link AlternateName}
     * @param methods the list of methods to try and find a match in
     * @return the {@link Method} of the matching method or null if no match
     * could be found
     */
    private List<Method> getMethods(
        String methodName,
        Method[] methods) {
        int tries = 0;
        List<Method> matchingMethods = new ArrayList<>();
        String lowerMethodName = methodName.toLowerCase();

        while (tries < 3 && matchingMethods.size() == 0) {
            List<Method> potentialMethods = null;

            switch (tries) {
                case 0:
                    // First try and find a method that matches the
                    // supplied name exactly.
                    for (Method method : methods) {
                        if (method.getName().toLowerCase().equals(lowerMethodName)) {
                            matchingMethods.add(method);
                        }
                    }
                    break;
                case 1:
                    // Next, try and find a method that matches the
                    // supplied name with 'set' added to the start.
                    String tempName = "set" + lowerMethodName;
                    for (Method method : methods) {
                        if (method.getName().toLowerCase().equals(tempName)) {
                            matchingMethods.add(method);
                        }
                    }
                    break;
                case 2:
                    // Finally, see if there is a method that has an
                    // AlternateNameAttribute with a matching name.
                    List<Method> tempMethods = new ArrayList<>();
                    for (Method method : methods) {
                        try {
                            AlternateName alternateName =
                                method.getAnnotation(AlternateName.class);
                            if (alternateName.value().toLowerCase().equals(
                                lowerMethodName)) {
                                tempMethods.add(method);
                            } else if (alternateName.value().toLowerCase().equals(
                                "set" + lowerMethodName)) {
                                tempMethods.add(method);
                            }
                        } catch (Exception e) {
                            logger.debug("Exception while attempting to get" +
                                "a set method with an alternate name annotation." +
                                "Name was '" + lowerMethodName + "'",
                                e);
                        }
                    }
                    potentialMethods = tempMethods;
                    break;
                default:
                    break;
            }

            if (potentialMethods != null && potentialMethods.size() > 0) {
                matchingMethods = potentialMethods;
            }

            tries++;
        }

        return matchingMethods;
    }

    /**
     * Get the element builder associated with the given name.
     * @param builderName the name of the builder to get. This is case
     *                    insensitive and can be:
     *                    1. The builder type name
     *                    2. The builder type name with the text 'builder'
     *                    removed from the end.
     *                    3. An alternate name, as defined by an
     *                    {@link ElementBuilder}
     * @return the {@link Class} of the element builder or null if no match
     * could be found
     */
    private Class<?> getBuilderType(String builderName) {
        int tries = 0;
        Class<?> builderType = null;
        String lowerBuilderName = builderName.toLowerCase();

        while (tries < 3 && builderType == null) {
            List<Class<?>> potentialBuilders = new ArrayList<>();

            switch (tries) {
                case 0:
                    // First try and find a builder that matches the
                    // supplied name exactly.
                    for (Class<?> builder : elementBuilders) {
                        if (builder.getSimpleName().toLowerCase().equals(lowerBuilderName)) {
                            potentialBuilders.add(builder);
                        }
                    }
                    break;
                case 1:
                    String tempName = lowerBuilderName + "builder";
                    // Next, try and find a builder that matches the
                    // supplied name with 'builder' added to the end.
                    for (Class<?> builder : elementBuilders) {
                        if (builder.getSimpleName().toLowerCase().equals(tempName)) {
                            potentialBuilders.add(builder);
                        }
                    }
                    break;
                case 2:
                    // Finally, see if there is a builder that has an
                    // AlternateNameAttribute with a matching name.
                    List<Class<?>> builders = new ArrayList<>();
                    for (Class<?> builder : elementBuilders) {
                        for (Annotation annotation : builder.getAnnotations()) {
                            if (annotation instanceof ElementBuilder &&
                                ((ElementBuilder) annotation).alternateName().toLowerCase().equals(lowerBuilderName)) {
                                builders.add(builder);
                            }
                        }
                    }
                    potentialBuilders = builders;
                    break;
                default:
                    break;
            }

            if (potentialBuilders.size() > 0) {
                // If the name matches multiple builders at any stage
                // then throw an error.
                if (potentialBuilders.size() > 1) {
                    List<String> names = new ArrayList<>();
                    for (Class<?> builder : potentialBuilders) {
                        names.add(builder.getName());
                    }

                    throw new PipelineConfigurationException(
                        "The flow element builder name '" + builderName + "'" +
                            "matches multiple builders: [" +
                            stringJoin(names, ",") + "].");
                }

                try {
                    builderType = potentialBuilders.get(0);
                } catch (IndexOutOfBoundsException e) {
                    builderType = null;
                }
            }

            tries++;
        }

        return builderType;
    }

    /**
     * Parse a string value to the target type using the 'parse' method on the
     * {@link Class} of the target type. For primitive types, the
     * {@link #primitiveTypes} map is used to get the class with a suitable
     * parse method.
     * @param targetType the type to parse the value to
     * @param value the value to parse
     * @param errorTextPrefix the text to prefix any errors that are thrown with
     * @return the parsed value
     */
    private Object parseToType(
        Class<?> targetType,
        String value,
        String errorTextPrefix) {
        // Check for a TryParse method on the type
        Method parse = null;
        Class<?> nonPrimitiveType;
        if (targetType.isPrimitive()) {
            nonPrimitiveType = primitiveTypes.get(targetType);
        } else {
            nonPrimitiveType = targetType;
        }
        for (Method method : nonPrimitiveType.getMethods()) {
            if (method.getName().startsWith("parse") &&
                method.getParameterTypes().length == 1 &&
                method.getParameterTypes()[0].equals(String.class)) {
                parse = method;
                break;
            }
        }
        if (parse == null) {
            throw new PipelineConfigurationException(
                errorTextPrefix + " but this type does not have " +
                    "the expected 'parse' method " +
                    "(or has multiples that cannot be resolved).");
        }
        // Call the try parse method.
        Object parsedValue;
        try {
            parsedValue = parse.invoke(new Object(), value);
        } catch (Exception e) {
            throw new PipelineConfigurationException(
                errorTextPrefix + ". Failed to parse value " +
                    "'" + value + "' using the static " +
                    "'parse' method.");
        }
        return parsedValue;
    }

    /**
     * Evaluate strings for substitution of system properties etc.
     */
    public static String evaluate(String value) {
        StringSubstitutor substitutor = FiftyOneLookup.getSubstitutor();
        return substitutor.replace(value);
    }
}
