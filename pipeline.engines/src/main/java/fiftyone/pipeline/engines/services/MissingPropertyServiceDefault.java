/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2019 51 Degrees Mobile Experts Limited, 5 Charlotte Close,
 * Caversham, Reading, Berkshire, United Kingdom RG4 7BY.
 *
 * This Original Work is licensed under the European Union Public Licence (EUPL) 
 * v.1.2 and is subject to its terms as set out below.
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

import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;

import java.util.List;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;

public class MissingPropertyServiceDefault implements MissingPropertyService {

    /**
     * Singleton instance of the service.
     */
    private static MissingPropertyServiceDefault missingPropertyService = null;

    private static volatile Object lock = new Object();

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

    @Override
    public MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        AspectEngine engine) {

        MissingPropertyReason reason = MissingPropertyReason.Unknown;

        AspectPropertyMetaData property = null;

        for (AspectPropertyMetaData currentProperty : (List<AspectPropertyMetaData>) engine.getProperties()) {
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
                reason = MissingPropertyReason.PropertyExculdedFromEngineConfiguration;
            }
        }

        // Build the message string to return to the caller.
        StringBuilder message = new StringBuilder();
        message.append("Property '" + propertyName + "' is not present in the results.\n");
        switch (reason) {
            case DataFileUpgradeRequired:
                message.append("This is because your license and/or data file " +
                    "does not include this property. The property is available ");
                message.append("with the ");
                message.append(stringJoin(property.getDataTiersWherePresent(), ","));
                message.append(" license/data for the " + engine.getClass().getSimpleName());
                break;
            case PropertyExculdedFromEngineConfiguration:
                message.append("This is because the property has been " +
                    "excluded when configuring the engine.");
                break;
            case Unknown:
                message.append("The reason for this is unknown. Please " +
                    "check that the aspect and property name are correct.");
                break;
            default:
                break;
        }

        return new MissingPropertyResult(reason, message.toString());
    }

    @Override
    public MissingPropertyResult getMissingPropertyReason(String propertyName, List<AspectEngine> engines) {
        MissingPropertyResult result = null;
        for (AspectEngine engine : engines) {
            result = getMissingPropertyReason(propertyName, engine);
            if (result.getReason() != MissingPropertyReason.Unknown) {
                return result;
            }
        }
        return result;
    }
}
