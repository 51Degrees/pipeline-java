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

import com.swancommunity.owid.OwidException;
import com.swancommunity.owid.OwidParseStatus;
import com.swancommunity.owid.OwidSignatureStatus;
import com.swancommunity.owid.OwidVerificationResult;
import com.swancommunity.owid.Version;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;

import static fiftyone.pipeline.did.FodIdTestFactory.CANONICAL_FLAGS;
import static fiftyone.pipeline.did.FodIdTestFactory.CANONICAL_MATCH_KEY;
import static fiftyone.pipeline.did.FodIdTestFactory.CANONICAL_LICENSE_ID;
import static fiftyone.pipeline.did.FodIdTestFactory.TEST_DOMAIN;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalPayload;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalPayloadWithSection;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalRandomPayload;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * The non-throwing read contract. Every result is checked for all three
 * facts at once, being whether the read succeeded, whether a value is
 * present, and the status, because a caller must be able to rely on any one
 * of them without consulting the others.
 */
public class FodIdParseTests {

    private static final Instant DATE = Instant.parse("2026-08-05T12:00:00Z");

    private FodIdTestFactory factory;

    @Before
    public void init() throws OwidException {
        factory = new FodIdTestFactory();
    }

    // ----- Success -----

    @Test
    public void tryFromBase64_ValidIdentifier_ParsedWithValue()
            throws Exception {
        FodIdParseResult result = FodId.tryFromBase64(
            factory.signedOwidBase64(canonicalPayload()));

        FodId fodId = assertParsed(result);
        assertEquals(CANONICAL_FLAGS, fodId.getFlags());
        assertEquals(CANONICAL_LICENSE_ID, fodId.getLicenseId());
        assertArrayEquals(CANONICAL_MATCH_KEY, fodId.getMatchKey());
        assertEquals(TEST_DOMAIN, fodId.getDomain());
    }

    @Test
    public void tryFromBase64_UrlSafeUnpaddedWithWhitespace_Parsed()
            throws Exception {
        String standard = factory.signedOwidBase64(canonicalPayload());
        String urlSafe = standard.replace('+', '-').replace('/', '_')
            .replace("=", "");

        FodId fromUrlSafe = assertParsed(FodId.tryFromBase64(urlSafe));
        FodId fromSpaced = assertParsed(
            FodId.tryFromBase64("  " + urlSafe + "\r\n"));

        assertArrayEquals(fromUrlSafe.asByteArray(), fromSpaced.asByteArray());
        assertArrayEquals(CANONICAL_MATCH_KEY, fromUrlSafe.getMatchKey());
    }

    @Test
    public void tryFromByteArray_ValidIdentifier_ParsedWithValue()
            throws Exception {
        byte[] bytes = factory.signedOwid(canonicalPayload()).asByteArray();

        FodId fodId = assertParsed(FodId.tryFromByteArray(bytes));

        assertArrayEquals(CANONICAL_MATCH_KEY, fodId.getMatchKey());
        assertArrayEquals(bytes, fodId.asByteArray());
    }

    @Test
    public void tryFromBase64_LongerSelfHostedDomain_Parsed()
            throws Exception {
        // The creator domain is a deployment parameter, and a self-hosted
        // container may sign with a much longer one than the cloud does.
        String longDomain = "identifier.creator.self-hosted-deployment."
            + "region-two.customer-platform.example.internal."
            + "a-rather-long-name.51degrees.com";
        String base64 = factory.signedOwidAt(
            canonicalPayload(), DATE, Version.VERSION3, longDomain).asBase64();

        FodId fodId = assertParsed(FodId.tryFromBase64(base64));

        assertEquals(longDomain, fodId.getDomain());
        assertTrue(fodId.verify(factory.publicPem));
    }

    @Test
    public void tryFromBase64_LongerContextSection_Parsed() throws Exception {
        // A payload longer than the match key carries a creator context section
        // whose shape belongs to the cloud, so an older reader accepts it
        // and exposes it as the payload beyond the match key.
        byte[] payload = canonicalPayloadWithSection(512);
        String base64 = factory.signedOwidAt(payload, DATE).asBase64();

        FodId fodId = assertParsed(FodId.tryFromBase64(base64));

        assertArrayEquals(CANONICAL_MATCH_KEY, fodId.getMatchKey());
        assertArrayEquals(payload, fodId.getPayload());
        assertTrue(fodId.verify(factory.publicPem));
    }

