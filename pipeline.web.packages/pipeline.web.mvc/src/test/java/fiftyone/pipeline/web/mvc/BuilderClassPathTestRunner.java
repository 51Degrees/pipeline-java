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

package fiftyone.pipeline.web.mvc;

//import com.sun.xml.bind.v2.ContextFactory;
import org.glassfish.jaxb.runtime.v2.ContextFactory;

import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByElementBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import jakarta.xml.bind.JAXBContext;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class BuilderClassPathTestRunner extends BlockJUnit4ClassRunner {

    static ClassLoader classLoaderWithBuidlers;

    public BuilderClassPathTestRunner(Class<?> klass) throws InitializationError {
        super(loadFromCustomClassloader(klass));
    }

    private static Class<?> loadFromCustomClassloader(Class<?> clazz) throws InitializationError {
        try {
            // Only load once to support parallel tests
            if (classLoaderWithBuidlers == null) {
                classLoaderWithBuidlers = new ClassLoaderWithBuilders();
            }
            return Class.forName(clazz.getName(), true, classLoaderWithBuidlers);
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }


    @Override
    public void run(final RunNotifier notifier) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                BuilderClassPathTestRunner.super.run(notifier);
            }
        };
        Thread thread = new Thread(runnable);
        thread.setContextClassLoader(classLoaderWithBuidlers);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    // Custom class loader.
    // Loads classes that match pattern, otherwise load from context loader
    public static class ClassLoaderWithBuilders extends URLClassLoader {

        ClassLoader parent = null;

        public ClassLoaderWithBuilders() {

            super(getClasspathUrls(), null);

            parent = Thread.currentThread()
                    .getContextClassLoader();

        }

        @Override
        public synchronized Class<?> loadClass(String name) throws ClassNotFoundException {
            return parent.loadClass(name);
        }


        private static URL[] getClasspathUrls() {
            ArrayList<URL> classpathUrls = new ArrayList<>();

            classpathUrls.add(SequenceElement.class.getProtectionDomain().getCodeSource().getLocation());
            classpathUrls.add(JsonBuilderElement.class.getProtectionDomain().getCodeSource().getLocation());
            classpathUrls.add(JavaScriptBuilderElement.class.getProtectionDomain().getCodeSource().getLocation());
            classpathUrls.add(SetHeadersElementBuilder.class.getProtectionDomain().getCodeSource().getLocation());
            classpathUrls.add(MultiplyByElementBuilder.class.getProtectionDomain().getCodeSource().getLocation());
            if (Integer.parseInt(System.getProperty("java.version").split("\\.")[0]) >= 9) {
                classpathUrls.add(JAXBContext.class.getProtectionDomain().getCodeSource().getLocation());
                classpathUrls.add(ContextFactory.class.getProtectionDomain().getCodeSource().getLocation());
            }

            return classpathUrls.toArray(new URL[classpathUrls.size()]);
        }
    }
}
