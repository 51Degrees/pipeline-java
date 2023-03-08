/*
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 *  (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 *  If a copy of the EUPL was not distributed with this file, You can obtain
 *  one at https://opensource.org/licenses/EUPL-1.2.
 *
 *  The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 *  amended by the European Commission) shall be deemed incompatible for
 *  the purposes of the Work and the provisions of the compatibility
 *  clause in Article 5 of the EUPL shall not apply.
 *
 *   If using the Work as, or as part of, a network application, by
 *   including the attribution notice(s) required under Article 5 of the EUPL
 *   in the end user terms of the application under an appropriate heading,
 *   such notice(s) shall fulfill the requirements of that article.
 */

package fiftyone.pipeline.util;

import fiftyone.pipeline.annotations.AlternateName;
import fiftyone.pipeline.annotations.BuildArg;
import fiftyone.pipeline.annotations.ElementBuilder;
import org.reflections8.Reflections;
import org.reflections8.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utility to find and document all Builders available on the classpath, the classpath
 * being defined in the project POM.
 * <p>
 * Note: not recommended to run this from the standard pipeline environment as that doesn't
 * include builders that are present in other projects such as Device Detection. Better to
 * create a separate project that references this class and includes reference to Device Detection,
 * and hence will transitively include Pipeline.
 */
public class Builders {

    private static final Logger logger = LoggerFactory.getLogger("Builders");
    /**
     * Uses reflection to get all classes on the classpath
     * which are annotated with the {@link ElementBuilder} annotation.
     */
    private static Set<Class<?>> getAvailableElementBuilders() {

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
     * Output a Markdown table listing builders and properties
     */

    public static void doTable() {
        for (Class<?> clazz : getAvailableElementBuilders()) {
            System.out.println("# " + clazz.getName());
            System.out.println("| Class | Method |");
            System.out.println("| --- | --- |");

            for (Method method : clazz.getMethods()) {

                if (method.getName().startsWith("set") || method.getName().equals("build")) {
                    System.out.print("| " + method.getDeclaringClass().getName() + " | " + method.getName());
                    System.out.print("(");
                    System.out.print(getParameterDetails(method));
                    System.out.println(")|");

                }
            }
            System.out.println();
        }
    }

    /**
     * Output an XML property file with all builders and parameters
     */

    public static void doXml() {
        System.out.println("<PipelineOptions>");
        System.out.println("    <Elements>");
        // get all classes in the classpath annotated with ElementBuilder
        for (Class<?> clazz : getAvailableElementBuilders()) {
            methodsSeen.clear();
            System.out.println("        <!--<Element>");
            System.out.println("           <BuilderName>" + clazz.getSimpleName() + "</BuilderName>");
            // if there is an alternate name, output that too
            ElementBuilder elementBuilder = clazz.getAnnotation(ElementBuilder.class);
            String alternateName = elementBuilder.alternateName();
            if (!alternateName.isEmpty()) {
                System.out.println("            <BuilderName>" + alternateName + "</BuilderName>");
            }

            // list the build parameters
            System.out.println("            <BuildParameters>");
            if (Arrays.stream(clazz.getMethods()).noneMatch((m) -> m.getName().startsWith("set"))) {
                logger.debug("No methods");
            }
            for (Method method : clazz.getMethods()) {
                if (method.getName().startsWith("set")) {
                    // detail the method under its name minus set
                    detailMethod(method);
                    // detail the alternate name if there is one
                    AlternateName alternateName1 = method.getAnnotation(AlternateName.class);
                    if (alternateName1 != null) {
                        detailMethod(method, alternateName1.value());
                    }
                }
                // detail any build args
                if (method.getName().equals("build")) {
                    for (Parameter parameter : method.getParameters()) {
                        BuildArg buildArg = parameter.getAnnotation(BuildArg.class);
                        if (buildArg != null) {
                            detailMethod(method, buildArg.value());
                        }
                    }
                }

            }

            System.out.println("            </BuildParameters>");
            System.out.println("        </Element>-->");
            System.out.println();
        }
        System.out.println("    </Elements>");
        System.out.println("</PipelineOptions>");

    }

    // what methods have we already seen for this builder (can be from superclass or interface)
    private static final Set<String> methodsSeen = new HashSet<>();
    private static void detailMethod(Method method) {
        // remove the "set"
        detailMethod(method, method.getName().substring(3));
    }

    private static void detailMethod(Method method, String name) {
        if (!checkParameterDetails(method)) {
            logger.debug("Skipping " + name);
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("             <")
                .append(name)
                .append(">");
        builder.append(getParameterDetails(method));

        builder.append("  see ")
                .append(method.getDeclaringClass().getName())
                .append("#")
                .append(method.getName());
        builder.append("(");
        builder.append(getParameterDetails(method));
        builder.append(")");

        builder.append("</")
                .append(name)
                .append(">");
/*
        builder.append("    <!-- see ")
                .append(method.getDeclaringClass().getName())
                .append("#")
                .append(method.getName());
        builder.append("(");
        builder.append(getParameterDetails(method));
        builder.append(") -->");
 */
        if (methodsSeen.contains(builder.toString())) {
            logger.debug("already seen {}", builder.toString());
        } else {
            System.out.println(builder.toString());
            methodsSeen.add(builder.toString());
        }
    }

    // build the parameter list
    private static String getParameterDetails(Method method) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];
            String parameterType = parameter.getType().getName();
            if (parameterType.equals("[B")) {
                builder.append("byte[]");
            } else if (parameterType.startsWith("[L")) {
                builder.append(parameterType.substring(2))
                        .append("[]");
            } else {
                builder.append(parameterType);
            }
            if (i != method.getParameterCount() - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    // checks that no fiftyone structured parameters are included
    private static boolean checkParameterDetails(Method method) {
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter parameter = method.getParameters()[i];
            String parameterType = parameter.getType().getName();
            if (parameterType.equals("[B")) {
                return false;
            } else if (parameterType.startsWith("[L")) {
                if (parameterType.substring(2).startsWith("fiftyone")){
                    return false;
                }
            } else if(parameterType.startsWith("fiftyone")) {
                return false;
            }
        }
        return true;
    }

}
