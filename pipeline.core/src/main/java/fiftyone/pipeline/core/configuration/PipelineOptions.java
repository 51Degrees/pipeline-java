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

package fiftyone.pipeline.core.configuration;

import fiftyone.pipeline.annotations.AlternateName;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration object that describes how to build a  {@link Pipeline}using a
 * {@link PipelineBuilder}.
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

}
