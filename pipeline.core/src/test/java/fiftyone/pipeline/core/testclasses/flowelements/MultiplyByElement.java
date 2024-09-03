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

package fiftyone.pipeline.core.testclasses.flowelements;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

public class MultiplyByElement
    extends FlowElementBase<TestElementData, ElementPropertyMetaData> {

    public List<String> evidenceKeys = Arrays.asList("value");
    private EvidenceKeyFilterWhitelist evidenceKeyFilter;
    private int multiple;

    public MultiplyByElement(int multiple) {
        super(
            mock(Logger.class),
            new ElementDataFactory<TestElementData>() {
                @Override
                public TestElementData create(FlowData flowData, FlowElement<TestElementData, ?> flowElement) {
                    return new TestElementData(
                        mock(Logger.class),
                        flowData);
                }
            });
        this.multiple = multiple;
        evidenceKeyFilter = new EvidenceKeyFilterWhitelist(evidenceKeys);
    }

    @Override
    public String getElementDataKey() {
        return "muliplyBy";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return Collections.emptyList();
    }

    @Override
    protected void processInternal(FlowData data) {
        TestElementData elementData = data.getOrAdd(
            getTypedDataKey(),
            getDataFactory());
        if (data.getEvidence().asKeyMap().containsKey(evidenceKeys.get(0))) {
            int value = (int) data.getEvidence().get(evidenceKeys.get(0));
            elementData.setResult(value * multiple);
        }
        else {
            elementData.setResult(null);
        }
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
