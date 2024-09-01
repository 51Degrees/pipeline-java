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

package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class FiftyOneAspectPropertyMetaDataDefault
    extends AspectPropertyMetaDataDefault
    implements FiftyOneAspectPropertyMetaData {

    private final String url;
    private final byte displayOrder;
    private final boolean mandatory;
    private final boolean list;
    private final boolean obsolete;
    private final boolean show;
    private final boolean showValues;
    private final String description;
    private final ComponentMetaData component;
    private final Iterable<ValueMetaDataDefault> values;
    private final ValueMetaDataDefault defaultValue;

    public FiftyOneAspectPropertyMetaDataDefault(
        String name,
        FlowElement<?,?> element,
        String category,
        Class<?> type,
        List<String> dataTiersWherePresent,
        boolean available,
        String url,
        byte displayOrder,
        boolean mandatory,
        boolean list,
        boolean obsolete,
        boolean show,
        boolean showValues,
        String description,
        ComponentMetaData component,
        Iterable<ValueMetaDataDefault> values,
        ValueMetaDataDefault defaultValue) {
        super(name, element, category, type, dataTiersWherePresent, available);
        this.url = url;
        this.displayOrder = displayOrder;
        this.mandatory = mandatory;
        this.list = list;
        this.obsolete = obsolete;
        this.show = show;
        this.showValues = showValues;
        this.description = description;
        this.component = component;
        this.values = values;
        this.defaultValue = defaultValue;

        // Set the property of the default value to this.
        defaultValue.setProperty(this);
        for (ValueMetaDataDefault value : this.values) {
            value.setProperty(this);
        }
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public byte getDisplayOrder() {
        return displayOrder;
    }

    @Override
    public boolean getMandatory() {
        return mandatory;
    }

    @Override
    public boolean getList() {
        return list;
    }

    @Override
    public boolean getObsolete() {
        return obsolete;
    }

    @Override
    public boolean getShow() {
        return show;
    }

    @Override
    public boolean getShowValues() {
        return showValues;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ComponentMetaData getComponent() {
        return component;
    }

    @Override
    public Iterable<ValueMetaData> getValues() {
        return () -> {
            Iterator<ValueMetaDataDefault> innerValues =
                    values.iterator();
            return new Iterator<ValueMetaData>() {
                @Override
                public boolean hasNext() {
                    return innerValues.hasNext();
                }

                @Override
                public ValueMetaData next() {
                    return innerValues.next();
                }
            };
        };
    }

    @Override
    public ValueMetaData getDefaultValue() {
        return defaultValue;
    }

    @Override
    public ValueMetaData getValue(String valueName) {
        for (ValueMetaData value : values) {
            if (value.getName().equals(valueName)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
