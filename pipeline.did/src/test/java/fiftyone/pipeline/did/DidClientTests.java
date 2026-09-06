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
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static fiftyone.pipeline.did.FodIdTestFactory.canonicalPayload;
import static fiftyone.pipeline.did.FodIdTestFactory.canonicalPayloadWithSection;
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

    // Offsets either side of a key boundary, chosen far apart so that each
    // one is plainly on its own side of the tolerance whatever the
    // tolerance is set to.
    private static final Duration JUST_INSIDE = Duration.ofMinutes(1);
    private static final Duration WELL_OUTSIDE = Duration.ofHours(1);

    private FodIdTestFactory key1;
    private FodIdTestFactory key2;
    private FodIdTestFactory key3;
    private FakeTransport transport;
    private MutableClock clock;
    private DidClient client;
    /** A genuine identifier as a page sends it, signed with key2. */
    private String validDid;

    @Before
    public void init() throws OwidException {
        key1 = new FodIdTestFactory();
        key2 = new FodIdTestFactory();
        key3 = new FodIdTestFactory();
        transport = new FakeTransport();
        validDid = key2.fodIdAt(canonicalPayload(), WEEK2).asBase64Url();
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

        List<SigningKey> keys = client.publicKeys().join();

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

        List<SigningKey> keys = client.publicKeys().join();

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

        List<SigningKey> keys = client.publicKeys().join();

        assertEquals(WEEK1, keys.get(0).getStartsAt());
        assertEquals(WEEK2, keys.get(1).getStartsAt());
        assertEquals(WEEK3, keys.get(2).getStartsAt());
    }

    @Test
    public void publicKeys_SecondCallUsesTheCache() throws Exception {
        transport.queue(200, keyList("startsAt", false));

        List<SigningKey> first = client.publicKeys().join();
        List<SigningKey> second = client.publicKeys().join();

        assertSame(first, second);
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void publicKeys_FirstFetchFailureFailsTheFuture() {
        transport.queue(500, "down");

        DidHttpException error =
            failure(DidHttpException.class, client.publicKeys());

        assertEquals(500, error.getStatusCode());
        assertEquals("down", error.getBody());
    }

    @Test
    public void publicKeys_UnreadableListFailsTheFuture() {
        transport.queue(200, "[{\"publicKey\":\"x\"}]");

        failure(DidHttpException.class, client.publicKeys());
    }

    @Test
    public void publicKeyFor_ReturnsTheKeyInForce() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(3)));

        SigningKey key = client.publicKeyFor(fodId).join();

        assertEquals(WEEK2, key.getStartsAt());
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void publicKeyFor_RefetchesWhenDateIsBeyondTheNewestStart()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        transport.queue(200, keyList("startsAt", false));
        client.publicKeys().join();
        FodId fodId = key3.fodIdAt(
            canonicalPayload(), WEEK3.plus(Duration.ofDays(8)));

        SigningKey key = client.publicKeyFor(fodId).join();

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

        SigningKey key = client.publicKeyFor(fodId).join();

        assertEquals(WEEK3, key.getStartsAt());
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void publicKeyFor_RefetchesWhenNoKeyCoversTheDate()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        transport.queue(200, keyList("startsAt", false));
        client.publicKeys().join();
        FodId fodId = key1.fodIdAt(
            canonicalPayload(), WEEK1.minus(Duration.ofDays(1)));

        SigningKey key = client.publicKeyFor(fodId).join();

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

        client.publicKeyFor(fodId).join();
        clock.advance(Duration.ofHours(25));
        client.publicKeyFor(fodId).join();

        assertEquals(2, transport.requests.size());
    }

    @Test
    public void publicKeyFor_DoesNotRefetchWithinADay() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        client.publicKeyFor(fodId).join();
        clock.advance(Duration.ofHours(23));
        client.publicKeyFor(fodId).join();

        assertEquals(1, transport.requests.size());
    }

    @Test
    public void publicKeyFor_RefetchFailureFailsTheFuture()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));
        client.publicKeyFor(fodId).join();
        clock.advance(Duration.ofHours(25));

        // Nothing queued, so the refetch fails with an I/O error.
        IOException error =
            failure(IOException.class, client.publicKeyFor(fodId));

        assertTrue(error.getMessage().contains("Nothing queued"));
        assertEquals(2, transport.requests.size());
    }

    @Test
    public void publicKeys_ConcurrentCallersShareOneFetch() {
        // The transport answers only when this test says so, so both
        // calls are certainly in flight at the same time. Nothing joins
        // a future before the answer is given, so nothing waits here.
        HeldTransport held = new HeldTransport();
        DidClient shared = DidClient.builder("resource")
            .endpoint(ENDPOINT).transport(held).clock(clock).build();

        CompletableFuture<List<SigningKey>> first = shared.publicKeys();
        CompletableFuture<List<SigningKey>> second = shared.publicKeys();

        assertEquals(1, held.requests.size());
        assertFalse(first.isDone());
        assertFalse(second.isDone());

        held.answer(200, keyList("startsAt", false));

        assertSame(first.join(), second.join());
        // The fetch is over, so the next caller is answered from the
        // held list rather than from a fetch that is no longer running.
        assertSame(first.join(), shared.publicKeys().join());
        assertEquals(1, held.requests.size());
    }

    @Test
    public void publicKeys_AFailedSharedFetchFailsEveryCallerWaiting() {
        HeldTransport held = new HeldTransport();
        DidClient shared = DidClient.builder("resource")
            .endpoint(ENDPOINT).transport(held).clock(clock).build();

        CompletableFuture<List<SigningKey>> first = shared.publicKeys();
        CompletableFuture<List<SigningKey>> second = shared.publicKeys();
        held.fail(new IOException("the cloud could not be reached"));

        failure(IOException.class, first);
        failure(IOException.class, second);

        // Nothing is held, so the next caller starts a fetch of its own
        // rather than being given the failed one.
        CompletableFuture<List<SigningKey>> third = shared.publicKeys();
        assertEquals(2, held.requests.size());
        held.answer(200, keyList("startsAt", false));
        assertEquals(3, third.join().size());
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
            keys, WEEK2.plus(JUST_INSIDE));

        assertEquals(2, candidates.size());
        assertEquals(WEEK2, candidates.get(0).getStartsAt());
        assertEquals(WEEK1, candidates.get(1).getStartsAt());
    }

    @Test
    public void candidates_LaterNeighbourJustBeforeABoundary() throws Exception {
        List<SigningKey> keys = DidClient.parseKeys(keyList("startsAt", false));

        List<SigningKey> candidates = DidClient.candidatesFor(
            keys, WEEK2.minus(JUST_INSIDE));

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
            keys, WEEK1.minus(JUST_INSIDE)).size());
    }

    // ----- Offline signature verification -----

    @Test
    public void verifySignature_TrueWithTheKeyInForce() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        assertTrue(client.verifySignature(fodId).join());
        assertEquals(DidClient.SignatureCheck.VERIFIED,
            client.verifySignatureDetailed(fodId).join());
    }

    @Test
    public void verifySignature_FalseWithTheWrongKey() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodIdTestFactory unpublished = new FodIdTestFactory();
        FodId fodId = unpublished.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        assertFalse(client.verifySignature(fodId).join());
        assertEquals(DidClient.SignatureCheck.INVALID,
            client.verifySignatureDetailed(fodId).join());
    }

    @Test
    public void verifySignature_FalseWithAPublishedKeyFromAnotherPeriod()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        // Signed with week 1's key but dated well inside week 2, which is
        // what a leaked key from an earlier period would produce.
        FodId fodId = key1.fodIdAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)));

        assertFalse(client.verifySignature(fodId).join());
    }

    @Test
    public void verifySignature_RefetchFailureFailsRatherThanAnsweringFalse()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        client.publicKeys().join();
        FodIdTestFactory missingKey = new FodIdTestFactory();
        FodId fodId = missingKey.fodIdAt(
            canonicalPayload(), WEEK3.plus(Duration.ofDays(8)));

        // The held schedule cannot contain the correct key, and the
        // required refetch has no queued answer.
        failure(IOException.class, client.verifySignature(fodId));
        assertEquals(2, transport.requests.size());
    }

    @Test
    public void verifySignature_EarlierNeighbourWithinToleranceAfterBoundary()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId inside = key1.fodIdAt(
            canonicalPayload(), WEEK2.plus(JUST_INSIDE));
        FodId outside = key1.fodIdAt(
            canonicalPayload(), WEEK2.plus(WELL_OUTSIDE));

        assertTrue(client.verifySignature(inside).join());
        assertFalse(client.verifySignature(outside).join());
    }

    @Test
    public void verifySignature_LaterNeighbourWithinToleranceBeforeBoundary()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId inside = key2.fodIdAt(
            canonicalPayload(), WEEK2.minus(JUST_INSIDE));
        FodId outside = key2.fodIdAt(
            canonicalPayload(), WEEK2.minus(WELL_OUTSIDE));

        assertTrue(client.verifySignature(inside).join());
        assertFalse(client.verifySignature(outside).join());
    }

    @Test
    public void verifySignature_NoKeyCoversADateBeforeTheSchedule()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key1.fodIdAt(
            canonicalPayload(), WEEK1.minus(Duration.ofDays(1)));

        assertEquals(DidClient.SignatureCheck.NO_KEY_COVERS_DATE,
            client.verifySignatureDetailed(fodId).join());
        assertFalse(client.verifySignature(fodId).join());
    }

    @Test
    public void verifySignature_FalseForVersion2() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = FodId.fromOwid(key2.signedOwidAt(
            canonicalPayload(), WEEK2.plus(Duration.ofDays(1)),
            Version.VERSION2));

        assertEquals(Version.VERSION2, fodId.getVersion());
        assertEquals(DidClient.SignatureCheck.UNSUPPORTED_VERSION,
            client.verifySignatureDetailed(fodId).join());
        assertFalse(client.verifySignature(fodId).join());
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
            client.verifySignatureDetailed(fodId).join());
        assertFalse(client.verifySignature(fodId).join());
        // Refused on shape before any key is needed.
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verifySignature_TrueForPayloadLongerThanBase() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayloadWithSection(25), WEEK2.plus(Duration.ofDays(1)));

        assertTrue(client.verifySignature(fodId).join());
    }

    @Test
    public void verifySignature_TrueForALongContextSectionAndLongDomain()
            throws Exception {
        // A self-hosted container signs with its own creator domain, which
        // may be longer than the cloud's, and a context section of a
        // version this package does not read may be any length. Neither is
        // this client's business, so both must verify.
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalPayloadWithSection(512),
            WEEK2.plus(Duration.ofDays(1)),
            "a-rather-long-self-hosted-creator.example.internal.51degrees.com");

        assertTrue(client.verifySignature(fodId).join());
    }

    @Test
    public void verifySignature_TrueForRandomBaseLength() throws Exception {
        transport.queue(200, keyList("startsAt", false));
        FodId fodId = key2.fodIdAt(
            canonicalRandomPayload(), WEEK2.plus(Duration.ofDays(1)));

        assertEquals(IdType.RANDOM, fodId.getType());
        assertTrue(client.verifySignature(fodId).join());
    }

    // ----- Cloud signature verification -----

    @Test
    public void verify_ValidAnswers200True() throws Exception {
        transport.queue(200, "{\"valid\":true}");
        FodId fodId = key2.fodIdAt(canonicalPayload(), WEEK2);

        assertTrue(client.verify(fodId).join());

        HttpTransport.Request request = transport.last();
        assertEquals("GET", request.getMethod());
        assertEquals(ENDPOINT + "id/verify/resource?51did="
            + fodId.asBase64Url() + "&owid=" + fodId.asBase64Url(),
            request.getUrl());
        assertFalse(request.getUrl().contains("licence"));
        assertNull(request.getBody());
    }

    @Test
    public void verify_OverLongStringIsRefusedBeforeTransport() {
        // Nothing this long can be an identifier, so it is turned away
        // before the client decodes it, fetches a key or calls the cloud.
        failure(IllegalArgumentException.class,
            client.verify(repeat('A', 8192)));

        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verify_OverLongObjectIsRefusedBeforeTransport()
            throws Exception {
        FodId fodId = overLongFodId(key2, WEEK2);

        failure(IllegalArgumentException.class, client.verify(fodId));

        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verify_InvalidAnswers400False() throws Exception {
        transport.queue(400, "{\"valid\":false}");

        assertFalse(client.verify(validDid).join());
    }

    @Test
    public void verify_ErrorsAnswer400FailsWithArgumentError() {
        // The cloud's own rejection of an identifier that read locally still
        // maps to the argument failure, with the cloud's message.
        transport.queue(400, "{\"errors\":[\"Value for 51did is not a valid "
            + "Base64-encoded 51Did.\"]}");

        IllegalArgumentException error = failure(
            IllegalArgumentException.class, client.verify(validDid));

        assertTrue(error.getMessage().contains("not a valid"));
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void verify_OtherStatusFailsWithStatusAndBody() {
        transport.queue(401, "{\"errors\":[\"bad key\"]}");

        DidHttpException error =
            failure(DidHttpException.class, client.verify(validDid));

        assertEquals(401, error.getStatusCode());
        assertTrue(error.getBody().contains("bad key"));
    }

    @Test
    public void verify_TransportFailureFailsWithIoException() {
        failure(IOException.class, client.verify(validDid));
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

        RedeemResult result =
            client.redeem(fodId, "sealed", "abc").join();

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

        RedeemResult result =
            client.redeem(validDid, "sealed", "abc").join();

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

        RedeemResult result =
            client.redeem(validDid, "sealed", "abc").join();

        assertEquals(RedeemResult.Signature.INVALID, result.getSignature());
    }

    @Test
    public void redeem_Expired() throws Exception {
        transport.queue(200, "{\"context\":\"expired\","
            + "\"verifiedAt\":\"2026-08-07T09:15:32Z\","
            + "\"secondsSinceVerified\":14}");

        RedeemResult result =
            client.redeem(validDid, "sealed", "abc").join();

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

        RedeemResult result =
            client.redeem(validDid, "sealed", "abc").join();

        assertEquals(RedeemResult.Context.REPLAYED, result.getContext());
        assertNull(result.getVerifiedAt());
        assertNull(result.getSecondsSinceVerified());
    }

    @Test
    public void redeem_Unreadable() throws Exception {
        transport.queue(200, "{\"context\":\"unreadable\"}");

        RedeemResult result =
            client.redeem(validDid, "sealed", "abc").join();

        assertEquals(RedeemResult.Context.UNREADABLE, result.getContext());
        assertEquals(RedeemResult.Signature.UNKNOWN, result.getSignature());
    }

    @Test
    public void redeem_503Unconfirmed() throws Exception {
        transport.queue(503, "{\"context\":\"unconfirmed\"}");

        RedeemResult result =
            client.redeem(validDid, "sealed", "abc").join();

        assertEquals(RedeemResult.Context.UNCONFIRMED, result.getContext());
        assertEquals(503, result.getStatusCode());
    }

    @Test
    public void redeem_UnknownContextFailsClosedAndKeepsTheRawValue()
            throws Exception {
        transport.queue(200, "{\"context\":\"something-new\"}");

        RedeemResult result =
            client.redeem(validDid, "sealed", "abc").join();

        assertEquals(RedeemResult.Context.UNREADABLE, result.getContext());
        assertEquals("something-new", result.getContextValue());
    }

    @Test
    public void redeem_MissingContextFailsClosed() throws Exception {
        transport.queue(200, "{}");

        RedeemResult result =
            client.redeem(validDid, "sealed", "abc").join();

        assertEquals(RedeemResult.Context.UNREADABLE, result.getContext());
        assertEquals("unreadable", result.getContextValue());
    }

    @Test
    public void redeem_400ErrorsFailsWithArgumentError() {
        transport.queue(400, "{\"errors\":[\"'x' is not a valid "
            + "Base64-encoded 51Did.\"]}");

        IllegalArgumentException error = failure(
            IllegalArgumentException.class,
            client.redeem(validDid, "sealed", "abc"));

        assertTrue(error.getMessage().contains("not a valid"));
        assertEquals(1, transport.requests.size());
    }

    @Test
    public void redeem_OverLongInputsAreRefusedBeforeTransport()
            throws Exception {
        FodId fodId = overLongFodId(key2, WEEK2);

        failure(IllegalArgumentException.class,
            client.redeem(repeat('A', 8192), "sealed", "abc"));
        failure(IllegalArgumentException.class,
            client.redeem(fodId, "sealed", "abc"));

        assertEquals(0, transport.requests.size());
    }

    @Test
    public void redeem_404FailsWithNotSupported() {
        transport.queue(404, "Not Found");

        DidNotSupportedException error = failure(
            DidNotSupportedException.class,
            client.redeem(validDid, "sealed", "abc"));

        assertEquals(404, error.getStatusCode());
        assertEquals("Not Found", error.getBody());
        assertTrue(error.getMessage().contains(ENDPOINT));
    }

    @Test
    public void redeem_OtherStatusFailsWithStatusAndBody() {
        transport.queue(500, "boom");

        DidHttpException error = failure(DidHttpException.class,
            client.redeem(validDid, "sealed", "abc"));

        assertEquals(500, error.getStatusCode());
        assertEquals("boom", error.getBody());
    }

    @Test
    public void redeem_NonJson200FailsWithHttpError() {
        transport.queue(200, "<html>proxy</html>");

        DidHttpException error = failure(DidHttpException.class,
            client.redeem(validDid, "sealed", "abc"));

        assertEquals(200, error.getStatusCode());
    }

    @Test
    public void redeem_TransportFailureFailsWithIoException() {
        failure(IOException.class,
            client.redeem(validDid, "sealed", "abc"));
    }

    @Test
    public void redeem_WithoutLicenceKeyOmitsTheField() throws Exception {
        DidClient noLicence = DidClient.builder("resource")
            .endpoint(ENDPOINT).transport(transport).build();
        transport.queue(200, "{\"context\":\"unreadable\"}");

        noLicence.redeem(validDid, "sealed", null).join();

        String form = new String(
            transport.last().getBody(), StandardCharsets.UTF_8);
        assertFalse(form.contains("license"));
        assertTrue(form.endsWith("&challenge="));
    }

    @Test
    public void redeem_FormEncodesTheValues() throws Exception {
        transport.queue(200, "{\"context\":\"unreadable\"}");
        // The standard alphabet with padding, as the cloud issues it, so
        // the form has characters that need encoding.
        String standard = key2.fodIdAt(canonicalPayload(), WEEK2).asBase64();
        assertTrue(standard.endsWith("="));

        client.redeem(standard, "a b&c", "x=y").join();

        String form = new String(
            transport.last().getBody(), StandardCharsets.UTF_8);
        assertTrue(form.startsWith(
            "resource=resource&51did=" + DidClient.encode(standard) + "&"));
        assertTrue(form.contains("%3D&result="));
        assertTrue(form.contains("&result=a+b%26c&"));
        assertTrue(form.contains("&challenge=x%3Dy&"));
    }

    // ----- Malformed input never reaches the network -----

    @Test
    public void verify_MalformedStringIsRefusedBeforeTransport() {
        // Not base64 at all, so the OWID reader's own status is the reason,
        // and neither a key fetch nor the verify call happens.
        IllegalArgumentException error = failure(
            IllegalArgumentException.class,
            client.verify("This is not a 51Did!"));

        assertTrue(error.getMessage(),
            error.getMessage().contains("INVALID_BASE64"));
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verify_ShortPayloadStringIsRefusedBeforeTransport()
            throws Exception {
        // A genuine envelope whose payload cannot carry the 51Did header.
        String tooShort = key2.signedOwidAt(new byte[3], WEEK2).asBase64();

        IllegalArgumentException error = failure(
            IllegalArgumentException.class, client.verify(tooShort));

        assertTrue(error.getMessage(),
            error.getMessage().contains("PAYLOAD_TOO_SHORT"));
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void redeem_MalformedStringIsRefusedBeforeTransport()
            throws Exception {
        String tooShort = key2.signedOwidAt(
            Arrays.copyOf(canonicalRandomPayload(),
                FodId.RANDOM_PAYLOAD_LENGTH - 1), WEEK2).asBase64();

        failure(IllegalArgumentException.class,
            client.redeem("This is not a 51Did!", "sealed", "abc"));
        IllegalArgumentException error = failure(
            IllegalArgumentException.class,
            client.redeem(tooShort, "sealed", "abc"));

        assertTrue(error.getMessage(),
            error.getMessage().contains("INVALID_TYPE_PAYLOAD_LENGTH"));
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void verify_LongerContextSectionStringReachesTheCloud()
            throws Exception {
        // Longer than any shape this package knows is still a 51Did, so it
        // is not turned away here and the cloud is asked as usual.
        transport.queue(200, "{\"valid\":true}");
        String longer = key2.fodIdAt(
            canonicalPayloadWithSection(600), WEEK2).asBase64Url();

        assertTrue(client.verify(longer).join());

        assertEquals(1, transport.requests.size());
    }

    // ----- Invalid is not the same as could not check -----

    @Test
    public void verifySignature_TamperedSignatureIsInvalidNotAnError()
            throws Exception {
        transport.queue(200, keyList("startsAt", false));
        byte[] bytes = key2.fodIdAt(canonicalPayload(), WEEK2).asByteArray();
        bytes[bytes.length - 1] ^= (byte) 0xFF;
        FodIdParseResult read = FodId.tryFromByteArray(bytes);
        // Reading succeeds, because reading never checks the signature.
        assertTrue(read.isSuccess());
        assertEquals(FodIdParseStatus.PARSED, read.getStatus());

        assertEquals(DidClient.SignatureCheck.INVALID,
            client.verifySignatureDetailed(read.getValue()).join());
        assertFalse(client.verifySignature(read.getValue()).join());
    }

    @Test
    public void verifySignature_FirstKeyFetchFailureFailsRatherThanAnsweringFalse()
            throws Exception {
        // Nothing queued, so the key list cannot be fetched. That is an
        // error, never a verdict on the signature.
        FodId fodId = key2.fodIdAt(canonicalPayload(), WEEK2);

        failure(IOException.class, client.verifySignature(fodId));
        failure(IOException.class, client.verifySignatureDetailed(fodId));
    }

    // ----- The default transport -----

    @Test
    public void defaultTransport_RunsTheExchangeOnTheBuildersExecutor() {
        // The executor keeps the work rather than running it, so the
        // exchange never happens and the test needs no network. What it
        // shows is that the work was handed over at all, because the
        // calling thread must not do the blocking.
        Held work = new Held();
        DidClient onHold = DidClient.builder("resource")
            .endpoint(ENDPOINT).executor(work).build();

        CompletableFuture<List<SigningKey>> keys = onHold.publicKeys();

        assertEquals(1, work.tasks.size());
        assertFalse(keys.isDone());
    }

    @Test
    public void defaultTransport_ReportsAFailedExchangeThroughTheFuture() {
        Held work = new Held();
        HttpTransport blocking =
            new DidClient.UrlConnectionTransport(work);

        CompletableFuture<HttpTransport.Response> answer = blocking.send(
            new HttpTransport.Request("GET", "no-such-scheme://host/",
                Collections.<String, String>emptyMap(), null));

        assertFalse(answer.isDone());
        work.tasks.get(0).run();

        // An unknown scheme is a MalformedURLException, which is an
        // IOException, and it arrives through the future rather than
        // being thrown at whoever called send.
        failure(IOException.class, answer);
    }

    @Test
    public void defaultTransport_ReportsARefusedExecutorThroughTheFuture() {
        HttpTransport blocking = new DidClient.UrlConnectionTransport(
            work -> {
                throw new RejectedExecutionException("the pool is closed");
            });

        CompletableFuture<HttpTransport.Response> answer = blocking.send(
            new HttpTransport.Request("GET", ENDPOINT,
                Collections.<String, String>emptyMap(), null));

        failure(RejectedExecutionException.class, answer);
    }

    // ----- Helpers -----

    /**
     * The failure a future reports, taken from the cause of the
     * {@link CompletionException} that {@code join} reports it as, which
     * is how every failure of this client reaches a caller.
     */
    private static <T extends Throwable> T failure(
            Class<T> type, CompletableFuture<?> answer) {
        CompletionException reported =
            assertThrows(CompletionException.class, answer::join);
        Throwable cause = reported.getCause();
        assertNotNull("No cause on " + reported, cause);
        assertTrue(cause.toString(), type.isInstance(cause));
        return type.cast(cause);
    }

    /**
     * An identifier whose encoded form is longer than the client will take
     * from a caller. The identifier itself is perfectly good, so this only
     * shows that the object overloads pass through the same guard as the
     * string ones.
     */
    private static FodId overLongFodId(FodIdTestFactory factory, Instant date)
            throws OwidException {
        FodId fodId = factory.fodIdAt(canonicalPayloadWithSection(3200), date);
        assertTrue(fodId.asBase64Url().length() > 4096);
        return fodId;
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
        public CompletableFuture<Response> send(Request request) {
            requests.add(request);
            CompletableFuture<Response> answer =
                new CompletableFuture<Response>();
            if (responses.isEmpty()) {
                answer.completeExceptionally(
                    new IOException("Nothing queued for " + request.getUrl()));
            } else {
                answer.complete(responses.removeFirst());
            }
            return answer;
        }
    }

    /** A transport whose answers the test gives by hand. */
    static final class HeldTransport implements HttpTransport {

        final List<Request> requests = new ArrayList<Request>();
        private final Deque<CompletableFuture<Response>> pending =
            new ArrayDeque<CompletableFuture<Response>>();

        @Override
        public CompletableFuture<Response> send(Request request) {
            requests.add(request);
            CompletableFuture<Response> answer =
                new CompletableFuture<Response>();
            pending.add(answer);
            return answer;
        }

        void answer(int status, String body) {
            pending.removeFirst().complete(new Response(status, body));
        }

        void fail(Throwable failure) {
            pending.removeFirst().completeExceptionally(failure);
        }
    }

    /** An executor that keeps the work rather than running it. */
    static final class Held implements Executor {

        final List<Runnable> tasks = new ArrayList<Runnable>();

        @Override
        public void execute(Runnable work) {
            tasks.add(work);
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
