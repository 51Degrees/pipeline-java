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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyReason;
import fiftyone.pipeline.engines.services.MissingPropertyResult;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AspectDataBaseTests {

    private TestData data;
    private AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine;
    private FlowData flowData;
    private MissingPropertyService missingPropertyService;

    @SuppressWarnings("unchecked")
    @Before
    public void Initisalise() {
        engine = mock(AspectEngine.class);
        flowData = mock(FlowData.class);
        missingPropertyService = mock(MissingPropertyService.class);
        when(missingPropertyService.getMissingPropertyReason(any(String.class), any(List.class)))
            .thenReturn(new MissingPropertyResult(
                MissingPropertyReason.Unknown,
                "TEST"
            ));
        data = new TestData(
            mock(Logger.class),
            flowData,
            engine,
            missingPropertyService
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void AspectData_Indexer_NullKey() {
        data.get(null);
    }

    @Test
    public void AspectData_Indexer_PutAndGet() {
        data.put("testproperty", "TestValue");
        Object result = data.get("testproperty");
        assertEquals("TestValue", result);
    }

    @Test(expected = PropertyMissingException.class)
    public void AspectData_Indexer_GetMissing() {
        data.get("testproperty");
    }

    private class TestData extends AspectDataBase {
        public TestData(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine,
            MissingPropertyService missingPropertyService) {
            super(logger, flowData, engine, missingPropertyService);
        }
    }
}
