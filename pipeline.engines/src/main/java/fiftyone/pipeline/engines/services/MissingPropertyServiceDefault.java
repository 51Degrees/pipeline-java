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

package fiftyone.pipeline.engines.services;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;

import java.util.List;
import java.util.stream.Collectors;

import static fiftyone.pipeline.engines.Constants.MissingPropertyMessages;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Default implementation of the {@link MissingPropertyService} interface.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/properties.md#missing-properties">Specification</a>
 */
public class MissingPropertyServiceDefault implements MissingPropertyService {
    /**
     * Singleton instance of the service.
     */
    private static MissingPropertyServiceDefault missingPropertyService = null;

    private static final Object lock = new Object();

    /**
     * Private constructor to prevent new copies
     */
    private MissingPropertyServiceDefault() {
    }

    /**
     * Get the singleton instance of the default missing property service.
     *
     * @return singleton service
     */
    public static MissingPropertyService getInstance() {
        if (missingPropertyService == null) {
            synchronized (lock) {
                if (missingPropertyService == null) {
                    missingPropertyService = new MissingPropertyServiceDefault();
                }
            }
        }
        return missingPropertyService;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        AspectEngine engine) {

        MissingPropertyReason reason = MissingPropertyReason.Unknown;

        AspectPropertyMetaData property = null;

        // We know the property has extends AspectPropertyMetaData as it
        // is a constraint of the AspectEngine interface, so don't check this
        // cast
        for (Object currentPropertyObject : engine.getProperties()) {
            AspectPropertyMetaData currentProperty =
                (AspectPropertyMetaData)currentPropertyObject;
            if (currentProperty.getName().equalsIgnoreCase(propertyName)) {
                property = currentProperty;
                break;
            }
        }

        if (property != null) {
            // Check if the property is available in the data file that is
            // being used by the engine.
            boolean containsDataTier = false;
            for (String tier : property.getDataTiersWherePresent()) {
                if (tier.equals(engine.getDataSourceTier())) {
                    containsDataTier = true;
                    break;
                }
            }

            if (containsDataTier == false) {
                reason = MissingPropertyReason.DataFileUpgradeRequired;
            }
            // Check if the property is excluded from the results.
            else if (property.isAvailable() == false) {
                reason = MissingPropertyReason.PropertyExcludedFromEngineConfiguration;
            }
        }
        else {
            if (CloudAspectEngine.class.isAssignableFrom(engine.getClass())) {
                if (engine.getProperties().size() == 0) {
                    reason = MissingPropertyReason.ProductNotAccessibleWithResourceKey;
                }
                else {
                    reason = MissingPropertyReason.PropertyNotAccessibleWithResourceKey;
                }
            }
        }

        // Build the message string to return to the caller.
        StringBuilder message = new StringBuilder();
        message.append(String.format(
            MissingPropertyMessages.PREFIX,
            propertyName,
            engine.getElementDataKey()));
        switch (reason) {
            case DataFileUpgradeRequired:
                message.append(String.format(
                    MissingPropertyMessages.DATA_UPGRADE_REQUIRED,
                    stringJoin(
                        property.getDataTiersWherePresent(),
                        ","),
                    engine.getClass().getSimpleName()));
                break;
            case PropertyExcludedFromEngineConfiguration:
                message.append(MissingPropertyMessages.PROPERTY_EXCLUDED);
                break;
            case ProductNotAccessibleWithResourceKey:
                message.append(String.format(
                    MissingPropertyMessages.PRODUCT_NOT_IN_CLOUD_RESOURCE,
                    engine.getElementDataKey()));
                break;
            case PropertyNotAccessibleWithResourceKey:
                List<String> available =
                    ((List<ElementPropertyMetaData>)engine.getProperties())
                    .stream().map(ElementPropertyMetaData::getName)
                    .collect(Collectors.toList());
                message.append(String.format(
                    MissingPropertyMessages.PROPERTY_NOT_IN_CLOUD_RESOURCE,
                    engine.getElementDataKey(),
                    stringJoin(available, ", ")));
                break;
            case Unknown:
                message.append(MissingPropertyMessages.UNKNOWN);
                break;
            default:
                break;
        }

        return new MissingPropertyResult(reason, message.toString());
    }

    @Override
    public MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> engines) {
        MissingPropertyResult result = null;
        for (AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine : engines) {
            result = getMissingPropertyReason(propertyName, engine);
            if (result.getReason() != MissingPropertyReason.Unknown) {
                return result;
            }
        }
        return result;
    }
}
