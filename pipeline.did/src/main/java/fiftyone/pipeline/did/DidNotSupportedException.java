/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
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

package fiftyone.pipeline.did;

/**
 * The host the client is pointed at does not offer the creator context. It
 * answered 404 to a redeem request, which the cloud's creator context
 * endpoints never do, so the caller should point the client at a host that
 * carries the feature rather than retry.
 */
public class DidNotSupportedException extends DidHttpException {

    private static final long serialVersionUID = 1L;

    /**
     * @param endpoint the API base the client was pointed at
     * @param body     the body the host answered with
     */
    public DidNotSupportedException(String endpoint, String body) {
        super("The service at " + endpoint + " does not support the "
            + "creator context.", 404, body);
    }
}
