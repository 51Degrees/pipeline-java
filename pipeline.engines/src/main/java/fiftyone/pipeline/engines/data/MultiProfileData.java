package fiftyone.pipeline.engines.data;

import java.util.List;

/**
 * Specialised implementation of {@link AspectData} where the instance contains
 * a list of profiles. The individual profiles must extend {@link AspectData}.
 * @param <T> the type of profile contained in the instance
 */
public interface MultiProfileData<T extends AspectData> extends AspectData {

    /**
     * Get the list of profiles.
     * @return all profiles contained in the instance
     */
    List<T> getProfiles();

    /**
     * Add a profile to this instance.
     * @param profile the profile to add
     */
    void addProfile(T profile);
}
