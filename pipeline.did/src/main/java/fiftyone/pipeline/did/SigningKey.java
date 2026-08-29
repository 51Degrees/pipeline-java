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

import java.time.Instant;
import java.util.Objects;

/**
 * One entry of the cloud's published signing key schedule: the public key
 * and the moment it came, or comes, into force. A key stays in force until
 * the next one starts, so the schedule never has a gap, and keys are
 * published ahead of their start.
 */
public final class SigningKey {

    private final Instant startsAt;
    private final String publicKeyPem;

    /**
     * @param startsAt     when the key comes into force, UTC
     * @param publicKeyPem the public key in SPKI PEM form
     */
    public SigningKey(Instant startsAt, String publicKeyPem) {
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt");
        this.publicKeyPem = Objects.requireNonNull(
            publicKeyPem, "publicKeyPem");
    }

    /** @return when the key comes into force, UTC */
    public Instant getStartsAt() {
        return startsAt;
    }

    /** @return the public key in SPKI PEM form */
    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    @Override
    public String toString() {
        return "SigningKey from " + startsAt;
    }
}
