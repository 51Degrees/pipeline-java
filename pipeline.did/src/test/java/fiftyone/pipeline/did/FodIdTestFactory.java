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
import com.swancommunity.owid.OwidParseResult;
import com.swancommunity.owid.Version;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Builds signed test envelopes over chosen payloads, with one key pair per
 * factory so that the client tests can stand up a schedule of keys.
 * <p>
 * Two routes produce an envelope. {@link #signedOwid(byte[])} asks the OWID
 * library's own {@link Creator} to stamp the date and sign, which is the
 * route production code takes. {@link #signedOwidAt} writes the envelope
 * bytes by hand, signs them with the same key, and reads them back through
 * the library, which is the only way to choose the date, the version or the
 * domain, and the only way to produce bytes the library would refuse.
 */
final class FodIdTestFactory {

    static final String TEST_DOMAIN = "51degrees.com";

    static final int CANONICAL_FLAGS = 0xA5;

    static final long CANONICAL_LICENSE_ID = 0x12345678L;

    static final byte[] CANONICAL_MATCH_KEY = canonicalMatchKey();

    static final Instant DATE_ORIGIN = Instant.parse("2020-01-01T00:00:00Z");

    private final Creator creator;

    final Crypto crypto;

    final String publicPem;

    FodIdTestFactory() throws OwidException {
        this(Crypto.generate());
    }

    FodIdTestFactory(Crypto crypto) throws OwidException {
        this.crypto = crypto;
        this.publicPem = crypto.publicKeyPem();
        this.creator = Creator.create(TEST_DOMAIN, crypto);
    }

    private static byte[] canonicalMatchKey() {
        byte[] matchKey = new byte[FodId.MATCH_KEY_LENGTH];
        for (int i = 0; i < matchKey.length; i++) {
            matchKey[i] = (byte) (0x20 + i);
        }
        return matchKey;
    }

    static byte[] canonicalPayload() {
        byte[] payload = new byte[FodId.PAYLOAD_LENGTH];
        payload[FodId.FLAGS_OFFSET] = (byte) CANONICAL_FLAGS;
        writeCanonicalLicenseId(payload);
        System.arraycopy(
            CANONICAL_MATCH_KEY, 0, payload,
            FodId.MATCH_KEY_OFFSET, FodId.MATCH_KEY_LENGTH);
        return payload;
    }

    static byte[] canonicalRandomPayload() {
        byte[] payload = new byte[FodId.RANDOM_PAYLOAD_LENGTH];
        payload[FodId.FLAGS_OFFSET] = (byte) ((1 << 6) | 0b001);
        writeCanonicalLicenseId(payload);
        for (int i = 0; i < FodId.GUID_LENGTH; i++) {
            payload[FodId.MATCH_KEY_OFFSET + i] = (byte) (0x40 + i);
        }
        return payload;
    }

    static byte[] canonicalPayloadWithSection(int sectionLength) {
        byte[] payload = new byte[FodId.PAYLOAD_LENGTH + sectionLength];
        System.arraycopy(
            canonicalPayload(), 0, payload, 0, FodId.PAYLOAD_LENGTH);
        for (int i = FodId.PAYLOAD_LENGTH; i < payload.length; i++) {
            payload[i] = (byte) 0xCC;
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

    /** Signs the payload through the library's creator, dated now. */
    Owid signedOwid(byte[] payload) throws OwidException {
        return creator.createBytes(payload);
    }

    String signedOwidBase64(byte[] payload) throws OwidException {
        return signedOwid(payload).asBase64();
    }

    Owid signedOwidAt(byte[] payload, Instant date) throws OwidException {
        return signedOwidAt(payload, date, Version.VERSION3);
    }

    Owid signedOwidAt(byte[] payload, Instant date, Version version)
            throws OwidException {
        return signedOwidAt(payload, date, version, TEST_DOMAIN);
    }

    /**
     * Writes an envelope by hand at the chosen date, version and domain,
     * signs it with this factory's key, and reads it back through the
     * library so the test holds exactly what production would.
     */
    Owid signedOwidAt(
            byte[] payload, Instant date, Version version, String domainName)
            throws OwidException {
        byte[] bytes = envelopeBytes(
            payload, date, version, domainName, payload.length);
        OwidParseResult read = Owid.parse(bytes);
        if (read.isSuccess() == false) {
            throw new OwidException(
                "The test envelope did not read back: " + read.getStatus());
        }
        return read.getValue();
    }

    /**
     * The raw bytes of a signed envelope, with the declared payload length
     * chosen separately from the payload so a test can produce a
     * declaration that disagrees with the bytes present. The signature is
     * over the bytes as written, so a matching declaration gives an envelope
     * that verifies with this factory's key.
     */
    byte[] envelopeBytes(
            byte[] payload,
            Instant date,
            Version version,
            String domainName,
            long declaredPayloadLength) throws OwidException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(version.asByte());
        byte[] domain = domainName.getBytes(StandardCharsets.UTF_8);
        out.write(domain, 0, domain.length);
        out.write(0);
        writeUInt32(out, Duration.between(DATE_ORIGIN, date).toMinutes());
        writeUInt32(out, declaredPayloadLength);
        out.write(payload, 0, payload.length);
        byte[] unsigned = out.toByteArray();
        byte[] signature = crypto.signByteArray(unsigned);
        out.write(signature, 0, signature.length);
        return out.toByteArray();
    }

    FodId fodIdAt(byte[] payload, Instant date) throws OwidException {
        return FodId.fromOwid(signedOwidAt(payload, date));
    }

    FodId fodIdAt(byte[] payload, Instant date, String domain)
            throws OwidException {
        return FodId.fromOwid(
            signedOwidAt(payload, date, Version.VERSION3, domain));
    }

    private static void writeUInt32(ByteArrayOutputStream out, long value) {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >> 8) & 0xFF));
        out.write((int) ((value >> 16) & 0xFF));
        out.write((int) ((value >> 24) & 0xFF));
    }
}
