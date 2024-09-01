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
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.core.flowelements.FlowElement;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration object that describes how to build a {@link FlowElement}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/pipeline-configuration.md#flow-elements">Specification</a>
 */
@XmlRootElement(name = "Element")
public class ElementOptions {

    /**
     * The name of the builder to use when creating the {@link FlowElement}.
     * This does not necessarily have to be the full name of the type.
     * The system will match on:
     * 1. Exact match of type name
     * 2. Convention based match by removing 'Builder' from the end of the type
     * name. e.g. a BuilderName value of 'DeviceDetectionEngine' would match to
     * 'DeviceDetectionEngineBuilder'
     * 3. Matching on an {@link ElementBuilder#alternateName()} annotation.
     * e.g. a BuilderName value 'DDEngine' would match to
     * 'DeviceDetectionEngineBuilder' if that class was also annotated with
     * '@ElementBuilder(alternateName = "DDEngine")'.
     */
    @XmlElement(name = "BuilderName")
    public String builderName;

    /**
     * The dictionary keys are method names or names of parameters on the build
     * method of the builder. The value is the parameter value.
     * Similar to the BuilderName, the key value does not necessarily have to be
     * the full name of the method. The system will match on:
     * 1. Exact match of method name
     * 2. Convention based match by removing 'set' from the start of the method
     * name. e.g. a key value of 'AutomaticUpdatesEnabled' would match to method
     * 'setAutomaticUpdatesEnabled'
     * 3. Matching on an {@link AlternateName}. e.g. a key value of
     * 'AutoUpdates' would match to 'setAutoUpdateEnabled' if that method was
     * also annotated with '@AlternateName("AutoUpdates")'.
     */
    @XmlElement(name = "BuildParameters")
    @XmlJavaTypeAdapter(MapAdapter.class)
    public Map<String, Object> buildParameters;

    /**
     * If this property is populated, the flow element is a ParallelElements
     * instance. {@link #builderName} and {@link #buildParameters} should be
     * ignored. Each options instance within subElements contains the
     * configuration for an element to be added to a ParallelElements instance.
     * A ParallelElements always executes all it's children in parallel so the
     * ordering of this elements is irrelevant.
     */
    @XmlElementWrapper(name = "SubElements")
    @XmlElement(name = "Element")
    public List<ElementOptions> subElements;

    /**
     * Public constructor needed for XML binding.
     */
    public ElementOptions() {
        buildParameters = new HashMap<>();
        subElements = new ArrayList<>();
    }
}