    @Test
    public void tryFromByteArray_MuchLongerPayload_NotRejectedForLength()
            throws Exception {
        // Nothing in this package puts an upper bound on a payload.
        for (int section : new int[] { 1, 100, 3000, 20000 }) {
            byte[] payload = canonicalPayloadWithSection(section);
            byte[] bytes = factory.signedOwidAt(payload, DATE).asByteArray();

            FodId fodId = assertParsed(FodId.tryFromByteArray(bytes));

            assertEquals(payload.length, fodId.getPayload().length);
            assertArrayEquals(CANONICAL_MATCH_KEY, fodId.getMatchKey());
        }
        byte[] random = Arrays.copyOf(canonicalRandomPayload(), 700);
        assertParsed(FodId.tryFromByteArray(
            factory.signedOwidAt(random, DATE).asByteArray()));
    }

    @Test
    public void tryFromBase64_EachTypeAtItsMinimum_Parsed() throws Exception {
        byte[] probabilistic = canonicalPayload();
        probabilistic[FodId.FLAGS_OFFSET] = 0b0000_0101;
        byte[] hashedEmail = canonicalPayload();
        hashedEmail[FodId.FLAGS_OFFSET] = (byte) 0b1000_0101;

        assertEquals(IdType.PROBABILISTIC, assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(probabilistic, DATE).asBase64())).getType());
        assertEquals(IdType.HASHED_EMAIL, assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(hashedEmail, DATE).asBase64())).getType());
        assertEquals(IdType.RANDOM, assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(canonicalRandomPayload(), DATE).asBase64()))
            .getType());
    }

    @Test
    public void tryFromBase64_ReservedHeaderOnly_ParsedBestEffort()
            throws Exception {
        byte[] payload = new byte[FodId.HEADER_LENGTH];
        payload[FodId.FLAGS_OFFSET] = (byte) 0b1100_0000;

        FodId fodId = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(payload, DATE).asBase64()));

        assertEquals(IdType.RESERVED, fodId.getType());
        assertEquals(0, fodId.getMatchKey().length);
    }

    // ----- 51Did payload rules -----

    @Test
    public void tryFromBase64_RandomOneByteShort_InvalidTypePayloadLength()
            throws Exception {
        byte[] payload = Arrays.copyOf(
            canonicalRandomPayload(), FodId.RANDOM_PAYLOAD_LENGTH - 1);

        assertFailed(
            FodId.tryFromBase64(factory.signedOwidAt(payload, DATE).asBase64()),
            FodIdParseStatus.INVALID_TYPE_PAYLOAD_LENGTH);
    }

    @Test
    public void tryFromBase64_ProbabilisticOneByteShort_InvalidTypePayloadLength()
            throws Exception {
        byte[] payload = Arrays.copyOf(
            canonicalPayload(), FodId.PAYLOAD_LENGTH - 1);
        payload[FodId.FLAGS_OFFSET] = 0;

        assertFailed(
            FodId.tryFromBase64(factory.signedOwidAt(payload, DATE).asBase64()),
            FodIdParseStatus.INVALID_TYPE_PAYLOAD_LENGTH);
    }

    @Test
    public void tryFromByteArray_HashedEmailOneByteShort_InvalidTypePayloadLength()
            throws Exception {
        // CANONICAL_FLAGS carries the HashedEmail tag.
        byte[] payload = Arrays.copyOf(
            canonicalPayload(), FodId.PAYLOAD_LENGTH - 1);

        assertFailed(
            FodId.tryFromByteArray(
                factory.signedOwidAt(payload, DATE).asByteArray()),
            FodIdParseStatus.INVALID_TYPE_PAYLOAD_LENGTH);
    }

    @Test
    public void tryFromBase64_ShorterThanHeader_PayloadTooShort()
            throws Exception {
        for (int length = 0; length < FodId.HEADER_LENGTH; length++) {
            byte[] payload = new byte[length];
            if (length > 0) {
                // Whatever the type bits say, the header is not all there.
                payload[FodId.FLAGS_OFFSET] = (byte) 0b1100_0000;
            }
            assertFailed(
                FodId.tryFromBase64(
                    factory.signedOwidAt(payload, DATE).asBase64()),
                FodIdParseStatus.PAYLOAD_TOO_SHORT);
            assertFailed(
                FodId.tryFromByteArray(
                    factory.signedOwidAt(payload, DATE).asByteArray()),
                FodIdParseStatus.PAYLOAD_TOO_SHORT);
        }
    }

    // ----- OWID statuses carried across unchanged -----

    @Test
    public void tryFromBase64_InvalidBase64_ReportsTheOwidStatus() {
        assertFailed(FodId.tryFromBase64("This is not valid Base64!@#$"),
            FodIdParseStatus.INVALID_BASE64);
        assertFailed(FodId.tryFromBase64("AwAA*"),
            FodIdParseStatus.INVALID_BASE64);
    }

    @Test
    public void tryFrom_NullOrEmpty_MissingInput() {
        assertFailed(FodId.tryFromBase64(null), FodIdParseStatus.MISSING_INPUT);
        assertFailed(FodId.tryFromBase64(""), FodIdParseStatus.MISSING_INPUT);
        assertFailed(FodId.tryFromBase64("   "),
            FodIdParseStatus.MISSING_INPUT);
        assertFailed(FodId.tryFromByteArray(null),
            FodIdParseStatus.MISSING_INPUT);
        assertFailed(FodId.tryFromByteArray(new byte[0]),
            FodIdParseStatus.MISSING_INPUT);
    }

    @Test
    public void tryFromByteArray_DeclarationMismatch_PropagatedUnchanged()
            throws Exception {
        // The declared payload count disagrees with the bytes present. The
        // OWID reader settles this before it sizes anything by the
        // declaration, and this package hands the reason on as it is. No
        // key is involved anywhere on this path, so no cryptography can
        // have been reached.
        byte[] payload = canonicalPayload();
        byte[] overDeclared = factory.envelopeBytes(
            payload, DATE, Version.VERSION3, TEST_DOMAIN, payload.length + 1);
        byte[] underDeclared = factory.envelopeBytes(
            payload, DATE, Version.VERSION3, TEST_DOMAIN, payload.length - 1);
        byte[] truncated = Arrays.copyOf(
            factory.signedOwidAt(payload, DATE).asByteArray(), 100);

        assertFailed(FodId.tryFromByteArray(overDeclared),
            FodIdParseStatus.BYTE_COUNT_MISMATCH);
        assertFailed(FodId.tryFromByteArray(underDeclared),
            FodIdParseStatus.BYTE_COUNT_MISMATCH);
        assertFailed(FodId.tryFromByteArray(truncated),
            FodIdParseStatus.BYTE_COUNT_MISMATCH);
        assertFailed(FodId.tryFromBase64(
                java.util.Base64.getEncoder().encodeToString(overDeclared)),
            FodIdParseStatus.BYTE_COUNT_MISMATCH);
    }

    @Test
    public void tryFromByteArray_OtherEnvelopeFaults_PropagatedUnchanged()
            throws Exception {
        byte[] good = factory.signedOwidAt(canonicalPayload(), DATE)
            .asByteArray();

        // Cut inside the domain, before its terminator.
        assertFailed(FodId.tryFromByteArray(Arrays.copyOf(good, 4)),
            FodIdParseStatus.UNEXPECTED_END);
        // A version byte nothing knows.
        byte[] unknownVersion = good.clone();
        unknownVersion[0] = (byte) 0x7F;
        assertFailed(FodId.tryFromByteArray(unknownVersion),
            FodIdParseStatus.UNSUPPORTED_VERSION);
        // The marker for an absent optional node is not a 51Did either.
        assertFailed(FodId.tryFromByteArray(new byte[] { 0 }),
            FodIdParseStatus.ABSENT_NODE);
    }

    @Test
    public void status_MirrorsEveryOwidStatusByName() {
        for (OwidParseStatus status : OwidParseStatus.values()) {
            assertEquals(status.name(),
                FodIdParseStatus.fromOwid(status).name());
        }
        assertEquals(FodIdParseStatus.PARSED,
            FodIdParseStatus.fromOwid(OwidParseStatus.PARSED));
    }

    // ----- Reading is not verifying -----

    @Test
    public void tamperedSignature_ParsesThenVerifiesAsSignatureInvalid()
            throws Exception {
        byte[] bytes = factory.signedOwidAt(canonicalPayload(), DATE)
            .asByteArray();
        bytes[bytes.length - 1] ^= (byte) 0xFF;

        FodId fodId = assertParsed(FodId.tryFromByteArray(bytes));
        OwidVerificationResult check = fodId.verifyDetailed(factory.publicPem);

        assertEquals(OwidSignatureStatus.SIGNATURE_INVALID, check.getStatus());
        assertFalse(check.isValid());
        assertFalse(fodId.verify(factory.publicPem));
        // The untouched envelope verifies with the same key.
        assertEquals(OwidSignatureStatus.SIGNATURE_VALID,
            factory.fodIdAt(canonicalPayload(), DATE)
                .verifyDetailed(factory.publicPem).getStatus());
    }

    @Test
    public void verifyDetailed_KeyUnavailable_IsNotSignatureInvalid()
            throws Exception {
        FodId fodId = factory.fodIdAt(canonicalPayload(), DATE);

        assertEquals(OwidSignatureStatus.KEY_UNAVAILABLE,
            fodId.verifyDetailed(null).getStatus());
        assertEquals(OwidSignatureStatus.KEY_UNAVAILABLE,
            fodId.verifyDetailed("  ").getStatus());
        assertEquals(OwidSignatureStatus.INVALID_KEY,
            fodId.verifyDetailed("not a public key").getStatus());
        for (OwidVerificationResult result : new OwidVerificationResult[] {
                fodId.verifyDetailed(null),
                fodId.verifyDetailed("not a public key") }) {
            assertNotEquals(OwidSignatureStatus.SIGNATURE_INVALID,
                result.getStatus());
            assertFalse(result.isValid());
        }
        // The boolean form keeps its documented behaviour for a bad key.
        assertThrows(OwidException.class,
            () -> fodId.verify("not a public key"));
    }

    // ----- The throwing readers make the same read -----

    @Test
    public void throwingReaders_ThrowTheDocumentedTypesForTheSameInputs()
            throws Exception {
        String tooShort = factory.signedOwidAt(new byte[2], DATE).asBase64();
        String randomShort = factory.signedOwidAt(Arrays.copyOf(
            canonicalRandomPayload(), FodId.RANDOM_PAYLOAD_LENGTH - 1), DATE)
            .asBase64();
        byte[] truncated = Arrays.copyOf(
            factory.signedOwidAt(canonicalPayload(), DATE).asByteArray(), 100);

        // Payload rule failures are argument failures.
        assertThrows(IllegalArgumentException.class,
            () -> FodId.fromBase64(tooShort));
        assertThrows(IllegalArgumentException.class,
            () -> FodId.fromBase64(randomShort));
        // Envelope failures are OWID failures, naming the status.
        OwidException notBase64 = assertThrows(OwidException.class,
            () -> FodId.fromBase64("This is not valid Base64!@#$"));
        assertTrue(notBase64.getMessage().contains("INVALID_BASE64"));
        OwidException mismatch = assertThrows(OwidException.class,
            () -> FodId.fromByteArray(truncated));
        assertTrue(mismatch.getMessage().contains("BYTE_COUNT_MISMATCH"));
        assertThrows(OwidException.class, () -> FodId.fromBase64(""));
        // Null keeps its own contract on the throwing surface.
        assertThrows(NullPointerException.class, () -> FodId.fromBase64(null));
        assertThrows(NullPointerException.class,
            () -> FodId.fromByteArray(null));
    }

    // ----- Helpers -----

    private static FodId assertParsed(FodIdParseResult result) {
        assertTrue(result.toString(), result.isSuccess());
        assertNotNull(result.getValue());
        assertEquals(FodIdParseStatus.PARSED, result.getStatus());
        return result.getValue();
    }

    private static void assertFailed(
            FodIdParseResult result, FodIdParseStatus expected) {
        assertFalse(result.toString(), result.isSuccess());
        assertNull(result.getValue());
        assertEquals(expected, result.getStatus());
    }
}
