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

package fiftyone.pipeline.util;

import fiftyone.pipeline.annotations.AlternateName;
import fiftyone.pipeline.annotations.BuildArg;
import fiftyone.pipeline.annotations.DefaultValue;
import fiftyone.pipeline.annotations.ElementBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static fiftyone.pipeline.core.flowelements.PipelineBuilder.getAvailableElementBuilders;

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
    // what methods have we already seen for this builder (can be from superclass or interface)
    private static final Set<String> methodsSeen = new HashSet<>();

    /**
     * Output a Markdown table listing builders and properties
     */

    public static void doTable(PrintStream out) {
        for (Class<?> clazz : getAvailableElementBuilders()) {
            out.println("# " + clazz.getName());
            out.println("| Method | Default | Class |");
            out.println("| --- | --- | --- |");

            for (Method method : clazz.getMethods()) {

                if (method.getName().startsWith("set") || method.getName().equals("build")) {
                    out.print("| ");
                    if (Objects.nonNull(method.getAnnotation(Deprecated.class))){
                        out.print("Deprecated - ");
                    }
                    out.print(method.getName());
                    out.print("(");
                    out.print(getParameterDetails(method));
                    out.print(") | ");

                    out.print(getDefaultValue(method));
                    out.print(" | " + method.getDeclaringClass().getName());
                    out.println(" |");
                }
            }
            out.println();
        }
    }

    /**
     * Output an XML property file with all builders and parameters
     */

    public static void doXml(PrintStream out) {
        out.println("<PipelineOptions>");
        out.println("    <Elements>");
        // get all classes in the classpath annotated with ElementBuilder
        for (Class<?> clazz : getAvailableElementBuilders()) {
            methodsSeen.clear();
            out.println("        <!--<Element>");
            out.println("           <BuilderName>" + clazz.getSimpleName() +
                    "</BuilderName>");
            // if there is an alternate name, output that too
            ElementBuilder elementBuilder = clazz.getAnnotation(ElementBuilder.class);
            String alternateName = elementBuilder.alternateName();
            if (!alternateName.isEmpty()) {
                out.println("            <BuilderName>" + alternateName + "</BuilderName>");
            }

            // list the build parameters
            out.println("            <BuildParameters>");
            if (Arrays.stream(clazz.getMethods()).noneMatch((m) -> m.getName().startsWith("set"))) {
                logger.debug("No methods");
            }
            // detail build args first
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals("build")) {
                    for (Parameter parameter : method.getParameters()) {
                        BuildArg buildArg = parameter.getAnnotation(BuildArg.class);
                        if (buildArg != null) {
                            String methodDetail = detailMethod(method, buildArg.value());
                            if (Check.notNullOrBlank(methodDetail)) {
                                out.println(methodDetail);
                            }
                        }
                    }
                }
            }
            // now detail the set methods
            for (Method method : clazz.getMethods()) {
                if (method.getName().startsWith("set")) {
                    // detail the method under its name minus set
                    String methodDetail = detailMethod(method);
                    if (Check.notNullOrBlank(methodDetail)) {
                        out.println(methodDetail);
                    }
                    // detail the alternate name if there is one
                    AlternateName alternateName1 = method.getAnnotation(AlternateName.class);
                    if (alternateName1 != null) {
                        out.println(detailMethod(method, alternateName1.value()));
                    }
                }
            }

            out.println("            </BuildParameters>");
            out.println("        </Element>-->");
            out.println();
        }
        out.println("    </Elements>");
        out.println("</PipelineOptions>");

    }

    private static String detailMethod(Method method) {
        // remove the "set"
        return detailMethod(method, method.getName().substring(3));
    }

    private static String detailMethod(Method method, String name) {
        Deprecated deprecated = method.getAnnotation(Deprecated.class);
        if (!checkParameterDetails(method) || Objects.nonNull(deprecated)) {
            logger.debug("Skipping " + name);
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("             <")
                .append(name)
                .append(">");
        if (method.getName().equals("build")) {
            builder.append("No default, value must be supplied");
        } else {
            builder.append(getDefaultValue(method));
        }
        builder.append(" - see ")
                .append(method.getDeclaringClass().getName())
                .append("#")
                .append(method.getName());
        builder.append("(");
        builder.append(getParameterDetails(method));
        builder.append(")");

        builder.append("</")
                .append(name)
                .append(">");

        if (methodsSeen.contains(builder.toString())) {
            logger.debug("already seen {}", builder.toString());
            return "";
        } else {
            methodsSeen.add(builder.toString());
            return builder.toString();
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
                // lose the trailing ";"
                builder.append(parameterType.substring(2, parameterType.length()-1))
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
                return false;
            } else if (parameterType.startsWith("fiftyone")) {
                return false;

            // not interested in List or Set
            } else if (parameterType.startsWith("java.util")) {
                return false;
            }
        }
        return true;
    }


    private static String getDefaultValue(Method method) {
        DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
        if (Objects.nonNull(defaultValue)) {
            if (Check.notNullOrBlank(defaultValue.value())) {
                return defaultValue.value();
            } else if (defaultValue.intValue() != Integer.MIN_VALUE) {
                return String.valueOf(defaultValue.intValue());
            } else if (defaultValue.doubleValue() != Double.MIN_VALUE) {
                return String.valueOf(defaultValue.doubleValue());
            } else {
                return String.valueOf(defaultValue.booleanValue());
            }
        }
        return "";
    }
}
