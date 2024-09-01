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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.MultiProfileData;
import fiftyone.pipeline.util.Types;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A specialised type of {@link CloudAspectEngineBase} that has the
 * functionality to support returning a list of matching {@link AspectData}
 * profiles rather than a single item.
 * @param <TData> the type of {@link AspectData} returned by this engine
 * @param <TProfile> the type of items in the list returned by this engine
 */
public abstract class PropertyKeyedCloudEngineBase<
        TData extends MultiProfileData<TProfile>,
        TProfile extends AspectData>
        extends CloudAspectEngineBase<TData> {

    private final EvidenceKeyFilter evidenceKeyFilter =
        new EvidenceKeyFilterWhitelist(new ArrayList<String>());

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    public PropertyKeyedCloudEngineBase(
        Logger logger,
        ElementDataFactory<TData> aspectDataFactory) {
        super(logger, aspectDataFactory);
    }

    @Override
    protected void processCloudEngine(FlowData data, TData aspectData, String json) {
    	
        // Extract data from json to the aspectData instance.
        JSONObject map = new JSONObject(json);
        // Access the data relating to this engine.
        JSONObject propertyKeyed = map.getJSONObject(getElementDataKey());
        // Access the 'Profiles' property
        for (Object entry : propertyKeyed.getJSONArray("profiles")) {
            // Iterate through the devices, parsing each one and
            // adding it to the result.

            JSONObject propertyValues = new JSONObject(entry.toString());

            TProfile profile = createProfileData(data);
            // Get the meta-data for properties on device instances.
            List<ElementPropertyMetaData> propertyMetaData = null;
            for (AspectPropertyMetaData p : getProperties()) {
                if (p.getName().equalsIgnoreCase("profiles")) {
                    propertyMetaData = p.getItemProperties();
                }
            }
            Map<String, Object> profileData = createAPVMap(
                propertyValues.toMap(),
                propertyMetaData);

            profile.populateFromMap(profileData);
            //device.SetNoValueReasons(nullReasons);
            aspectData.addProfile(profile);
        }
    }

    @Override
    public TypedKey<TData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(getElementDataKey(), Types.findSubClassParameterType(this, PropertyKeyedCloudEngineBase.class, 0));
        }
        return typedKey;
    }

    protected abstract TProfile createProfileData(FlowData flowData);
}
