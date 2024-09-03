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

package fiftyone.pipeline.core.configuration;

import fiftyone.pipeline.util.FileFinder;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

/**
 * Instantiate pipeline options from an XML config file
 */
public class PipelineOptionsFactory {

    public static final String PIPELINE_OPTIONS_SCHEMA = "pipelineOptions.xsd";

    public static PipelineOptions getOptionsFromFile(String configFile) throws Exception{
        return getOptionsFromFile(new File(configFile));
    }
    public static PipelineOptions getOptionsFromFile(File configFile) throws Exception{
        JAXBContext jaxbContext = JAXBContext.newInstance(PipelineOptions.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        //Setup schema validator
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema optionsSchema = sf.newSchema(PipelineOptionsFactory.class.getClassLoader().getResource(PIPELINE_OPTIONS_SCHEMA));
        unmarshaller.setSchema(optionsSchema);

        return (PipelineOptions) unmarshaller.unmarshal(configFile);
    }
}
