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
import com.swancommunity.owid.Version;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static fiftyone.pipeline.did.FodIdTestFactory.canonicalPayload;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalRandomPayload;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DidClient} with the network stood in for by a recording
 * transport. Three signing keys, one per week, stand in for the cloud's
 * published schedule.
 */
public class DidClientTests {

    private static final String ENDPOINT = "https://example.test/api/v4/";
    private static final Instant WEEK1 = Instant.parse("2026-08-03T00:00:00Z");
    private static final Instant WEEK2 = WEEK1.plus(Duration.ofDays(7));
    private static final Instant WEEK3 = WEEK2.plus(Duration.ofDays(7));
    private static final int MAXIMUM_PAYLOAD_LENGTH = 56;
    private static final int MAXIMUM_BYTE_LENGTH = 136;
    private static final int MAXIMUM_BASE64_LENGTH = 184;
    private static final int MAXIMUM_BASE64_URL_LENGTH = 182;
    private static final String MAXIMUM_TEST_DOMAIN = "51d.es";

    private FodIdTestFactory key1;
    private FodIdTestFactory key2;
    private FodIdTestFactory key3;
    private FakeTransport transport;
    private MutableClock clock;
    private DidClient client;

    @Before
    public void init() throws OwidException {
        key1 = new FodIdTestFactory();
        key2 = new FodIdTestFactory();
        key3 = new FodIdTestFactory();
        transport = new FakeTransport();
        clock = new MutableClock(WEEK2.plus(Duration.ofDays(1)));
        client = DidClient.builder("resource")
            .licenceKey("licence")
            .endpoint(ENDPOINT)
            .transport(transport)
            .clock(clock)
            .build();
    }

    // ----- Construction -----

    @Test
    public void builder_RejectsBlankResourceKey() {
        assertThrows(IllegalArgumentException.class,
            () -> DidClient.builder(" "));
        assertThrows(IllegalArgumentException.class,
            () -> new DidClient(null));
    }

    @Test
    public void endpoint_IsNormalisedToOneTrailingSlash() {
        assertEquals("https://host/api/v4/",
            DidClient.resolveEndpoint("https://host/api/v4"));
        assertEquals("https://host/api/v4/",
            DidClient.resolveEndpoint("https://host/api/v4///"));
        assertEquals("https://host/api/v4/",
            DidClient.resolveEndpoint(" https://host/api/v4/ "));
        assertEquals(ENDPOINT,
            new DidClient("resource", null, ENDPOINT).getEndpoint());
    }

    @Test
    public void endpoint_DefaultsToTheCloudOrTheEnvironment() {
        String fromEnvironment = System.getenv(DidClient.ENDPOINT_VARIABLE);
        String expected = fromEnvironment == null
            || fromEnvironment.trim().isEmpty()
            ? DidClient.DEFAULT_ENDPOINT
            : DidClient.resolveEndpoint(fromEnvironment);
        assertEquals(expected, DidClient.resolveEndpoint(null));
        assertEquals(expected, DidClient.resolveEndpoint(""));
    }

    @Test
    public void licenceKey_BlankCountsAsNone() {
        assertFalse(new DidClient("resource", " ", ENDPOINT).hasLicenceKey());
        assertTrue(client.hasLicenceKey());
    }

    // ----- Public keys -----

    @Test
    public void publicKeys_ReadsStartsAtAndIgnoresWeekStart() throws Exception {
        transport.queue(200, keyList("startsAt", true));

        List<SigningKey> keys = client.publicKeys();

        assertEquals(3, keys.size());
        assertEquals(WEEK1, keys.get(0).getStartsAt());
        assertEquals(WEEK2, keys.get(1).getStartsAt());
        assertEquals(WEEK3, keys.get(2).getStartsAt());
        assertEquals(key1.publicPem, keys.get(0).getPublicKeyPem());
        assertEquals(key3.publicPem, keys.get(2).getPublicKeyPem());

        HttpTransport.Request request = transport.last();
        assertEquals("GET", request.getMethod());
        assertEquals(ENDPOINT + "id/key/resource", request.getUrl());
        assertTrue(request.getHeaders().get("User-Agent")
            .startsWith("pipeline.did/"));
    }

