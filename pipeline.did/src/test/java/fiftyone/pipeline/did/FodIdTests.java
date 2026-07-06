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

import com.swancommunity.owid.Crypto;
import com.swancommunity.owid.Owid;
import com.swancommunity.owid.OwidException;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import static fiftyone.pipeline.did.FodIdTestFactory.CANONICAL_FLAGS;
import static fiftyone.pipeline.did.FodIdTestFactory.CANONICAL_HASH;
import static fiftyone.pipeline.did.FodIdTestFactory.CANONICAL_LICENSE_ID;
import static fiftyone.pipeline.did.FodIdTestFactory.TEST_DOMAIN;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalPayload;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalRandomPayload;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link FodId}. Ports the live .NET suite (including the Type-model
 * cases) and adds the runbook's gap tests.
 */
public class FodIdTests {

    private FodIdTestFactory factory;

    @Before
    public void init() throws OwidException {
        factory = new FodIdTestFactory();
    }

    // ----- Current .NET coverage -----

    @Test
    public void constants_AreInternallyConsistent() {
        assertEquals(FodId.PAYLOAD_LENGTH, FodId.HASH_OFFSET + FodId.HASH_LENGTH);
        assertEquals(FodId.HASH_OFFSET, FodId.LICENSE_ID_OFFSET + FodId.LICENSE_ID_LENGTH);
        assertEquals(FodId.RANDOM_PAYLOAD_LENGTH, FodId.HASH_OFFSET + FodId.GUID_LENGTH);
    }

