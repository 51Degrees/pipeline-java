package fiftyone.pipeline.core.configuration;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;

/**
 * Instantiate pipeline options from an XML config file
 */
public class PipelineOptionsFactory {
    public static PipelineOptions getOptionsFromFile(String configFile) throws Exception{
        return getOptionsFromFile(new File(configFile));
    }
    public static PipelineOptions getOptionsFromFile(File configFile) throws Exception{
        JAXBContext jaxbContext = JAXBContext.newInstance(PipelineOptions.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (PipelineOptions) unmarshaller.unmarshal(configFile);
    }
}
