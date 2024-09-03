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

package fiftyone.pipeline.developerexamples.clientsideelement.flowelements;

import fiftyone.pipeline.developerexamples.clientsideelement.data.StarSign;
import fiftyone.pipeline.developerexamples.clientsideelement.data.StarSignData;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import org.slf4j.Logger;

import java.util.*;

/**
 * Note that this example is not runnable. It serves as an example of how to
 * write a FlowElement, and can be seen running in the
 * pipeline.developer-examples.clientside-element-mvc example.
 */
//! [class]
//! [constructor]
public class SimpleClientSideElement extends FlowElementBase<StarSignData, ElementPropertyMetaData> {

    public SimpleClientSideElement(
        Logger logger,
        ElementDataFactory<StarSignData> elementDataFactory) {
        super(logger, elementDataFactory);
        init();
    }
//! [constructor]

    private static final String[][] starSignData = {
        {"Aries","21/03","19/04"},
        {"Taurus","20/04","20/05"},
        {"Gemini","21/05","20/06"},
        {"Cancer","21/06","22/07"},
        {"Leo","23/07","22/08"},
        {"Virgo","23/08","22/09"},
        {"Libra","23/09","22/10"},
        {"Scorpio","23/10","21/11"},
        {"Sagittarius","22/11","21/12"},
        {"Capricorn","22/12","19/01"},
        {"Aquarius","20/01","18/02"},
        {"Pisces","19/02","20/03"}
    };

    private List<StarSign> starSigns;

//! [init]
    private void init() {
        List<StarSign> starSigns = new ArrayList<>();
        for (String[] starSign : starSignData) {
            starSigns.add(new StarSign(
                starSign[0],
                starSign[1],
                starSign[2]));
        }
        this.starSigns = starSigns;
    }
//! [init]

    @Override
    protected void processInternal(FlowData data) throws Exception {
        // Create a new StarSignData, and cast to StarSignDataInternal so the 'setter' is available.
        StarSignDataInternal elementData = (StarSignDataInternal)data.getOrAdd(getElementDataKey(),getDataFactory());

        boolean validDateOfBirth = false;

        TryGetResult<String> dateString = data.tryGetEvidence("cookie.date-of-birth", String.class);
        Calendar monthAndDay = Calendar.getInstance();

        if (dateString.hasValue()) {
            // "date-of-birth" is there, so parse it.
            String[] dateSections = dateString.getValue().split("/");
            try {
                monthAndDay.set(
                    0,
                    Integer.parseInt(dateSections[1]) - 1,
                    Integer.parseInt(dateSections[0]));
                validDateOfBirth = true;
            }
            catch (Exception e) {
                logger.warn("Exception getting the date of birth", e);
            }
        }
        if (validDateOfBirth) {
            // "date-of-birth" is valid, so set the star sign.
            for (StarSign starSign : starSigns) {
                if (monthAndDay.compareTo(starSign.getStart()) >= 0 &&
                    monthAndDay.compareTo(starSign.getEnd()) <= 0) {
                    elementData.setStarSign(starSign.getName());
                    break;
                }
            }
            // No need to run the client side code again.
            elementData.setDobJavaScript(new JavaScript(""));

        }
        else
        {
            // "date-of-birth" is not there, so set the star sign to unknown.
            elementData.setStarSign("Unknown");
            // Set the client side JavaScript to get the date of birth.
            elementData.setDobJavaScript(new JavaScript(
                "var dob = window.prompt('Enter your date of birth.','dd/mm/yyyy');" +
                "if (dob != null) {" +
                "document.cookie='date-of-birth='+dob;" +
                "location.reload();" +
                "}"));
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
            Collections.singletonList("cookie.date-of-birth"),
            String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        // The properties which will be returned are "starsign" and the
        // JavaScript to get the date of birth.
        return Arrays.asList(
            (ElementPropertyMetaData)new ElementPropertyMetaDataDefault(
                "starsign",
                this,
                "starsign",
                String.class,
                true),
            new ElementPropertyMetaDataDefault(
                "dobjavascript",
                this,
                "javascript",
                JavaScript.class,
                true));
    }

    @Override
    protected void managedResourcesCleanup() {
        // Nothing to clean up here.
    }

    @Override
    protected void unmanagedResourcesCleanup() {
        // Nothing to clean up here.
    }
}
//! [class]
