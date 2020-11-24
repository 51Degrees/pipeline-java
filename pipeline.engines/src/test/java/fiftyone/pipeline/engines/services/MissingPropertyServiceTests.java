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

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;
import javafx.scene.control.SelectionMode;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
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

    @Test
    public void MissingPropertyService_GetReason_Upgrade() {
        // Arrange
        AspectEngine engine = mock(AspectEngine.class);
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
                    stringJoin(((AspectPropertyMetaData)engine.getProperties().get(0)).getDataTiersWherePresent(), ","),
                    engine.getClass().getSimpleName()),
            result.getDescription());

    }

    @Test
    public void MissingPropertyService_GetReason_Excluded() {
        // Arrange
        AspectEngine engine = mock(AspectEngine.class);
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

    @Test
    public void MissingPropertyService_GetReason_NotInEngine() {
        // Arrange
        AspectEngine engine = mock(AspectEngine.class);
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

    @Test
    public void MissingPropertyService_GetReason_ProductNotInResource() {
        // Arrange
        CloudAspectEngine engine = mock(CloudAspectEngine.class);
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

    @Test
    public void MissingPropertyService_GetReason_PropertyNotInResource() {
        // Arrange
        CloudAspectEngine engine = mock(CloudAspectEngine.class);
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

    @Test
    public void MissingPropertyService_GetReason_Unknown() {
        // Arrange
        AspectEngine engine = mock(AspectEngine.class);
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

    private void configureProperty(AspectEngine engine) {
        configureProperty(engine, true);
    }

    private void configureProperty(
        AspectEngine engine,
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
