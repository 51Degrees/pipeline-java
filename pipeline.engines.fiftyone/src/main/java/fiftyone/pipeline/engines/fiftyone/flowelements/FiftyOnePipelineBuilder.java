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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;

/**
 * Pipeline builder class that allows the 51Degrees share usage element to be
 * enabled/disabled.
 */
public class FiftyOnePipelineBuilder extends PipelineBuilder {

    private boolean shareUsageEnabled = true;

    /**
     * Construct a new builder.
     */
    public FiftyOnePipelineBuilder() {
        super();
    }

    /**
     * Construct a new instance
     * @param loggerFactory logger factory to use when passing loggers to any
     *                      instances created by the builder
     */
    public FiftyOnePipelineBuilder(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    /**
     * Set share usage enabled/disabled.
     * Defaults to enabled.
     * @param enabled true to enable usage sharing, false to disable
     * @return this builder
     */
    public FiftyOnePipelineBuilder setShareUsage(boolean enabled) {
        this.shareUsageEnabled = enabled;
        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void onPreBuild() {
        
        // Add the sequence element if it does not exist
        boolean containsSequence = false;
        for (FlowElement element : getFlowElements()) {
            if (element instanceof SequenceElement) {
                containsSequence = true;
                break;
            }
        }
        if (containsSequence == false) {
            try {
                flowElements.add(0, new SequenceElementBuilder(loggerFactory).build());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        
        // Add the share usage element if it does not exist in the list.
        if (shareUsageEnabled) {
            boolean containsShareUsage = false;
            for (FlowElement element : getFlowElements()) {
                if (element instanceof ShareUsageElement) {
                    containsShareUsage = true;
                    break;
                }
            }
            if (containsShareUsage == false) {
                try {
                    addFlowElement(new ShareUsageBuilder(
                        loggerFactory).build());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
