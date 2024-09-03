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

package fiftyone.pipeline.engines.testhelpers.data;

import fiftyone.pipeline.core.data.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Map;

import static org.mockito.Mockito.*;

public class MockFlowData {

    @SuppressWarnings("unchecked")
    public static FlowData createFromEvidence(
        final Map<String, Object> evidenceData,
        boolean dataKeyFromAllEvidence) {
        final Evidence evidence = mock(Evidence.class);
        when(evidence.asKeyMap()).thenReturn(evidenceData);
        FlowData data = mock(FlowData.class);
        when(data.getEvidence()).thenReturn(evidence);
        when(data.addEvidence(anyString(), any())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                evidenceData.put(
                    (String) invocationOnMock.getArgument(0),
                    invocationOnMock.getArgument(1));
                return null;
            }
        });

        if (dataKeyFromAllEvidence) {
            DataKeyBuilder keyBuilder = new DataKeyBuilderDefault();
            for (Map.Entry<String, Object> entry : evidenceData.entrySet()) {
                keyBuilder.add(0, entry.getKey(), entry.getValue());
            }
            DataKey key = keyBuilder.build();
            when(data.generateKey(any(EvidenceKeyFilter.class))).thenReturn(key);
        }

        when(data.tryGetEvidence(anyString(), any(Class.class))).thenAnswer(
            new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    TryGetResult<Object> result = new TryGetResult<>();
                    if (evidenceData.containsKey((String)invocation.getArgument(0))) {
                        result.setValue(evidenceData.get((String)invocation.getArgument(0)));
                    }
                    return result;
                }
            }
        );

        return data;
    }
}
