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

package fiftyone.pipeline.core.configuration;

import fiftyone.pipeline.annotations.AlternateName;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;

import fiftyone.pipeline.util.FiftyOneLookup;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration object that describes how to build a {@link Pipeline} using a
 * {@link PipelineBuilder}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/pipeline-configuration.md#pipelines">Specification</a>
 */
@XmlRootElement(name = "PipelineOptions")
public class PipelineOptions {

    /**
     * Configuration information for the {@link FlowElement}s that the Pipeline
     * will contain.
     * The order of elements is important as the pipeline will execute them
     * sequentially in the order they are supplied. To execute elements in
     * parallel, the {@link ElementOptions#subElements} property should be used.
     */
    @XmlElementWrapper(name = "Elements")
    @XmlElement(name = "Element")
    public List<ElementOptions> elements = new ArrayList<>();

    /**
     * A map where the keys are method names and the values are parameter values.
     * The method names can be exact matches, 'set' + name or match an
     * {@link AlternateName} annotation.
     */
    @XmlElement(name = "BuildParameters")
    @XmlJavaTypeAdapter(MapAdapter.class)
    public Map<String, Object> pipelineBuilderParameters = new HashMap<>();

    /**
     * Retrieve the value for the build option specified
     * @param builderName a builder name
     * @param buildParameter a builder parameter
     * @return the value for that option or null if not present
     */
    public String find(String builderName, String buildParameter){
        for (ElementOptions opts: elements) {
            if (opts.builderName.equals(builderName)) {
                return (String) opts.buildParameters.get(buildParameter);
            }
        }
        return null;
    }
    /**
     * Retrieve the value for the build option specified
     * @param builderName a builder name
     * @param buildParameter a builder parameter
     * @return the value for that option, with Lookup Options
     * substituted or null if not present
     */
    public String findAndSubstitute(String builderName, String buildParameter){
        for (ElementOptions opts: elements) {
            if (opts.builderName.equals(builderName)) {
                return FiftyOneLookup.getSubstitutor().
                        replace(opts.buildParameters.get(buildParameter));
            }
        }
        return null;
    }
    /**
     * Replace the value for the build option specified
     * @param builderName a builder name
     * @param buildParameter a builder parameter
     * @param value the new value
     * @return true if the option was found and replaced, false otherwise
     */
    public boolean replace(String builderName, String buildParameter, String value){
        for (ElementOptions opts: elements) {
            if (opts.builderName.equals(builderName)) {
                opts.buildParameters.put(buildParameter, value);
                return true;
            }
        }
        return false;
    }
}
