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
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalPayloadWithTerms;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalPayloadWithTermsAndSection;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalRandomPayload;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalRandomPayloadWithTerms;
import static fiftyone.pipeline.did.FodIdTestFactory.payloadEndingAtMatchKey;
import static fiftyone.pipeline.did.FodIdTestFactory.randomPayloadEndingAtMatchKey;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
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

    /**
     * The usage is the highest granted, because the bits are cumulative.
     * A mask for the non-marketing bit alone would say yes for every
     * marketing identifier, which is the wrong answer for a data
     * protection decision.
     */
    @Test
    public void getUsage_IsTheHighestGranted() throws Exception {
        int[] bits = {0b000, 0b001, 0b011, 0b111};
        Usage[] expected = {
            Usage.NONE, Usage.NON_MARKETING, Usage.STANDARD, Usage.PERSONALIZED};
        String[] idUsage = {null, "non-marketing", "standard", "personalized"};
        for (int i = 0; i < bits.length; i++) {
            byte[] payload = canonicalRandomPayload();
            payload[FodId.FLAGS_OFFSET] = (byte) ((1 << 6) | bits[i]);
            FodId fodId = assertParsed(FodId.tryFromBase64(
                factory.signedOwidAt(payload, DATE).asBase64()));
            assertEquals("usage bits " + bits[i], expected[i], fodId.getUsage());
            assertEquals(idUsage[i], fodId.getUsage().getIdUsage());
            assertEquals(IdType.RANDOM, fodId.getType());
            assertFalse(fodId.isUsageFromConsent());
        }
    }

    /**
     * Bit 3 records that the usage came from a consent string rather than
     * being stated, and reads independently of which usage it is.
     */
    @Test
    public void isUsageFromConsent_IsBitThree() throws Exception {
        byte[] payload = canonicalRandomPayload();
        payload[FodId.FLAGS_OFFSET] = (byte) ((1 << 6) | 0b1011);
        FodId fodId = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(payload, DATE).asBase64()));
        assertTrue(fodId.isUsageFromConsent());
        assertEquals(Usage.STANDARD, fodId.getUsage());
    }

    /**
     * An identifier whose payload ends at the match key has no Terms byte.
     * A missing byte is read as index zero, which says the terms are not
     * stated in the identifier, so absence and zero mean the same thing and
     * nothing has to tell them apart, and the identifier answers with no
     * address.
     */
    @Test
    public void getTerms_NoByteAfterTheMatchKey_HasNoAddress()
            throws Exception {
        for (byte[] payload : new byte[][] {
                payloadEndingAtMatchKey(),
                randomPayloadEndingAtMatchKey() }) {
            FodId fodId = assertParsed(FodId.tryFromBase64(
                factory.signedOwidAt(payload, DATE).asBase64()));

            assertNull(fodId.getTerms());
        }
    }

    /**
     * A Terms byte holding zero answers exactly as no byte at all does, so
     * the two never have to be told apart.
     */
    @Test
    public void getTerms_ZeroByte_AnswersAsAbsenceDoes() throws Exception {
        FodId absent = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(payloadEndingAtMatchKey(), DATE)
                .asBase64()));
        FodId zero = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(canonicalPayloadWithTerms(0), DATE)
                .asBase64()));

        assertEquals(absent.getTerms(), zero.getTerms());
        assertNull(zero.getTerms());
    }

    /**
     * Index one is the Model Terms for Marketing version 2, whose address is
     * answered exactly as the specification writes it and is never fetched.
     * The byte is read after the match key, whose length the type sets, so
     * both match key lengths are checked and neither loses a byte to the
     * Terms.
     */
    @Test
    public void getTerms_IndexOne_ModelTermsAddressForBothKeyLengths()
            throws Exception {
        FodId probabilistic = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(canonicalPayloadWithTerms(1), DATE)
                .asBase64()));
        FodId random = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(canonicalRandomPayloadWithTerms(1), DATE)
                .asBase64()));

        for (FodId fodId : new FodId[] { probabilistic, random }) {
            assertEquals("https://m4ow.uk/mtm/2.txt", fodId.getTerms());
        }
        // The 32-byte match key and the 16-byte one are both the same as
        // they are without the Terms byte, so nothing was taken from either.
        assertEquals(IdType.HASHED_EMAIL, probabilistic.getType());
        assertArrayEquals(CANONICAL_MATCH_KEY, probabilistic.getMatchKey());
        assertEquals(IdType.RANDOM, random.getType());
        assertEquals(FodId.GUID_LENGTH, random.getMatchKey().length);
        assertArrayEquals(
            assertParsed(FodId.tryFromBase64(factory
                .signedOwidAt(randomPayloadEndingAtMatchKey(), DATE)
                .asBase64()))
                .getMatchKey(),
            random.getMatchKey());
    }

    /**
     * An index added to the specification after this package was released
     * answers with no address, and no address is ever built from the index,
     * because that would name a document nobody wrote. A caller cannot tell
     * such an index from zero, which is deliberate, since both say the
     * identifier does not give the terms and the answer has to come from
     * somewhere else. The byte is unsigned in the read, so 255 is 255 and
     * not a negative number.
     */
    @Test
    public void getTerms_IndexThisPackageDoesNotKnow_HasNoAddress()
            throws Exception {
        FodId notStated = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(canonicalPayloadWithTerms(0), DATE)
                .asBase64()));

        for (int index : new int[] { 2, 127, 200, 255 }) {
            FodId unknown = assertParsed(FodId.tryFromBase64(
                factory.signedOwidAt(canonicalPayloadWithTerms(index), DATE)
                    .asBase64()));

            assertNull("terms index " + index, unknown.getTerms());
            assertEquals("terms index " + index,
                Terms.UNKNOWN, Terms.fromIndex(index));
        }
        assertNull(notStated.getTerms());
    }

    /**
     * Every index the table does not carry is unknown and has no address,
     * across the whole byte, so none of them can be read as zero.
     */
    @Test
    public void terms_EveryIndexOutsideTheTable_IsUnknownWithNoUrl() {
        assertEquals(Terms.NOT_STATED, Terms.fromIndex(0));
        assertNull(Terms.NOT_STATED.getUrl());
        assertEquals(Terms.MODEL_TERMS_FOR_MARKETING_2, Terms.fromIndex(1));
        assertEquals("https://m4ow.uk/mtm/2.txt",
            Terms.MODEL_TERMS_FOR_MARKETING_2.getUrl());
        for (int index = 2; index <= 255; index++) {
            assertEquals("terms index " + index,
                Terms.UNKNOWN, Terms.fromIndex(index));
            assertNull("terms index " + index,
                Terms.fromIndex(index).getUrl());
        }
    }

    // ----- The payload version -----

    /**
     * The payload with its version bits set to the given version, leaving
     * every other bit of the Flags byte alone.
     */
    private static byte[] withVersion(byte[] payload, int version) {
        byte[] withVersion = payload.clone();
        withVersion[FodId.FLAGS_OFFSET] = (byte) (
            (payload[FodId.FLAGS_OFFSET] & 0b1100_1111)
            | (version << 4));
        return withVersion;
    }

    /**
     * A Flags byte with bits 4 and 5 clear is version 0, which is the
     * layout this package reads, so every field reads as it does on the
     * canonical payload.
     */
    @Test
    public void version_Zero_ReadsEveryField() throws Exception {
        FodId fodId = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(canonicalPayload(), DATE).asBase64()));

        assertEquals(IdType.HASHED_EMAIL, fodId.getType());
        assertEquals(Usage.PERSONALIZED, fodId.getUsage());
        assertArrayEquals(CANONICAL_MATCH_KEY, fodId.getMatchKey());
        assertEquals("https://m4ow.uk/mtm/2.txt", fodId.getTerms());
    }

    /**
     * Versions 1, 2 and 3 are not assigned, so a payload naming one is
     * refused rather than read under the layout this package knows.
     */
    @Test
    public void version_NotZero_IsRefused() throws Exception {
        for (int version : new int[] { 1, 2, 3 }) {
            FodIdParseResult result = FodId.tryFromBase64(factory
                .signedOwidAt(withVersion(canonicalPayload(), version), DATE)
                .asBase64());

            assertFalse("version " + version, result.isSuccess());
            assertEquals("version " + version,
                FodIdParseStatus.UNSUPPORTED_PAYLOAD_VERSION,
                result.getStatus());
            // Nothing is handed back, rather than a value with some fields
            // filled in, because there is no identifier to expose fields
            // for when the layout was not understood.
            assertNull("version " + version, result.getValue());
        }
    }

    /**
     * The throwing readers name the version they found, so whoever reads
     * the message knows which layout the identifier claims rather than only
     * that some version was refused.
     */
    @Test
    public void version_NotZero_MessageNamesTheVersion() throws Exception {
        for (int version : new int[] { 1, 2, 3 }) {
            String base64 = factory
                .signedOwidAt(withVersion(canonicalPayload(), version), DATE)
                .asBase64();
            try {
                FodId.fromBase64(base64);
                fail("version " + version + " should have been refused");
            } catch (IllegalArgumentException thrown) {
                assertTrue(thrown.getMessage(),
                    thrown.getMessage().contains("version " + version));
            }
        }
    }

    /**
     * The version bits are read on their own, so an identifier of version 0
     * still reads whatever its usage and type bits hold, and one of another
     * version is refused whatever they hold. A reader masking the wrong
     * bits would fail one of these.
     */
    @Test
    public void version_IsReadApartFromTheUsageAndTypeBits()
            throws Exception {
        for (int usage : new int[] { 0b000, 0b001, 0b011, 0b111 }) {
            for (int type : new int[] { 0b00, 0b10, 0b11 }) {
                int flags = (type << 6) | usage;
                byte[] payload = payloadEndingAtMatchKey();
                payload[FodId.FLAGS_OFFSET] = (byte) flags;

                assertTrue("flags " + flags, FodId.tryFromBase64(
                    factory.signedOwidAt(payload, DATE).asBase64())
                    .isSuccess());

                for (int version : new int[] { 1, 2, 3 }) {
                    FodIdParseResult refused = FodId.tryFromBase64(factory
                        .signedOwidAt(withVersion(payload, version), DATE)
                        .asBase64());

                    assertEquals("flags " + flags + " version " + version,
                        FodIdParseStatus.UNSUPPORTED_PAYLOAD_VERSION,
                        refused.getStatus());
                    assertNull(refused.getValue());
                }
            }
        }
    }

    /**
     * The Terms sits before the creator context section, so a payload
     * carrying both still reads the match key and the Terms from the places
     * they are written at, and the section is exposed as it always was.
     */
    @Test
    public void getTerms_ByteThenContextSection_ReadAtTheRightOffset()
            throws Exception {
        byte[] payload = canonicalPayloadWithTermsAndSection(1, 512);

        FodId fodId = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(payload, DATE).asBase64()));

        assertArrayEquals(CANONICAL_MATCH_KEY, fodId.getMatchKey());
        assertEquals("https://m4ow.uk/mtm/2.txt", fodId.getTerms());
        assertArrayEquals(payload, fodId.getPayload());
        assertTrue(fodId.verify(factory.publicPem));
    }

    /**
     * The terms table is the one place the package says which index is
     * which document, and the index to member map is built from the
     * members rather than written out again. This walks every member and
     * fails if one does not read back from its own index, or if a member
     * that names a document has no address, which is what would happen if
     * a member and its address were ever added apart.
     */
    @Test
    public void terms_EveryMemberAgreesWithTheTable() {
        for (Terms terms : Terms.values()) {
            if (terms == Terms.UNKNOWN) {
                // It stands for every index not in the table, so it has no
                // index of its own and no address.
                assertEquals(-1, terms.getIndex());
                assertNull(terms.getUrl());
                continue;
            }
            assertEquals(
                "member " + terms + " does not read back from its index",
                terms,
                Terms.fromIndex(terms.getIndex()));
            if (terms == Terms.NOT_STATED) {
                // Names no document, so it has no address.
                assertEquals(0, terms.getIndex());
                assertNull(terms.getUrl());
            } else {
                assertNotNull(
                    "member " + terms + " names a document with no address",
                    terms.getUrl());
                assertTrue(
                    "address for " + terms + " is not an https address",
                    terms.getUrl().startsWith("https://"));
            }
        }
    }

    /**
     * The Reserved type has no defined match key length, so the reader
     * takes every byte after the header as the match key and leaves none
     * to read as the Terms. Such an identifier therefore states no terms,
     * which is the right answer rather than a gap to close, because an
     * identifier of a type this package cannot lay out is one whose Terms
     * it cannot place either.
     */
    @Test
    public void getTerms_ReservedType_StatesNoTerms() throws Exception {
        byte[] payload = canonicalPayloadWithTerms(1);
        payload[FodId.FLAGS_OFFSET] =
            (byte) ((CANONICAL_FLAGS & 0b0011_1111) | 0b1100_0000);

        FodId fodId = assertParsed(FodId.tryFromBase64(
            factory.signedOwidAt(payload, DATE).asBase64()));

        assertEquals(IdType.RESERVED, fodId.getType());
        assertNull(fodId.getTerms());
        // The byte that would have been the Terms is inside the match key,
        // which is what taking every byte after the header means.
        assertEquals(
            payload.length - FodId.HEADER_LENGTH,
            fodId.getMatchKey().length);
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