    @Test
    public void exposesOwidLevelFields() throws Exception {
        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(canonicalPayload()));
        // OWID-level concerns are delegated to the wrapped envelope.
        assertEquals(TEST_DOMAIN, fodId.getDomain());
        assertNotNull(fodId.getVersion());
    }

    @Test
    public void constructor_FromBase64_UnpacksAllThreeFields() throws Exception {
        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(canonicalPayload()));

        assertEquals(CANONICAL_FLAGS, fodId.getFlags());
        assertEquals(CANONICAL_LICENSE_ID, fodId.getLicenseId());
        assertArrayEquals(CANONICAL_HASH, fodId.getHash());
        assertEquals(TEST_DOMAIN, fodId.getDomain());
    }

    @Test
    public void constructor_FromBytes_UnpacksAllThreeFields() throws Exception {
        String base64 = factory.signedOwidBase64(canonicalPayload());
        byte[] bytes = Base64.getDecoder().decode(base64);

        FodId fodId = FodId.fromByteArray(bytes);

        assertEquals(CANONICAL_FLAGS, fodId.getFlags());
        assertEquals(CANONICAL_LICENSE_ID, fodId.getLicenseId());
        assertArrayEquals(CANONICAL_HASH, fodId.getHash());
        assertEquals(TEST_DOMAIN, fodId.getDomain());
    }

    @Test
    public void constructor_FromOwid_UnpacksAllThreeFields() throws Exception {
        Owid owid = factory.signedOwid(canonicalPayload());

        FodId fodId = FodId.fromOwid(owid);

        assertEquals(CANONICAL_FLAGS, fodId.getFlags());
        assertEquals(CANONICAL_LICENSE_ID, fodId.getLicenseId());
        assertArrayEquals(CANONICAL_HASH, fodId.getHash());
        assertEquals(owid.getDomain(), fodId.getDomain());
        assertEquals(owid.getDate(), fodId.getDate());
        assertEquals(owid.getVersion(), fodId.getVersion());
        // owid-java getters clone, so assert content equality, not identity.
        assertArrayEquals(owid.getPayload(), fodId.getPayload());
        assertArrayEquals(owid.getSignature(), fodId.getSignature());
    }

    @Test
    public void constructor_NullOwid_Throws() {
        assertThrows(NullPointerException.class, () -> FodId.fromOwid(null));
    }

    @Test
    public void licenseId_IsLittleEndian() throws Exception {
        byte[] payload = canonicalPayload();
        payload[FodId.LICENSE_ID_OFFSET] = 0x01;
        payload[FodId.LICENSE_ID_OFFSET + 1] = 0x00;
        payload[FodId.LICENSE_ID_OFFSET + 2] = 0x00;
        payload[FodId.LICENSE_ID_OFFSET + 3] = 0x00;

        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(payload));

        assertEquals(1L, fodId.getLicenseId());
    }

    @Test
    public void licenseId_MaxValue_IsLittleEndian() throws Exception {
        byte[] payload = canonicalPayload();
        payload[FodId.LICENSE_ID_OFFSET] = (byte) 0xFF;
        payload[FodId.LICENSE_ID_OFFSET + 1] = (byte) 0xFF;
        payload[FodId.LICENSE_ID_OFFSET + 2] = (byte) 0xFF;
        payload[FodId.LICENSE_ID_OFFSET + 3] = (byte) 0xFF;

        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(payload));

        assertEquals(4294967295L, fodId.getLicenseId());
    }

    @Test
    public void licenseId_HighBitSet_StaysUnsigned() throws Exception {
        byte[] payload = canonicalPayload();
        // 0x80000000 little-endian: 00 00 00 80
        payload[FodId.LICENSE_ID_OFFSET] = 0x00;
        payload[FodId.LICENSE_ID_OFFSET + 1] = 0x00;
        payload[FodId.LICENSE_ID_OFFSET + 2] = 0x00;
        payload[FodId.LICENSE_ID_OFFSET + 3] = (byte) 0x80;

        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(payload));

        assertEquals(0x80000000L, fodId.getLicenseId());
    }

    @Test
    public void flags_ZeroValue_Exposed() throws Exception {
        byte[] payload = canonicalPayload();
        payload[FodId.FLAGS_OFFSET] = 0x00;

        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(payload));

        assertEquals(0, fodId.getFlags());
    }

    @Test
    public void flags_AllBitsSet_Exposed() throws Exception {
        byte[] payload = canonicalPayload();
        payload[FodId.FLAGS_OFFSET] = (byte) 0xFF;

        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(payload));

        assertEquals(255, fodId.getFlags());
    }

    @Test
    public void hash_IsDefensiveCopy() throws Exception {
        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(canonicalPayload()));

        byte[] hash = fodId.getHash();
        hash[0] = 0x00;
        hash[FodId.HASH_LENGTH - 1] = 0x00;

        // Neither the underlying payload nor a fresh getHash() is affected.
        assertEquals(CANONICAL_HASH[0], fodId.getPayload()[FodId.HASH_OFFSET]);
        assertArrayEquals(CANONICAL_HASH, fodId.getHash());
    }

    @Test
    public void constructor_PayloadOneByteShort_Throws() throws Exception {
        // 36 bytes - one short of the minimum 37 (flags 0 -> Probabilistic).
        String base64 = factory.signedOwidBase64(new byte[FodId.PAYLOAD_LENGTH - 1]);
        assertThrows(IllegalArgumentException.class, () -> FodId.fromBase64(base64));
    }

    @Test
    public void constructor_PayloadEmpty_Throws() throws Exception {
        String base64 = factory.signedOwidBase64(new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> FodId.fromBase64(base64));
    }

    @Test
    public void constructor_NullBase64_Throws() {
        assertThrows(NullPointerException.class, () -> FodId.fromBase64(null));
    }

    @Test
    public void constructor_NullBuffer_Throws() {
        assertThrows(NullPointerException.class, () -> FodId.fromByteArray(null));
    }

    @Test
    public void constructor_InvalidBase64_Throws() {
        assertThrows(OwidException.class,
            () -> FodId.fromBase64("This is not valid Base64!@#$"));
    }

    @Test
    public void constructor_PayloadLargerThanSpec_UsesFirst37Bytes() throws Exception {
        byte[] payload = new byte[64];
        System.arraycopy(canonicalPayload(), 0, payload, 0, FodId.PAYLOAD_LENGTH);
        for (int i = FodId.PAYLOAD_LENGTH; i < payload.length; i++) {
            payload[i] = (byte) 0xCC;
        }

        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(payload));

        assertEquals(CANONICAL_FLAGS, fodId.getFlags());
        assertEquals(CANONICAL_LICENSE_ID, fodId.getLicenseId());
        assertArrayEquals(CANONICAL_HASH, fodId.getHash());
        assertEquals(FodId.HASH_LENGTH, fodId.getHash().length);
    }

    @Test
    public void fodId_IsCryptographicallyVerifiable() throws Exception {
        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(canonicalPayload()));
        assertTrue(fodId.verify(factory.publicPem));
    }

    @Test
    public void base64Roundtrip_PreservesAllFields() throws Exception {
        FodId fodId1 = FodId.fromBase64(factory.signedOwidBase64(canonicalPayload()));
        FodId fodId2 = FodId.fromBase64(fodId1.asBase64());

        assertEquals(fodId1.getFlags(), fodId2.getFlags());
        assertEquals(fodId1.getLicenseId(), fodId2.getLicenseId());
        assertArrayEquals(fodId1.getHash(), fodId2.getHash());
        assertEquals(fodId1.getDomain(), fodId2.getDomain());
    }

    // ----- Type model -----

    @Test
    public void type_DecodedFromTopTwoFlagBits() throws Exception {
        assertEquals(IdType.PROBABILISTIC, typeForFlags((byte) 0b0000_0101));
        assertEquals(IdType.HASHED_EMAIL, typeForFlags((byte) 0b1000_0101));
        assertEquals(IdType.RESERVED, typeForFlags((byte) 0b1100_0101));
    }

    private IdType typeForFlags(byte flags) throws Exception {
        byte[] payload = canonicalPayload();
        payload[FodId.FLAGS_OFFSET] = flags;
        return FodId.fromBase64(factory.signedOwidBase64(payload)).getType();
    }

    @Test
    public void type_RandomWhenBits01() throws Exception {
        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(canonicalRandomPayload()));
        assertEquals(IdType.RANDOM, fodId.getType());
    }

    @Test
    public void constructor_RandomPayload21Bytes_Parses() throws Exception {
        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(canonicalRandomPayload()));

        assertEquals(CANONICAL_LICENSE_ID, fodId.getLicenseId());
        assertEquals(FodId.GUID_LENGTH, fodId.getHash().length);
        byte[] expected = new byte[FodId.GUID_LENGTH];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) (0x40 + i);
        }
        assertArrayEquals(expected, fodId.getHash());
    }

    @Test
    public void constructor_RandomPayloadOneByteShort_Throws() throws Exception {
        byte[] payload = Arrays.copyOf(
            canonicalRandomPayload(), FodId.RANDOM_PAYLOAD_LENGTH - 1);
        String base64 = factory.signedOwidBase64(payload);
        assertThrows(IllegalArgumentException.class, () -> FodId.fromBase64(base64));
    }

    @Test
    public void constructor_RandomPayloadLargerThanSpec_UsesFirst16ValueBytes()
            throws Exception {
        byte[] payload = new byte[FodId.PAYLOAD_LENGTH];
        System.arraycopy(
            canonicalRandomPayload(), 0, payload, 0, FodId.RANDOM_PAYLOAD_LENGTH);
        for (int i = FodId.RANDOM_PAYLOAD_LENGTH; i < payload.length; i++) {
            payload[i] = (byte) 0xCC;
        }

        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(payload));

        assertEquals(IdType.RANDOM, fodId.getType());
        assertEquals(FodId.GUID_LENGTH, fodId.getHash().length);
    }

    @Test
    public void constructor_HashedEmailPayloadOneByteShort_Throws() throws Exception {
        // CANONICAL_FLAGS (0xA5) carries the HashedEmail tag, so the 37-byte
        // minimum still applies.
        byte[] payload = Arrays.copyOf(canonicalPayload(), FodId.PAYLOAD_LENGTH - 1);
        String base64 = factory.signedOwidBase64(payload);
        assertThrows(IllegalArgumentException.class, () -> FodId.fromBase64(base64));
    }

    @Test
    public void constructor_ReservedHeaderOnly_Parses() throws Exception {
        byte[] payload = new byte[FodId.HASH_OFFSET];
        payload[FodId.FLAGS_OFFSET] = (byte) 0b1100_0000;

        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(payload));

        assertEquals(IdType.RESERVED, fodId.getType());
        assertEquals(0, fodId.getHash().length);
    }

    // ----- Gap tests (runbook section 6b) -----

    @Test
    public void compareTwo51Dids_SamePayload_SameValueDifferentEnvelopes()
            throws Exception {
        byte[] payload = canonicalPayload();
        Owid a = factory.signedOwid(payload);
        Owid b = factory.signedOwid(payload);
        // sign() stamps "now" to the minute, so set distinct dates to
        // represent two reissues at different times.
        a.setDate(Instant.parse("2026-01-01T00:00:00Z"));
        b.setDate(Instant.parse("2026-01-01T00:05:00Z"));

        FodId fodA = FodId.fromBase64(a.asBase64());
        FodId fodB = FodId.fromBase64(b.asBase64());

        // The value is stable across reissues...
        assertArrayEquals(fodA.getHash(), fodB.getHash());
        // ...while the envelope differs.
        assertNotEquals(fodA.getDate(), fodB.getDate());
        assertFalse(Arrays.equals(fodA.getSignature(), fodB.getSignature()));
        assertNotEquals(a.asBase64(), b.asBase64());
    }

    @Test
    public void construction_DoesNotVerify() throws Exception {
        // An OWID with a present but tampered (invalid) signature still
        // constructs and exposes all three fields - construction must not
        // verify.
        byte[] bytes = Base64.getDecoder().decode(
            factory.signedOwidBase64(canonicalPayload()));
        bytes[bytes.length - 1] ^= 0xFF;   // corrupt the signature
        Owid tampered = Owid.fromByteArray(bytes);

        FodId fodId = FodId.fromOwid(tampered);

        assertEquals(CANONICAL_FLAGS, fodId.getFlags());
        assertEquals(CANONICAL_LICENSE_ID, fodId.getLicenseId());
        assertArrayEquals(CANONICAL_HASH, fodId.getHash());
    }

    @Test
    public void fromOwid_IsDecoupledFromSourceOwid() throws Exception {
        // Mutating the source OWID after construction must not affect the
        // FodId (it holds an independent copy).
        Owid owid = factory.signedOwid(canonicalPayload());
        FodId fodId = FodId.fromOwid(owid);

        owid.setPayload(new byte[FodId.PAYLOAD_LENGTH]);

        assertEquals(CANONICAL_FLAGS, fodId.getFlags());
        assertArrayEquals(CANONICAL_HASH, fodId.getHash());
        assertEquals(CANONICAL_HASH[0], fodId.getPayload()[FodId.HASH_OFFSET]);
    }

    @Test
    public void verify_WithWrongKey_ReturnsFalse() throws Exception {
        FodId fodId = FodId.fromBase64(factory.signedOwidBase64(canonicalPayload()));
        String otherPublicPem = Crypto.generate().publicKeyPem();

        assertFalse(fodId.verify(otherPublicPem));
    }

    @Test
    public void roundtrip_ThroughBytesConstructor_PreservesAllFields()
            throws Exception {
        FodId fodId1 = FodId.fromBase64(factory.signedOwidBase64(canonicalPayload()));

        FodId fodId2 = FodId.fromByteArray(fodId1.asByteArray());

        assertEquals(fodId1.getFlags(), fodId2.getFlags());
        assertEquals(fodId1.getLicenseId(), fodId2.getLicenseId());
        assertArrayEquals(fodId1.getHash(), fodId2.getHash());
        assertEquals(fodId1.getDomain(), fodId2.getDomain());
    }
}
