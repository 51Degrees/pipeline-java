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

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MissingPropertyServiceTests {

    private MissingPropertyService service;

    @Before
    public void Initialise() {
        service = MissingPropertyServiceDefault.getInstance();
    }

    /**
     * Check that an "upgrade required" reason is returned when a property is
     * available, but in another data tier.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void MissingPropertyService_GetReason_Upgrade() {
        // Arrange
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine = mock(AspectEngine.class);
        when(engine.getDataSourceTier()).thenReturn("lite");
        when(engine.getElementDataKey()).thenReturn("testElement");
        configureProperty(engine);

        // Act
        MissingPropertyResult result = service.getMissingPropertyReason(
            "testProperty",
            engine);

        // Assert
        assertEquals(
            MissingPropertyReason.DataFileUpgradeRequired,
            result.getReason());
        assertEquals(
            String.format(
                Constants.MissingPropertyMessages.PREFIX,
                "testProperty",
                "testElement") +
                String.format(
                    Constants.MissingPropertyMessages.DATA_UPGRADE_REQUIRED,
                    stringJoin((engine.getProperties().get(0)).getDataTiersWherePresent(), ","),
                    engine.getClass().getSimpleName()),
            result.getDescription());

    }

    /**
     * Check that an "excluded from configuration" reason is returned if the
     * property exists in the data tier, but was not included in the list of
     * required properties when configuring the engine.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void MissingPropertyService_GetReason_Excluded() {
        // Arrange
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine = mock(AspectEngine.class);
        when(engine.getDataSourceTier()).thenReturn("premium");
        when(engine.getElementDataKey()).thenReturn("testElement");
        configureProperty(engine, false);

        // Act
        MissingPropertyResult result = service.getMissingPropertyReason(
            "testProperty",
            engine);

        // Assert
        assertEquals(
            MissingPropertyReason.PropertyExcludedFromEngineConfiguration,
            result.getReason());
        assertEquals(
            String.format(
                Constants.MissingPropertyMessages.PREFIX,
                "testProperty",
                "testElement") +
                Constants.MissingPropertyMessages.PROPERTY_EXCLUDED,
            result.getDescription());

    }

    /**
     * Check that an "unknown" reason is returned when the property does not
     * exist in the engine.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void MissingPropertyService_GetReason_NotInEngine() {
        // Arrange
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine = mock(AspectEngine.class);
        when(engine.getDataSourceTier()).thenReturn("premium");
        when(engine.getElementDataKey()).thenReturn("testElement");
        configureProperty(engine, false);

        // Act
        MissingPropertyResult result = service.getMissingPropertyReason(
            "otherProperty",
            engine);

        // Assert
        assertEquals(MissingPropertyReason.Unknown, result.getReason());
        assertEquals(
            String.format(
                Constants.MissingPropertyMessages.PREFIX,
                "otherProperty",
                "testElement") +
            Constants.MissingPropertyMessages.UNKNOWN,
            result.getDescription());
    }

    /**
     * Check that a "product not in resource" reason is returned when a cloud
     * engine does not contain the product.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void MissingPropertyService_GetReason_ProductNotInResource() {
        // Arrange
        CloudAspectEngine<? extends AspectData, AspectPropertyMetaData> engine = mock(CloudAspectEngine.class);
        when(engine.getElementDataKey()).thenReturn("testElement");
        when(engine.getProperties()).thenReturn(Collections.emptyList());

        // Act
        MissingPropertyResult result = service.getMissingPropertyReason(
            "otherProperty",
            engine);

        // Assert
        assertEquals(
            MissingPropertyReason.ProductNotAccessibleWithResourceKey,
            result.getReason());
        assertEquals(
            String.format(
                Constants.MissingPropertyMessages.PREFIX,
                "otherProperty",
                "testElement") +
            String.format(
                Constants.MissingPropertyMessages.PRODUCT_NOT_IN_CLOUD_RESOURCE,
                "testElement"),
            result.getDescription());
    }

    /**
     * Check that a "property not in resource" reason is returned when a cloud
     * engine does contain the product, but not the property.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void MissingPropertyService_GetReason_PropertyNotInResource() {
        // Arrange
        CloudAspectEngine<? extends AspectData, AspectPropertyMetaData> engine = mock(CloudAspectEngine.class);
        when(engine.getElementDataKey()).thenReturn("testElement");
        configureProperty(engine);

        // Act
        MissingPropertyResult result = service.getMissingPropertyReason(
            "otherProperty",
            engine);

        // Assert
        assertEquals(
            MissingPropertyReason.PropertyNotAccessibleWithResourceKey,
            result.getReason());
        assertEquals(
            String.format(
                Constants.MissingPropertyMessages.PREFIX,
                "otherProperty",
                "testElement") +
                String.format(
                    Constants.MissingPropertyMessages.PROPERTY_NOT_IN_CLOUD_RESOURCE,
                    "testElement",
                    "testProperty"),
            result.getDescription());
    }

    /**
     * Check that an "unknown" reason is returned when none of the above are
     * true.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void MissingPropertyService_GetReason_Unknown() {
        // Arrange
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine = mock(AspectEngine.class);
        when(engine.getDataSourceTier()).thenReturn("premium");
        when(engine.getElementDataKey()).thenReturn("testElement");
        configureProperty(engine);

        // Act
        MissingPropertyResult result = service.getMissingPropertyReason(
            "testProperty",
            engine);

        // Assert
        assertEquals(MissingPropertyReason.Unknown, result.getReason());
        assertEquals(
            String.format(
                Constants.MissingPropertyMessages.PREFIX,
                "testProperty",
                "testElement") +
                Constants.MissingPropertyMessages.UNKNOWN,
            result.getDescription());
    }

    private void configureProperty(AspectEngine<? extends AspectData, AspectPropertyMetaData> engine) {
        configureProperty(engine, true);
    }

    private void configureProperty(
        AspectEngine<? extends AspectData, AspectPropertyMetaData> engine,
        boolean propertyAvailable) {
        List<String> dataFiles = Arrays.asList("premium", "enterprise");
        AspectPropertyMetaData property = new AspectPropertyMetaDataDefault(
            "testProperty",
            engine,
            "",
            String.class,
            dataFiles,
            propertyAvailable);
        List<AspectPropertyMetaData> propertyList = Arrays.asList(property);
        when(engine.getProperties()).thenReturn(propertyList);
    }
}
