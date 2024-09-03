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

package fiftyone.pipeline.developerexamples.clientsideelement.flowelements;

import fiftyone.pipeline.developerexamples.clientsideelement.data.StarSignData;
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import org.slf4j.ILoggerFactory;

//! [class]
@ElementBuilder(alternateName = "SimpleClientSideElement")
public class SimpleClientSideElementBuilder {
    private final ILoggerFactory loggerFactory;

    public SimpleClientSideElementBuilder(ILoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    public SimpleClientSideElement build() {
        return new SimpleClientSideElement(
            loggerFactory.getLogger(SimpleClientSideElement.class.getName()),
            new ElementDataFactory<StarSignData>() {
                @Override
                public StarSignData create(
                    FlowData flowData,
                    FlowElement<StarSignData, ?> flowElement) {
                    return new StarSignDataInternal(
                        loggerFactory.getLogger(StarSignDataInternal.class.getName()),
                        flowData);
                }
            });
    }
}
//! [class]
