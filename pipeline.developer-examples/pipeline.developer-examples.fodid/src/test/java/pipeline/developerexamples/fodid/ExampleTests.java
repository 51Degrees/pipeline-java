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
import fiftyone.pipeline.did.DidClient;
import fiftyone.pipeline.did.FodId;
import fiftyone.pipeline.did.HttpTransport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExampleTests {

    private static final String ENDPOINT = "https://example.test/api/v4/";

    private Crypto crypto;
    private String did;
    private FakeTransport transport;
    private DidClient client;

    @BeforeEach
    public void init() throws Exception {
        crypto = Crypto.generate();
        Creator creator = Creator.create("51degrees.com", crypto);
        // As the page sends it, in the URL-safe alphabet without padding.
        did = FodId.fromOwid(creator.createBytes(samplePayload()))
            .asBase64Url();
        transport = new FakeTransport();
        client = DidClient.builder("resource")
            .licenceKey("licence")
            .endpoint(ENDPOINT)
            .transport(transport)
            .build();
    }

    /**
     * The 51Did example is fully offline, so unlike the cloud examples it must
     * complete without throwing. {@code run()} also self-checks the
     * invariant that the match key is stable while the envelope changes,
     * and throws if it does not hold.
     */
    @Test
    public void FodId_Example_Test() throws Exception {
        new Main.Example().run();
    }

    @Test
    public void Redeem_Route_Answers_In_The_Clouds_Shape_With_ServerSignature() {
        transport.queue(200, keyList(crypto));
        transport.queue(200, "{\"signature\":\"verified\","
            + "\"context\":\"verified\","
            + "\"verifiedAt\":\"2026-08-07T09:15:32Z\","
            + "\"secondsSinceVerified\":2}");

        CreatorContextDemoServer.Answer answer =
            CreatorContextDemoServer.redeem(client, did, "sealed", "abc");

        assertEquals(200, answer.status);
        assertEquals("application/json", answer.type);
        JSONObject json = new JSONObject(answer.bodyText());
        assertEquals("verified", json.getString("signature"));
        assertEquals("verified", json.getString("context"));
        assertEquals("2026-08-07T09:15:32Z", json.getString("verifiedAt"));
        assertEquals(2, json.getInt("secondsSinceVerified"));
        assertEquals("verified", json.getString("serverSignature"));
        assertFalse(json.has("factors"));

        // The keys were fetched once and the redeem was a POST to the bare
        // path, carrying the resource and licence keys in the form, never
        // in the URL.
        assertEquals(2, transport.requests.size());
        HttpTransport.Request redeem = transport.requests.get(1);
        assertEquals("POST", redeem.getMethod());
        assertEquals(ENDPOINT + "id/redeem", redeem.getUrl());
        String form = new String(redeem.getBody(), StandardCharsets.UTF_8);
        assertTrue(form.startsWith("resource=resource&51did="));
        assertTrue(form.contains("&result=sealed"));
        assertTrue(form.contains("&challenge=abc"));
        assertTrue(form.contains("&license=licence"));
    }

    @Test
    public void Redeem_Route_Relays_Factors_On_A_Mismatch() {
        transport.queue(200, keyList(crypto));
        transport.queue(200, "{\"signature\":\"verified\","
            + "\"context\":\"mismatch\","
            + "\"factors\":{\"transport\":\"verified\",\"device\":\"mismatch\","
            + "\"browserip\":\"verified\",\"connectionip\":\"verified\","
            + "\"asn\":\"verified\",\"browser\":\"verified\"},"
            + "\"verifiedAt\":\"2026-08-07T09:15:32Z\","
            + "\"secondsSinceVerified\":3}");

        CreatorContextDemoServer.Answer answer =
            CreatorContextDemoServer.redeem(client, did, "sealed", "abc");

        JSONObject json = new JSONObject(answer.bodyText());
        assertEquals("mismatch", json.getString("context"));
        assertEquals("mismatch",
            json.getJSONObject("factors").getString("device"));
        assertEquals("verified",
            json.getJSONObject("factors").getString("transport"));
    }

    @Test
    public void Redeem_Route_Reports_Its_Own_Signature_Check() throws Exception {
        // The published key is not the one that signed the identifier, so
        // the server's own check says invalid whatever the cloud says.
        transport.queue(200, keyList(Crypto.generate()));
        transport.queue(200, "{\"signature\":\"verified\","
            + "\"context\":\"verified\","
            + "\"verifiedAt\":\"2026-08-07T09:15:32Z\","
            + "\"secondsSinceVerified\":2}");

        CreatorContextDemoServer.Answer answer =
            CreatorContextDemoServer.redeem(client, did, "sealed", "abc");

        JSONObject json = new JSONObject(answer.bodyText());
        assertEquals("verified", json.getString("signature"));
        assertEquals("invalid", json.getString("serverSignature"));
    }

    @Test
    public void Redeem_Route_Relays_503_Unconfirmed() {
        transport.queue(200, keyList(crypto));
        transport.queue(503, "{\"context\":\"unconfirmed\"}");

        CreatorContextDemoServer.Answer answer =
            CreatorContextDemoServer.redeem(client, did, "sealed", "abc");

        assertEquals(503, answer.status);
        JSONObject json = new JSONObject(answer.bodyText());
        assertEquals("unconfirmed", json.getString("context"));
        assertFalse(json.has("signature"));
        assertEquals("verified", json.getString("serverSignature"));
    }

    @Test
    public void Redeem_Route_Answers_404_As_Text_For_A_Host_Without_The_Feature() {
        transport.queue(200, keyList(crypto));
        transport.queue(404, "Not Found");

        CreatorContextDemoServer.Answer answer =
            CreatorContextDemoServer.redeem(client, did, "sealed", "abc");

        assertEquals(404, answer.status);
        assertTrue(answer.type.startsWith("text/plain"));
        assertEquals("Not Found", answer.bodyText());
    }

    @Test
    public void Redeem_Route_Answers_502_When_The_Cloud_Is_Unreachable() {
        // Nothing queued, so the first request fails as the network would.
        CreatorContextDemoServer.Answer answer =
            CreatorContextDemoServer.redeem(client, did, "sealed", "abc");

        assertEquals(502, answer.status);
        JSONObject json = new JSONObject(answer.bodyText());
        assertTrue(json.has("error"));
    }

    @Test
    public void Redeem_Route_Answers_400_For_A_Malformed_51Did() {
        CreatorContextDemoServer.Answer answer =
            CreatorContextDemoServer.redeem(client, "not a 51did", "s", "c");

        assertEquals(400, answer.status);
        JSONObject json = new JSONObject(answer.bodyText());
        assertTrue(json.getJSONArray("errors").getString(0)
            .contains("not a valid"));
        assertEquals(0, transport.requests.size());
    }

    @Test
    public void Redeem_Route_Answers_400_When_The_Cloud_Refuses_The_51Did() {
        transport.queue(200, keyList(crypto));
        transport.queue(400, "{\"errors\":[\"'x' is not a valid "
            + "Base64-encoded 51Did.\"]}");

        CreatorContextDemoServer.Answer answer =
            CreatorContextDemoServer.redeem(client, did, "sealed", "abc");

        assertEquals(400, answer.status);
        JSONObject json = new JSONObject(answer.bodyText());
        assertTrue(json.getJSONArray("errors").getString(0)
            .contains("not a valid"));
    }

    // ----- Helpers -----

    /**
     * A one-entry key list in force since yesterday, so the identifier
     * signed a moment ago falls inside its period.
     */
    private static String keyList(Crypto crypto) {
        try {
            JSONObject entry = new JSONObject();
            entry.put("startsAt",
                Instant.now().minus(Duration.ofDays(1)).toString());
            entry.put("publicKey", crypto.publicKeyPem());
            return new JSONArray().put(entry).toString();
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * A canonical 37-byte Probabilistic payload: flags 0x00, License Id
     * 0x12345678 (little-endian) and a 32-byte match key 0x20..0x3F.
     */
    private static byte[] samplePayload() {
        byte[] payload = new byte[FodId.PAYLOAD_LENGTH];
        payload[FodId.LICENSE_ID_OFFSET] = 0x78;
        payload[FodId.LICENSE_ID_OFFSET + 1] = 0x56;
        payload[FodId.LICENSE_ID_OFFSET + 2] = 0x34;
        payload[FodId.LICENSE_ID_OFFSET + 3] = 0x12;
        for (int i = 0; i < FodId.MATCH_KEY_LENGTH; i++) {
            payload[FodId.MATCH_KEY_OFFSET + i] = (byte) (0x20 + i);
        }
        return payload;
    }

    /** Records every request and answers from a queue. */
    static final class FakeTransport implements HttpTransport {

        final List<Request> requests = new ArrayList<Request>();
        private final Deque<Response> responses = new ArrayDeque<Response>();

        void queue(int status, String body) {
            responses.add(new Response(status, body));
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
}
