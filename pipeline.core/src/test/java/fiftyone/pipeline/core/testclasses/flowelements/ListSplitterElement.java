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

import fiftyone.pipeline.annotations.BuildArg;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.testclasses.data.ListSplitterElementData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;

public class ListSplitterElement
    extends FlowElementBase<ListSplitterElementData, ElementPropertyMetaData> {

    public List<String> evidenceKeys = Arrays.asList("list-to-split");
    private EvidenceKeyFilterWhitelist evidenceKeyFilter;
    private String delimiter;
    private int maxLength;

    public ListSplitterElement(@BuildArg("delimiter") String delimiter, @BuildArg("maxLength") int maxLength) {
        super(
            mock(Logger.class),
            new ElementDataFactory<ListSplitterElementData>() {
                @Override
                public ListSplitterElementData create(FlowData flowData, FlowElement<ListSplitterElementData, ?> flowElement) {
                    return new ListSplitterElementData(
                        mock(Logger.class),
                        flowData);
                }
            });
        this.delimiter = delimiter;
        this.maxLength = maxLength;
        evidenceKeyFilter = new EvidenceKeyFilterWhitelist(evidenceKeys);
    }

    public String getDelimiter() {
        return delimiter;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public String getElementDataKey() {
        return "listSplitter";
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
        ListSplitterElementData elementData = data.getOrAdd(
            getTypedDataKey(),
            getDataFactory());
        String source = (String) data.getEvidence().get(evidenceKeys.get(0));
        String[] results = source.split(Pattern.quote(delimiter));

        List<String> result = new ArrayList<>();
        for (String string : results) {
            while (string.length() > maxLength) {
                // Take the first _maxLength characters and add them
                // to the element data result.
                result.add(string.substring(0, maxLength));
                // Remove the first _maxLength characters from the
                // string and repeat.
                string = string.substring(maxLength);
            }
            // Add the string to the element data result.
            if (string.length() > 0) {
                result.add(string);
            }
        }
        elementData.setResult(result);
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
