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

package pipeline.developerexamples.onpremiseengine.data;

import java.util.Calendar;

//! [class]
public class StarSign {
    private final Calendar end;
    private final Calendar start;
    private final String name;

    public StarSign(String name, String start, String end) {
        this.name = name;
        this.start = Calendar.getInstance();
        String[] startDate = start.split("/");
        this.start.set(
            0,
            Integer.parseInt(startDate[1]) - 1,
            Integer.parseInt(startDate[0]));
        this.end = Calendar.getInstance();
        String[] endDate = end.split("/");
        this.end.set(
            0,
            Integer.parseInt(endDate[1]) - 1,
            Integer.parseInt(endDate[0]));
    }

    public String getName() {
        return name;
    }

    public Calendar getStart() {
        return start;
    }

    public Calendar getEnd() {
        return end;
    }
}
//! [class]
