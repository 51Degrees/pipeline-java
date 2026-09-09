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

package pipeline.developerexamples.fodid;

import com.swancommunity.owid.Creator;
import com.swancommunity.owid.Crypto;
import fiftyone.pipeline.did.FodId;

import java.util.Arrays;

/**
 * Offline example for the 51Did ({@link FodId}) reader.
 * <p>
 * The 51Degrees Cloud service issues real 51Dids. To keep this example
 * self-contained and offline, it builds a sample 51Did in process - generate
 * an ECDSA P-256 key pair, sign a canonical 37-byte payload - then parses it
 * back with {@link FodId} and prints the three payload fields.
 * <p>
 * It also demonstrates the headline use case: a 51Did is re-issued fresh on
 * every call (the envelope, hence the base64, changes), but the match key
 * is stable. <b>Compare match keys, never envelopes.</b>
 */
public class Main {

    private static final String DOMAIN = "51degrees.com";

    // This example stands in for the cloud, so it builds a payload byte by
    // byte, which is a writer's job and not a reader's. The layout is not
    // part of the reader's public surface, so the offsets are spelled out
    // here from the specification at
    // https://github.com/51Degrees/specifications/blob/main/did-specification/identifier-layout.md
    // Code that reads a 51Did should use the typed accessors instead.
    private static final int LICENSE_ID_OFFSET = 1;
    private static final int MATCH_KEY_OFFSET = 5;
    private static final int MATCH_KEY_LENGTH = 32;
    private static final int PAYLOAD_LENGTH =
            MATCH_KEY_OFFSET + MATCH_KEY_LENGTH;

    public static class Example {

        public void run() throws Exception {
            // Generate a key pair and a signer entirely in process.
            Crypto crypto = Crypto.generate();
            Creator creator = Creator.create(DOMAIN, crypto);

            byte[] payload = samplePayload();

            // Issue a 51Did over the payload and parse it back.
            FodId fodId = FodId.fromBase64(issue(creator, payload));

            System.out.println("51Did parsed from base64:");
            System.out.println("  Domain       : " + fodId.getDomain());
            System.out.println("  Type         : " + fodId.getType());
            System.out.println("  Usage        : " + fodId.getUsage());
            System.out.println("  From consent : "
                    + fodId.isUsageFromConsent());
            System.out.println("  LicenseId    : " + fodId.getLicenseId());
            System.out.println("  Match key    : "
                    + toHex(fodId.getMatchKey()));
            System.out.println("  Verifies     : "
                    + fodId.verify(crypto.publicKeyPem()));

            // Issue the SAME payload again: a separate envelope, same match
            // key.
            FodId reissued = FodId.fromBase64(issue(creator, payload));
            boolean sameEnvelope =
                    fodId.asBase64().equals(reissued.asBase64());
            boolean sameMatchKey =
                    Arrays.equals(fodId.getMatchKey(), reissued.getMatchKey());

            System.out.println();
            System.out.println("Same payload, re-issued:");
            System.out.println("  Same envelope (base64) : " + sameEnvelope);
            System.out.println("  Same match key         : " + sameMatchKey);

            // The reader's whole purpose: the match key is the stable,
            // comparable part while the envelope is not.
            if (sameEnvelope || !sameMatchKey) {
                throw new IllegalStateException(
                        "Expected a different envelope but the same match "
                        + "key across reissues.");
            }
        }

        /**
         * Issues (signs) a 51Did over the payload and returns it as base64.
         * The creator stamps the date and signs in one step, which is the
         * only way an OWID comes into being.
         */
        private String issue(Creator creator, byte[] payload)
                throws Exception {
            return creator.createBytes(payload).asBase64();
        }

        /**
         * A canonical 37-byte Probabilistic payload with flags 0x03, License Id
         * 0x12345678 (little-endian) and a 32-byte match key 0x20..0x3F.
         */
        private byte[] samplePayload() {
            byte[] payload = new byte[PAYLOAD_LENGTH];
            // Flags 0b0000_0011, being standard marketing usage stated by
            // the caller, on a Probabilistic identifier (bits 6-7 zero).
            payload[0] = 0b0000_0011;
            payload[LICENSE_ID_OFFSET] = 0x78;
            payload[LICENSE_ID_OFFSET + 1] = 0x56;
            payload[LICENSE_ID_OFFSET + 2] = 0x34;
            payload[LICENSE_ID_OFFSET + 3] = 0x12;
            for (int i = 0; i < MATCH_KEY_LENGTH; i++) {
                payload[MATCH_KEY_OFFSET + i] = (byte) (0x20 + i);
            }
            return payload;
        }

        private static String toHex(byte[] bytes) {
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b & 0xFF));
            }
            return builder.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        new Example().run();
    }
}
