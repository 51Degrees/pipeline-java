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

import com.swancommunity.owid.Creator;
import com.swancommunity.owid.Crypto;
import com.swancommunity.owid.Owid;
import com.swancommunity.owid.OwidException;

import java.time.Instant;

/**
 * Shared test helper for the 51Did tests. Generates a fresh ECDSA P-256 key
 * pair per instance and signs real OWID envelopes with it, and builds the
 * canonical payloads the tests assert against.
 */
final class FodIdTestFactory {

    /** The domain stamped into every signed test OWID. */
    static final String TEST_DOMAIN = "51degrees.com";

    /**
     * The canonical flags byte (0xA5): usage bits plus the HashedEmail type
     * tag in bits 6-7, so the 37-byte payload minimum applies.
     */
    static final int CANONICAL_FLAGS = 0xA5;

    /** The canonical little-endian License Id, 0x12345678. */
    static final long CANONICAL_LICENSE_ID = 0x12345678L;

    /** The canonical 32-byte hash value, bytes 0x20..0x3F. */
    static final byte[] CANONICAL_HASH = canonicalHash();

    private final Creator creator;

    /** The PEM-encoded public key matching the signing key. */
    final String publicPem;

    FodIdTestFactory() throws OwidException {
        Crypto crypto = Crypto.generate();
        this.publicPem = crypto.publicKeyPem();
        this.creator = Creator.create(TEST_DOMAIN, crypto);
    }

    private static byte[] canonicalHash() {
        byte[] hash = new byte[FodId.HASH_LENGTH];
        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte) (0x20 + i);
        }
        return hash;
    }

    /**
     * A canonical 37-byte 51Did payload: {@link #CANONICAL_FLAGS},
     * {@link #CANONICAL_LICENSE_ID} (little-endian) and {@link #CANONICAL_HASH}.
     */
    static byte[] canonicalPayload() {
        byte[] payload = new byte[FodId.PAYLOAD_LENGTH];
        payload[FodId.FLAGS_OFFSET] = (byte) CANONICAL_FLAGS;
        writeCanonicalLicenseId(payload);
        System.arraycopy(
            CANONICAL_HASH, 0, payload, FodId.HASH_OFFSET, FodId.HASH_LENGTH);
        return payload;
    }

    /**
     * A canonical 21-byte Random payload: the Random type tag in bits 6-7 plus
     * usage bits 0b001, {@link #CANONICAL_LICENSE_ID}, and a stable 16-byte
     * GUID block (0x40..0x4F).
     */
    static byte[] canonicalRandomPayload() {
        byte[] payload = new byte[FodId.RANDOM_PAYLOAD_LENGTH];
        payload[FodId.FLAGS_OFFSET] = (byte) ((1 << 6) | 0b001);
        writeCanonicalLicenseId(payload);
        for (int i = 0; i < FodId.GUID_LENGTH; i++) {
            payload[FodId.HASH_OFFSET + i] = (byte) (0x40 + i);
        }
        return payload;
    }

    private static void writeCanonicalLicenseId(byte[] payload) {
        // Little-endian: low byte first.
        payload[FodId.LICENSE_ID_OFFSET] = 0x78;
        payload[FodId.LICENSE_ID_OFFSET + 1] = 0x56;
        payload[FodId.LICENSE_ID_OFFSET + 2] = 0x34;
        payload[FodId.LICENSE_ID_OFFSET + 3] = 0x12;
    }

    /**
     * Creates and signs a real OWID with the given payload. Note that
     * {@code Creator.sign} stamps the date itself (current time, to the
     * minute), so callers that need distinct dates set them after signing.
     */
    Owid signedOwid(byte[] payload) throws OwidException {
        Owid owid = new Owid(TEST_DOMAIN, Instant.now(), payload);
        creator.sign(owid);
        return owid;
    }

    /** Signs the given payload and returns the OWID as base64. */
    String signedOwidBase64(byte[] payload) throws OwidException {
        return signedOwid(payload).asBase64();
    }
}
