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

package pipeline.developerexamples.onpremiseengine.flowelements;

import pipeline.developerexamples.onpremiseengine.data.StarSign;
import pipeline.developerexamples.onpremiseengine.data.StarSignData;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngineBase;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

//! [class]
//! [constructor]
public class SimpleOnPremiseEngine extends OnPremiseAspectEngineBase<StarSignData, AspectPropertyMetaData> {

    public SimpleOnPremiseEngine(
        String dataFile,
        Logger logger,
        ElementDataFactory<StarSignData> elementDataFactory,
        String tempDir) throws IOException {
        super(logger, elementDataFactory, tempDir);
        this.dataFile = dataFile;
        init();
    }
//! [constructor]

    private final String dataFile;

    private List<StarSign> starSigns;

//! [init]
    private void init() throws IOException {
        List<StarSign> starSigns = new ArrayList<>();
        try (FileReader fileReader = new FileReader(dataFile)) {
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split(",");
                    starSigns.add(new StarSign(
                        columns[0],
                        columns[1],
                        columns[2]));
                }
            }
        }
        this.starSigns = starSigns;
    }
//! [init]

    @Override
    public String getTempDataDirPath() {
        return dataFile;
    }

    @Override
    public Date getDataFilePublishedDate(String dataFileIdentifier) {
        return null;
    }

    @Override
    public Date getDataFileUpdateAvailableTime(String dataFileIdentifier) {
        return null;
    }

    @Override
    public void refreshData(String dataFileIdentifier) {
        try {
            init();
        } catch (IOException e) {
            logger.warn("There was an exception refreshing the data file" +
            "'" + dataFileIdentifier + "'",
                e);
        }
    }

    @Override
    public void refreshData(String dataFileIdentifier, byte[] data) {
        // Lets not implement this logic in this example.
    }

    @Override
    protected void processEngine(FlowData data, StarSignData aspectData) throws Exception {
        // Cast the StarSignData to StarSignDataInternal so the 'setter' is available.
        StarSignDataInternal starSignData = (StarSignDataInternal)aspectData;

        TryGetResult<Date> date = data.tryGetEvidence("date-of-birth", Date.class);
        if (date.hasValue()) {
            // "date-of-birth" is there, so set the star sign.
            Calendar dob = Calendar.getInstance();
            dob.setTime(date.getValue());
            Calendar monthAndDay = Calendar.getInstance();
            monthAndDay.set(
                0,
                dob.get(Calendar.MONTH),
                dob.get(Calendar.DATE));
            for (StarSign starSign : starSigns) {
                if (monthAndDay.compareTo(starSign.getStart()) >= 0 &&
                    monthAndDay.compareTo(starSign.getEnd()) <= 0) {
                    starSignData.setStarSign(starSign.getName());
                    break;
                }
            }
        }
        else
        {
            // "date-of-birth" is not there, so set the star sign to unknown.
            starSignData.setStarSign("Unknown");
        }
    }

    @Override
    public String getElementDataKey() {
        // The StarSignData will be stored with the key "starsign" in the FlowData.
        return "starsign";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        // The only item of evidence needed is "date-of-birth".
        return new EvidenceKeyFilterWhitelist(
            Collections.singletonList("date-of-birth"),
            String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public List<AspectPropertyMetaData> getProperties() {
        // The only property which will be returned is "starsign" which will be
        // an String.
        return Collections.singletonList(
            (AspectPropertyMetaData) new AspectPropertyMetaDataDefault(
                "starsign",
                this,
                "starsign",
                String.class,
                Collections.singletonList("free"),
                true));
    }

    @Override
    public String getDataSourceTier() {
        return "free";
    }

    @Override
    protected void unmanagedResourcesCleanup() {
        // Nothing to clean up here.
    }
}
//! [class]
