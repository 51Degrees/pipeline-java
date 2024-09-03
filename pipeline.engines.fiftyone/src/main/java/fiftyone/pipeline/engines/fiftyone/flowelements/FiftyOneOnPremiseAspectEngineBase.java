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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.fiftyone.data.*;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngineBase;
import fiftyone.pipeline.util.Types;
import org.slf4j.Logger;

/**
 * 51Degrees specific engine base class. This adds the concept of license keys
 * to the standard Engine base class.
 * @param <TData> the type of aspect data that the flow element will write to
 * @param <TProperty> the type of meta data that the flow element will supply
 *                    about the properties it populates.
 */
public abstract class FiftyOneOnPremiseAspectEngineBase<
    TData extends AspectData,
    TProperty extends FiftyOneAspectPropertyMetaData>
    extends OnPremiseAspectEngineBase<TData, TProperty>
    implements FiftyOneAspectEngine<TData, TProperty> {

    /**
     * Construct a new instance of the {@link FiftyOneOnPremiseAspectEngineBase}.
     * @param logger logger instance to use for logging
     * @param aspectDataFactory the factory to use when creating a TData
     *                          instance
     * @param tempDataFilePath the file where a temporary data file copy
     *                        will be stored if one is created
     */
    public FiftyOneOnPremiseAspectEngineBase(
        Logger logger,
        ElementDataFactory<TData> aspectDataFactory,
        String tempDataFilePath) {
        super(logger, aspectDataFactory, tempDataFilePath);
    }


    @Override
    public TypedKey<TData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(
                getElementDataKey(),
                Types.findSubClassParameterType(
                    this,
                    FiftyOneOnPremiseAspectEngineBase.class,
                    0));
        }
        return typedKey;
    }

    @Override
    public abstract CloseableIterable<ProfileMetaData> getProfiles();

    @Override
    public ProfileMetaData getProfile(int profileId) throws Exception {
        try(CloseableIterable<ProfileMetaData> profiles = getProfiles()){
          for (ProfileMetaData profile : profiles) {
            if (profile.getProfileId() == profileId) {
                return profile;
            }
          }
        }
        return null;
    }

    @Override
    public abstract CloseableIterable<ComponentMetaData> getComponents();

    @Override
    public ComponentMetaData getComponent(String name) throws Exception {
        try(CloseableIterable<ComponentMetaData> metaDataComponents = getComponents()){
          for (ComponentMetaData component : metaDataComponents) {
            if (component.getName().equalsIgnoreCase(name)) {
                return component;
            }
          }
        }
        return null;
    }

    @Override
    public abstract CloseableIterable<ValueMetaData> getValues();

    @Override
    public ValueMetaData getValue(String propertyName, String valueName) throws Exception {
        try (CloseableIterable<ValueMetaData> values = getValues()) {
          for (ValueMetaData value : values) {
            if (value.getProperty().getName().equalsIgnoreCase(propertyName) &&
                value.getName().equalsIgnoreCase(valueName)) {
                return value;
            }
          }
        }
        return null;
    }
}