    @Test
    public void publicKeys_FallsBackToCreated() throws Exception {
        transport.queue(200, keyList("created", false));

        List<SigningKey> keys = client.publicKeys();

        assertEquals(WEEK1, keys.get(0).getStartsAt());
        assertEquals(WEEK3, keys.get(2).getStartsAt());
    }

    @Test
    public void publicKeys_SortsByStart() throws Exception {
        JSONArray reversed = new JSONArray();
        reversed.put(keyEntry("startsAt", WEEK3, key3));
        reversed.put(keyEntry("startsAt", WEEK1, key1));
        reversed.put(keyEntry("startsAt", WEEK2, key2));
        transport.queue(200, reversed.toString());

        List<SigningKey> keys = client.publicKeys();

        assertEquals(WEEK1, keys.get(0).getStartsAt());
        assertEquals(WEEK2, keys.get(1).getStartsAt());
        assertEquals(WEEK3, keys.get(2).getStartsAt());
    }

    @Test
    public void publicKeys_SecondCallUsesTheCache() throws Exception {
        transport.queue(200, keyList("startsAt", false));

        List<SigningKey> first = client.publicKeys();
        List<SigningKey> second = client.publicKeys();

        assertSame(first, second);
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void publicKeys_FirstFetchFailureRaises() {
        transport.queue(500, "down");

        DidHttpException error = assertThrows(DidHttpException.class,
            () -> client.publicKeys());

        assertEquals(500, error.getStatusCode());
        assertEquals("down", error.getBody());
    }

    @Test
    public void publicKeys_UnreadableListRaises() {
        transport.queue(200, "[{\"publicKey\":\"x\"}]");

        assertThrows(DidHttpException.class, () -> client.publicKeys());
    }

    @Test
    public void publicKeyFor_ReturnsTheKeyInForce() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(3)));

        SigningKey key = client.publicKeyFor(fodId);

        assertEquals(WEEK2, key.getStartsAt());
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void publicKeyFor_OversizedObjectIsRefusedBeforeKeyFetch()
            throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> oversizedFodId(key2, WEEK2));
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void publicKeyFor_RefetchesWhenDateIsBeyondTheNewestStart()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        transport.queue(200, keyList("startsAt", false));
        client.publicKeys();
        FodId fodId = key3.fodIdAt(
            canonicalPayload(), WEEK3.plus(Duration.ofDays(8)));

        SigningKey key = client.publicKeyFor(fodId);

        assertEquals(WEEK3, key.getStartsAt());
        assertEquals(2, transport.requests.size());
    }

    @Test
    public void publicKeyFor_DoesNotRefetchStraightAfterTheFirstFetch()
            throws Exception {
        // A list fetched for this very call cannot get better by fetching
        // again, so a date the fresh list does not reach costs one use,
        // not two.
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key3.fodIdAt(
            canonicalPayload(), WEEK3.plus(Duration.ofDays(8)));

        SigningKey key = client.publicKeyFor(fodId);

        assertEquals(WEEK3, key.getStartsAt());
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void publicKeyFor_RefetchesWhenNoKeyCoversTheDate()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        transport.queue(200, keyList("startsAt", false));
        client.publicKeys();
        FodId fodId = key1.fodIdAt(
            canonicalPayload(), WEEK1.minus(Duration.ofDays(1)));

        SigningKey key = client.publicKeyFor(fodId);

        assertNull(key);
        assertEquals(2, transport.requests.size());
    }

    @Test
    public void publicKeyFor_RefetchesWhenTheListIsADayOld()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        client.publicKeyFor(fodId);
        clock.advance(Duration.ofHours(25));
        client.publicKeyFor(fodId);

        assertEquals(2, transport.requests.size());
    }

    @Test
    public void publicKeyFor_DoesNotRefetchWithinADay() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        client.publicKeyFor(fodId);
        clock.advance(Duration.ofHours(23));
        client.publicKeyFor(fodId);

        assertEquals(1, transport.requests.size());
    }

    @Test
    public void publicKeyFor_RefetchFailureRaises()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));
        client.publicKeyFor(fodId);
        clock.advance(Duration.ofHours(25));

        // Nothing queued, so the refetch fails with an I/O error.
        IOException error = assertThrows(IOException.class,
            () -> client.publicKeyFor(fodId));

        assertTrue(error.getMessage().contains("Nothing queued"));
        assertEquals(2, transport.requests.size());
    }

    // ----- Selection -----

    @Test
    public void candidates_KeyInForceOnly_AwayFromBoundaries() throws Exception {
        List<SigningKey> keys = DidClient.parseKeys(keyList("startsAt", false));

        List<SigningKey> candidates = DidClient.candidatesFor(
            keys, WEEK2.plus(Duration.ofDays(3)));

        assertEquals(1, candidates.size());
        assertEquals(WEEK2, candidates.get(0).getStartsAt());
    }

    @Test
    public void candidates_EarlierNeighbourJustAfterABoundary() throws Exception {
        List<SigningKey> keys = DidClient.parseKeys(keyList("startsAt", false));

        List<SigningKey> candidates = DidClient.candidatesFor(
            keys, WEEK2.plus(Duration.ofMinutes(5)));

        assertEquals(2, candidates.size());
        assertEquals(WEEK2, candidates.get(0).getStartsAt());
        assertEquals(WEEK1, candidates.get(1).getStartsAt());
    }

    @Test
    public void candidates_LaterNeighbourJustBeforeABoundary() throws Exception {
        List<SigningKey> keys = DidClient.parseKeys(keyList("startsAt", false));

        List<SigningKey> candidates = DidClient.candidatesFor(
            keys, WEEK2.minus(Duration.ofMinutes(5)));

        assertEquals(2, candidates.size());
        assertEquals(WEEK1, candidates.get(0).getStartsAt());
        assertEquals(WEEK2, candidates.get(1).getStartsAt());
    }

    @Test
    public void candidates_NoneBeforeTheSchedule() throws Exception {
        List<SigningKey> keys = DidClient.parseKeys(keyList("startsAt", false));

        assertTrue(DidClient.candidatesFor(
            keys, WEEK1.minus(Duration.ofDays(1))).isEmpty());
        // Within the tolerance of the first start, the first key applies.
        assertEquals(1, DidClient.candidatesFor(
            keys, WEEK1.minus(Duration.ofMinutes(5))).size());
    }

    // ----- Offline signature verification -----

    @Test
    public void verifySignature_TrueWithTheKeyInForce() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        assertTrue(client.verifySignature(fodId));
        assertEquals(DidClient.SignatureCheck.VERIFIED,
            client.verifySignatureDetailed(fodId));
    }

    @Test
    public void verifySignature_FalseWithTheWrongKey() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodIdTestFactory unpublished = new FodIdTestFactory();
        FodId fodId = unpublished.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        assertFalse(client.verifySignature(fodId));
        assertEquals(DidClient.SignatureCheck.INVALID,
            client.verifySignatureDetailed(fodId));
    }

    @Test
    public void verifySignature_FalseWithAPublishedKeyFromAnotherPeriod()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        // Signed with week 1's key but dated well inside week 2, which is
        // what a leaked key from an earlier period would produce.
        FodId fodId = key1.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        assertFalse(client.verifySignature(fodId));
    }

    @Test
    public void verifySignature_RefetchFailureRaisesRatherThanFalse()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        client.publicKeys();
        FodIdTestFactory missingKey = new FodIdTestFactory();
        FodId fodId = missingKey.fodIdAt(
            canonicalPayload(), WEEK3.plus(Duration.ofDays(8)));

        // The held schedule cannot contain the correct key, and the
        // required refetch has no queued answer.
        assertThrows(IOException.class, () -> client.verifySignature(fodId));
        assertEquals(2, transport.requests.size());
    }

    @Test
    public void verifySignature_EarlierNeighbourWithinToleranceAfterBoundary()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId inside = key1.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofMinutes(5)));
        FodId outside = key1.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofMinutes(20)));

        assertTrue(client.verifySignature(inside));
        assertFalse(client.verifySignature(outside));
    }

    @Test
    public void verifySignature_LaterNeighbourWithinToleranceBeforeBoundary()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId inside = key2.fodIdAt(
            canonicalPayload(), WEEK2.minus(Duration.ofMinutes(5)));
        FodId outside = key2.fodIdAt(
            canonicalPayload(), WEEK2.minus(Duration.ofMinutes(20)));

        assertTrue(client.verifySignature(inside));
        assertFalse(client.verifySignature(outside));
    }

    @Test
    public void verifySignature_NoKeyCoversADateBeforeTheSchedule()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key1.fodIdAt(
            canonicalPayload(), WEEK1.minus(Duration.ofDays(1)));

        assertEquals(DidClient.SignatureCheck.NO_KEY_COVERS_DATE,
            client.verifySignatureDetailed(fodId));
        assertFalse(client.verifySignature(fodId));
    }

    @Test
    public void verifySignature_FalseForVersion2() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = FodId.fromOwid(key2.signedOwidAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)),
            Version.VERSION2));

        assertEquals(Version.VERSION2, fodId.getVersion());
        assertEquals(DidClient.SignatureCheck.UNSUPPORTED_VERSION,
            client.verifySignatureDetailed(fodId));
        assertFalse(client.verifySignature(fodId));
    }

    @Test
    public void verifySignature_FalseForPayloadShorterThanBase()
            throws Exception {
        // Only the Reserved type parses with a payload below the 37-byte
        // base, which is exactly the shape the cloud refuses on length.
        byte[] payload = new byte[FodId.HEADER_LENGTH + 4];
        payload[FodId.FLAGS_OFFSET] = (byte) 0b1100_0000;
        FodId fodId = key2.fodIdAt(payload, WEEK2.plus(Duration.ofDays(1)));

        assertEquals(DidClient.SignatureCheck.MALFORMED_PAYLOAD,
            client.verifySignatureDetailed(fodId));
        assertFalse(client.verifySignature(fodId));
        // Refused on shape before any key is needed.
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verifySignature_TrueForPayloadLongerThanBase() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = maximumFodId(
            key2, WEEK2.plus(Duration.ofDays(1)));

        assertTrue(client.verifySignature(fodId));
    }

    @Test
    public void verifySignature_OversizedIdentifierIsRefusedBeforeKeyFetch()
            throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> oversizedFodId(
                key2, WEEK2.plus(Duration.ofDays(1))));
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verifySignature_TrueForRandomBaseLength() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalRandomPayload(), WEEK2.plus(Duration.ofDays(1)));

        assertEquals(IdType.RANDOM, fodId.getType());
        assertTrue(client.verifySignature(fodId));
    }

    // ----- Cloud signature verification -----

    @Test
    public void verify_ValidAnswers200True() throws Exception {
        transport.queue(200, "{\"valid\":true}");
        FodId fodId = key2.fodIdAt(canonicalPayload(), WEEK2);

        assertTrue(client.verify(fodId));

        HttpTransport.Request request = transport.last();
        assertEquals("GET", request.getMethod());
        assertEquals(ENDPOINT + "id/verify/resource?51did="
            + fodId.asBase64Url() + "&owid=" + fodId.asBase64Url(),
            request.getUrl());
        assertFalse(request.getUrl().contains("licence"));
        assertNull(request.getBody());
    }

    @Test
    public void verify_AcceptsMaximumPaddedUnpaddedAndObjectForms()
            throws Exception {
        transport.queue(200, "{\"valid\":true}");
        transport.queue(200, "{\"valid\":true}");
        transport.queue(200, "{\"valid\":true}");
        FodId fodId = maximumFodId(key2, WEEK2);
        String padded = fodId.asBase64();
        String unpadded = fodId.asBase64Url();

        assertEquals(MAXIMUM_BYTE_LENGTH, fodId.asByteArray().length);
        assertEquals(MAXIMUM_BASE64_LENGTH, padded.length());
        assertEquals(MAXIMUM_BASE64_URL_LENGTH, unpadded.length());
        assertTrue(client.verify(padded));
        assertTrue(client.verify(unpadded));
        assertTrue(client.verify(fodId));
        assertEquals(3, transport.requests.size());
    }

    @Test
    public void verify_OversizedStringIsRefusedBeforeTransport() {
        assertThrows(IllegalArgumentException.class,
            () -> client.verify(repeat('A', MAXIMUM_BASE64_LENGTH + 1)));

        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verify_OversizedObjectIsRefusedBeforeTransport()
            throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> oversizedFodId(key2, WEEK2));
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verify_InvalidAnswers400False() throws Exception {
        transport.queue(400, "{\"valid\":false}");

        assertFalse(client.verify("AwAA"));
    }

    @Test
    public void verify_ErrorsAnswer400Raises() {
        transport.queue(400, "{\"errors\":[\"Value for 51did is not a valid "
            + "Base64-encoded 51Did: 'x'.\"]}");

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class, () -> client.verify("x"));

        assertTrue(error.getMessage().contains("not a valid"));
    }

    @Test
    public void verify_OtherStatusRaisesWithStatusAndBody() {
        transport.queue(401, "{\"errors\":[\"bad key\"]}");

        DidHttpException error = assertThrows(DidHttpException.class,
            () -> client.verify("AwAA"));

        assertEquals(401, error.getStatusCode());
        assertTrue(error.getBody().contains("bad key"));
    }

    @Test
    public void verify_TransportFailureRaisesIoException() {
        assertThrows(IOException.class, () -> client.verify("AwAA"));
    }

    // ----- Redeem -----

    @Test
    public void redeem_RedeemedWithFactors() throws Exception {
        String body = "{\"signature\":\"verified\",\"context\":\"mismatch\","
            + "\"factors\":{\"transport\":\"verified\",\"device\":\"mismatch\","
            + "\"browserip\":\"verified\",\"connectionip\":\"verified\","
            + "\"asn\":\"verified\",\"browser\":\"mismatch\"},"
            + "\"verifiedAt\":\"2026-08-07T09:15:32Z\","
            + "\"secondsSinceVerified\":2}";
        transport.queue(200, body);
        FodId fodId = key2.fodIdAt(canonicalPayload(), WEEK2);

        RedeemResult result = client.redeem(fodId, "sealed", "abc");

        assertEquals(RedeemResult.Context.MISMATCH, result.getContext());
        assertEquals("mismatch", result.getContextValue());
        assertEquals(RedeemResult.Signature.VERIFIED, result.getSignature());
        assertTrue(result.hasFactors());
        assertEquals(Arrays.asList("transport", "device", "browserip",
            "connectionip", "asn", "browser"),
            new ArrayList<String>(result.getFactors().keySet()));
        assertEquals(RedeemResult.Factor.VERIFIED,
            result.getFactors().get("transport"));
        assertEquals(RedeemResult.Factor.MISMATCH,
            result.getFactors().get("device"));
        assertEquals(Instant.parse("2026-08-07T09:15:32Z"),
            result.getVerifiedAt());
        assertEquals(Integer.valueOf(2), result.getSecondsSinceVerified());
        assertEquals(200, result.getStatusCode());
        assertEquals(body, result.getRaw());

        HttpTransport.Request request = transport.last();
        assertEquals("POST", request.getMethod());
        assertEquals(ENDPOINT + "id/redeem", request.getUrl());
        assertFalse(request.getUrl().contains("licence"));
        assertFalse(request.getUrl().contains("resource"));
        assertTrue(request.getHeaders().get("Content-Type")
            .startsWith("application/x-www-form-urlencoded"));
        String form = new String(request.getBody(), StandardCharsets.UTF_8);
        assertTrue(form.startsWith("resource=resource&51did="
            + DidClient.encode(fodId.asBase64()) + "&"));
        assertTrue(form.contains("&result=sealed"));
        assertTrue(form.contains("&challenge=abc"));
        assertTrue(form.contains("&license=licence"));
    }

    @Test
    public void redeem_RedeemedWithoutFactors() throws Exception {
        transport.queue(200, "{\"signature\":\"verified\","
            + "\"context\":\"verified\","
            + "\"verifiedAt\":\"2026-08-07T09:15:32Z\","
            + "\"secondsSinceVerified\":0}");

        RedeemResult result = client.redeem("AwAA", "sealed", "abc");

        assertEquals(RedeemResult.Context.VERIFIED, result.getContext());
        assertEquals(RedeemResult.Signature.VERIFIED, result.getSignature());
        assertFalse(result.hasFactors());
        assertTrue(result.getFactors().isEmpty());
        assertEquals(Integer.valueOf(0), result.getSecondsSinceVerified());
        assertNotNull(result.getVerifiedAt());
    }

    @Test
    public void redeem_InvalidSignatureIsReported() throws Exception {
        transport.queue(200, "{\"signature\":\"invalid\","
            + "\"context\":\"verified\","
            + "\"verifiedAt\":\"2026-08-07T09:15:32Z\","
            + "\"secondsSinceVerified\":1}");

        RedeemResult result = client.redeem("AwAA", "sealed", "abc");

        assertEquals(RedeemResult.Signature.INVALID, result.getSignature());
    }

    @Test
    public void redeem_Expired() throws Exception {
        transport.queue(200, "{\"context\":\"expired\","
            + "\"verifiedAt\":\"2026-08-07T09:15:32Z\","
            + "\"secondsSinceVerified\":14}");

        RedeemResult result = client.redeem("AwAA", "sealed", "abc");

        assertEquals(RedeemResult.Context.EXPIRED, result.getContext());
        assertEquals(RedeemResult.Signature.UNKNOWN, result.getSignature());
        assertEquals(Integer.valueOf(14), result.getSecondsSinceVerified());
        assertEquals(Instant.parse("2026-08-07T09:15:32Z"),
            result.getVerifiedAt());
        assertFalse(result.hasFactors());
    }

    @Test
    public void redeem_Replayed() throws Exception {
        transport.queue(200, "{\"context\":\"replayed\"}");

        RedeemResult result = client.redeem("AwAA", "sealed", "abc");

        assertEquals(RedeemResult.Context.REPLAYED, result.getContext());
        assertNull(result.getVerifiedAt());
        assertNull(result.getSecondsSinceVerified());
    }

    @Test
    public void redeem_Unreadable() throws Exception {
        transport.queue(200, "{\"context\":\"unreadable\"}");

        RedeemResult result = client.redeem("AwAA", "sealed", "abc");

        assertEquals(RedeemResult.Context.UNREADABLE, result.getContext());
        assertEquals(RedeemResult.Signature.UNKNOWN, result.getSignature());
    }

    @Test
    public void redeem_503Unconfirmed() throws Exception {
        transport.queue(503, "{\"context\":\"unconfirmed\"}");

        RedeemResult result = client.redeem("AwAA", "sealed", "abc");

        assertEquals(RedeemResult.Context.UNCONFIRMED, result.getContext());
        assertEquals(503, result.getStatusCode());
    }

    @Test
    public void redeem_UnknownContextFailsClosedAndKeepsTheRawValue()
            throws Exception {
        transport.queue(200, "{\"context\":\"something-new\"}");

        RedeemResult result = client.redeem("AwAA", "sealed", "abc");

        assertEquals(RedeemResult.Context.UNREADABLE, result.getContext());
        assertEquals("something-new", result.getContextValue());
    }

    @Test
    public void redeem_MissingContextFailsClosed() throws Exception {
        transport.queue(200, "{}");

        RedeemResult result = client.redeem("AwAA", "sealed", "abc");

        assertEquals(RedeemResult.Context.UNREADABLE, result.getContext());
        assertEquals("unreadable", result.getContextValue());
    }

    @Test
    public void redeem_400ErrorsRaisesArgumentError() {
        transport.queue(400, "{\"errors\":[\"'x' is not a valid "
            + "Base64-encoded 51Did.\"]}");

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> client.redeem("x", "sealed", "abc"));

        assertTrue(error.getMessage().contains("not a valid"));
    }

    @Test
    public void redeem_OversizedInputsAreRefusedBeforeTransport()
            throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> client.redeem(
                repeat('A', MAXIMUM_BASE64_LENGTH + 1), "sealed", "abc"));
        assertThrows(IllegalArgumentException.class,
            () -> oversizedFodId(key2, WEEK2));
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void redeem_404RaisesNotSupported() {
        transport.queue(404, "Not Found");

        DidNotSupportedException error = assertThrows(
            DidNotSupportedException.class,
            () -> client.redeem("AwAA", "sealed", "abc"));

        assertEquals(404, error.getStatusCode());
        assertEquals("Not Found", error.getBody());
        assertTrue(error.getMessage().contains(ENDPOINT));
    }

    @Test
    public void redeem_OtherStatusRaisesWithStatusAndBody() {
        transport.queue(500, "boom");

        DidHttpException error = assertThrows(DidHttpException.class,
            () -> client.redeem("AwAA", "sealed", "abc"));

        assertEquals(500, error.getStatusCode());
        assertEquals("boom", error.getBody());
    }

    @Test
    public void redeem_NonJson200Raises() {
        transport.queue(200, "<html>proxy</html>");

        DidHttpException error = assertThrows(DidHttpException.class,
            () -> client.redeem("AwAA", "sealed", "abc"));

        assertEquals(200, error.getStatusCode());
    }

    @Test
    public void redeem_TransportFailureRaisesIoException() {
        assertThrows(IOException.class,
            () -> client.redeem("AwAA", "sealed", "abc"));
    }

    @Test
    public void redeem_WithoutLicenceKeyOmitsTheField() throws Exception {
        DidClient noLicence = DidClient.builder("resource")
            .endpoint(ENDPOINT).transport(transport).build();
        transport.queue(200, "{\"context\":\"unreadable\"}");

        noLicence.redeem("AwAA", "sealed", null);

        String form = new String(
            transport.last().getBody(), StandardCharsets.UTF_8);
        assertFalse(form.contains("license"));
        assertTrue(form.endsWith("&challenge="));
    }

    @Test
    public void redeem_FormEncodesTheValues() throws Exception {
        transport.queue(200, "{\"context\":\"unreadable\"}");

        client.redeem("AwAA+/==", "a b&c", "x=y");

        String form = new String(
            transport.last().getBody(), StandardCharsets.UTF_8);
        assertTrue(form.startsWith("resource=resource&51did=AwAA%2B%2F%3D%3D&"));
        assertTrue(form.contains("&result=a+b%26c&"));
        assertTrue(form.contains("&challenge=x%3Dy&"));
    }

    // ----- Helpers -----

    private static byte[] maximumPayload() {
        byte[] payload = new byte[MAXIMUM_PAYLOAD_LENGTH];
        System.arraycopy(
            canonicalPayload(), 0, payload, 0, FodId.PAYLOAD_LENGTH);
        return payload;
    }

    private static byte[] oversizedPayload() {
        return Arrays.copyOf(maximumPayload(), MAXIMUM_PAYLOAD_LENGTH + 1);
    }

    private static FodId maximumFodId(
            FodIdTestFactory factory, Instant date) throws OwidException {
        return factory.fodIdAt(maximumPayload(), date, MAXIMUM_TEST_DOMAIN);
    }

    private static FodId oversizedFodId(
            FodIdTestFactory factory, Instant date) throws OwidException {
        return factory.fodIdAt(oversizedPayload(), date, MAXIMUM_TEST_DOMAIN);
    }

    private static String repeat(char value, int count) {
        char[] characters = new char[count];
        Arrays.fill(characters, value);
        return new String(characters);
    }

    private String keyList(String dateField, boolean withWeekStart) {
        JSONArray array = new JSONArray();
        array.put(keyEntry(dateField, WEEK1, key1, withWeekStart));
        array.put(keyEntry(dateField, WEEK2, key2, withWeekStart));
        array.put(keyEntry(dateField, WEEK3, key3, withWeekStart));
        return array.toString();
    }

    private static JSONObject keyEntry(
            String dateField, Instant startsAt, FodIdTestFactory key) {
        return keyEntry(dateField, startsAt, key, false);
    }

    private static JSONObject keyEntry(
            String dateField,
            Instant startsAt,
            FodIdTestFactory key,
            boolean withWeekStart) {
        JSONObject entry = new JSONObject();
        // The cloud writes the C# round-trip form, with seven fractional
        // digits, so that is what the parser is given.
        entry.put(dateField, startsAt.toString()
            .replace("Z", ".0000000Z"));
        if (withWeekStart) {
            // A wrong value on purpose, so a parser that read it would
            // select the wrong key and fail the selection tests.
            entry.put("weekStart", "2001-01-01T00:00:00.0000000Z");
        }
        entry.put("publicKey", key.publicPem);
        return entry;
    }

    /** Records every request and answers from a queue. */
    static final class FakeTransport implements HttpTransport {

        final List<Request> requests = new ArrayList<Request>();
        private final Deque<Response> responses = new ArrayDeque<Response>();

        void queue(int status, String body) {
            responses.add(new Response(status, body));
        }

        Request last() {
            return requests.get(requests.size() - 1);
        }

        @Override
        public Response send(Request request) throws IOException {
            requests.add(request);
            if (responses.isEmpty()) {
                throw new IOException("Nothing queued for " + request.getUrl());
            }
            return responses.removeFirst();
        }
    }

    /** A clock the test moves by hand. */
    static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
