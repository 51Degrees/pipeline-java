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

package fiftyone.pipeline.web.shared.flowelements;

import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.web.shared.data.JavaScriptData;
import org.slf4j.ILoggerFactory;

@ElementBuilder
public class JavaScriptBundlerElementBuilder {

    private ILoggerFactory loggerFactory;

    public JavaScriptBundlerElementBuilder(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    public JavaScriptBundlerElement build() {
        return new JavaScriptBundlerElement(
            loggerFactory.getLogger(JavaScriptBundlerElement.class.getName()),
            new JavaScriptDataFactory());
    }

    private class JavaScriptDataFactory implements ElementDataFactory<JavaScriptData> {

        @Override
        public JavaScriptData create(FlowData flowData, FlowElement<JavaScriptData, ?> flowElement) {
            return new JavaScriptData(
                loggerFactory.getLogger(JavaScriptData.class.getName()),
                flowData);
        }
    }
}
