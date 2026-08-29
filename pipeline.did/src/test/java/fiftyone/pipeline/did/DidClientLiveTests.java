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

import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests against the live cloud, skipped unless {@code _51DEGREES_RESOURCE_KEY}
 * (or the older {@code RESOURCE_KEY}) is set. {@code FOD_CLOUD_API_URL}
 * points them at another host, and {@code _51DEGREES_LICENSE_KEY} (or
 * {@code LICENSE_KEY}) supplies the licence key where the account holds
 * one. Each test creates a 51Did through the cloud {@code json} endpoint,
 * which is one use against the resource key, plus one for each cloud call
 * it then makes.
 */
public class DidClientLiveTests {

    private String resourceKey;
    private DidClient client;

    @Before
    public void init() {
        resourceKey = env("_51DEGREES_RESOURCE_KEY", "RESOURCE_KEY");
        Assume.assumeTrue(
            "Set _51DEGREES_RESOURCE_KEY to run the live 51Did cloud tests.",
            resourceKey != null);
        client = new DidClient(
            resourceKey, env("_51DEGREES_LICENSE_KEY", "LICENSE_KEY"));
    }

    @Test
    public void create_Parse_VerifyOffline_VerifyThroughTheCloud()
            throws Exception {
        FodId fodId = create();

        assertEquals(DidClient.SignatureCheck.VERIFIED,
            client.verifySignatureDetailed(fodId));
        assertTrue(client.verifySignature(fodId));
        assertTrue(client.verify(fodId));
    }

    @Test
    public void redeem_GarbageResult_IsUnreadable() throws Exception {
        FodId fodId = create();

        RedeemResult result;
        try {
            result = client.redeem(fodId, "not-base64url!!", "live-test");
        } catch (DidNotSupportedException unsupported) {
            Assume.assumeNoException(
                "The host does not offer the creator context.", unsupported);
            return;
        }

        assertEquals(200, result.getStatusCode());
        assertEquals(RedeemResult.Context.UNREADABLE, result.getContext());
    }

    /**
     * Creates a 51Did for this connection through the cloud {@code json}
     * endpoint, the same call a page or the cloud request engine makes.
     */
    private FodId create() throws Exception {
        String url = client.getEndpoint() + "json?resource="
            + DidClient.encode(resourceKey) + "&values=FODiD.IdProbGlobal";
        HttpURLConnection connection = (HttpURLConnection)
            URI.create(url).toURL().openConnection();
        connection.setRequestProperty("User-Agent", "pipeline.did live test");
        int status = connection.getResponseCode();
        String body = readAll(status >= 400
            ? connection.getErrorStream()
            : connection.getInputStream());
        assertEquals("Creating a 51Did: " + body, 200, status);
        JSONObject fodid = new JSONObject(body).optJSONObject("fodid");
        String value = fodid == null
            ? null
            : fodid.optString("idprobglobal", null);
        Assume.assumeTrue(
            "The resource key does not return FODiD.IdProbGlobal.",
            value != null);
        return FodId.fromBase64(value);
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
    }

    private static String env(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && value.trim().isEmpty() == false) {
                return value.trim();
            }
        }
        return null;
    }
}
