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

package fiftyone.pipeline.engines.testhelpers.flowelements;

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;
import fiftyone.pipeline.engines.testhelpers.data.EmptyEngineData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmptyEngine
    extends AspectEngineBase<EmptyEngineData, AspectPropertyMetaData> {

    private long processCost = 0;
    private Exception exception = null;
    private List<AspectPropertyMetaData> properties = null;
    private EvidenceKeyFilterWhitelist evidenceWhitelist =
        new EvidenceKeyFilterWhitelist(Arrays.asList("test.value"));

    private final TypedKey<EmptyEngineData> typedKey = new TypedKeyDefault<>(getElementDataKey(), EmptyEngineData.class);

    public EmptyEngine(
        Logger logger,
        ElementDataFactory<EmptyEngineData> aspectDataFactory) {
        super(logger, aspectDataFactory);

        properties = Arrays.asList(
            (AspectPropertyMetaData) new AspectPropertyMetaDataDefault("valueone", this, "", int.class, new ArrayList<String>(), true),
            (AspectPropertyMetaData) new AspectPropertyMetaDataDefault("valuetwo", this, "", int.class, new ArrayList<String>(), true)
        );
    }


    public long getProcessCost() {
        return processCost;
    }
    public void setProcessCost(long milliseconds) {
        processCost = milliseconds;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public List<AspectPropertyMetaData> getProperties() {
        return properties;
    }

    @Override
    public String getElementDataKey() {
        return "empty-aspect";
    }

    @Override
    public TypedKey<EmptyEngineData> getTypedDataKey() {
        return typedKey;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceWhitelist;
    }

    @Override
    public String getDataSourceTier() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void processEngine(FlowData data, EmptyEngineData aspectData) throws Exception {
        if (exception != null) {
            throw exception;
        }
        aspectData.setValueOne(1);
        if (processCost > 0) {
            Thread.sleep(processCost);
        }
        aspectData.setValueTwo(2);
    }

    @Override
    protected void unmanagedResourcesCleanup() {

    }
}
