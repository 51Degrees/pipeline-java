package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
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
 * A specialised type of {@link CloudAspectEngineBase<TData>} that has the
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
    protected void processEngine(FlowData data, TData aspectData) {
        CloudRequestData requestData = data.getFromElement(
            getRequestEngine().getInstance());
        String json = requestData == null ? null : requestData.getJsonResponse();

        if (json == null || json.isEmpty()) {
            throw new PipelineConfigurationException(
                "Json response from cloud request engine is null. " +
                "This is probably because there is not a " +
                "'CloudRequestEngine' before the '" +
                this.getClass().getSimpleName() +
                "' in the Pipeline. This engine will be unable " +
                "to produce results until this is corrected.");
        }
        else {
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
