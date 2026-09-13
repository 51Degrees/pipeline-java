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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

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
            client.verifySignatureDetailed(fodId).join());
        assertTrue(client.verifySignature(fodId).join());
        assertTrue(client.verify(fodId).join());
    }

    @Test
    public void redeem_GarbageResult_IsUnreadable() throws Exception {
        FodId fodId = create();

        RedeemResult result;
        try {
            result = client.redeem(fodId, "not-base64url!!", "live-test")
                .join();
        } catch (CompletionException reported) {
            if (reported.getCause() instanceof DidNotSupportedException) {
                Assume.assumeNoException(
                    "The host does not offer the creator context.",
                    reported.getCause());
                return;
            }
            throw reported;
        }

        assertEquals(200, result.getStatusCode());
        assertEquals(RedeemResult.Context.UNREADABLE, result.getContext());
    }

    /**
     * Creates a 51Did for this connection through the cloud {@code json}
     * endpoint, the same call a page or the cloud request engine makes.
     */
    private FodId create() throws Exception {
        // id.usage is required. Without it the service takes the caller
        // as not having asked for a 51Did at all and creates none, so this
        // skipped on every run and the job reported success having tested
        // nothing.
        String url = client.getEndpoint() + "json?resource="
            + DidClient.encode(resourceKey)
            + "&id.usage=non-marketing&values=FODiD.IdProbGlobal";
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
    /**
     * The versioned Model Terms for Marketing document a marketing 51Did is
     * created under.
     * <p>
     * Written out here rather than read from the package, because a test
     * that asked the package what it expects would agree with itself
     * whatever the package said. The literal is what a receiver has to be
     * able to fetch.
     */
    private static final String MODEL_TERMS_FOR_MARKETING_2 =
        "https://m4ow.uk/mtm/2.txt";

    /**
     * Asks the {@code json} endpoint for a 51Did with the given query
     * parameter and returns every identifier it answered with. An empty
     * list means the resource key is not entitled to that usage, which the
     * caller reports rather than fails.
     */
    private List<FodId> identifiersFor(String name, String value)
            throws Exception {
        String url = client.getEndpoint() + "json?resource="
            + DidClient.encode(resourceKey)
            + "&" + name + "=" + DidClient.encode(value)
            + "&values=FODiD.IdProbGlobal&values=FODiD.IdProbLic";
        HttpURLConnection connection = (HttpURLConnection)
            URI.create(url).toURL().openConnection();
        connection.setRequestProperty("User-Agent", "pipeline.did live test");
        int status = connection.getResponseCode();
        String body = readAll(status >= 400
            ? connection.getErrorStream()
            : connection.getInputStream());
        assertEquals("Creating a 51Did: " + body, 200, status);

        List<FodId> identifiers = new ArrayList<>();
        JSONObject fodid = new JSONObject(body).optJSONObject("fodid");
        if (fodid == null) {
            return identifiers;
        }
        for (String field : new String[] { "idprobglobal", "idproblic" }) {
            String found = fodid.optString(field, null);
            if (found != null && !found.isEmpty()) {
                identifiers.add(FodId.fromBase64(found));
            }
        }
        return identifiers;
    }

    /**
     * Asserts the terms and every field the flags byte carries, read
     * through the accessors rather than by masking. The usage values are
     * cumulative, being 001, 011 and 111, so a caller masking the byte for
     * the non-marketing bit reads every marketing identifier as
     * non-marketing.
     */
    private void assertAligned(
            String label,
            FodId fodId,
            Usage usage,
            String terms,
            boolean fromConsent) {
        assertEquals(label + ": usage", usage, fodId.getUsage());
        assertEquals(
            label + ": whether the usage came from a consent string",
            fromConsent, fodId.isUsageFromConsent());
        assertEquals(label + ": terms", terms, fodId.getTerms());
        assertEquals(
            label + ": an idprob* value must be a probabilistic identifier",
            IdType.PROBABILISTIC, fodId.getType());
    }

    /**
     * Every {@code id.usage} the service offers, read back through the
     * package.
     * <p>
     * A non-marketing identifier may not reach a demand source at all, so
     * there is nothing for a receiver to agree to and it states no terms.
     * The two marketing usages both carry the Model Terms for Marketing,
     * and those are the rows that show the service wrote the byte, because
     * an identifier from a service predating the Terms release ends at the
     * match key and reads as no terms.
     */
    @Test
    public void everyUsageReadsBackTheTermsAndFlagsTheServiceWrote()
            throws Exception {
        String[][] cases = {
            { "non-marketing", "NON_MARKETING", null },
            { "standard", "STANDARD", MODEL_TERMS_FOR_MARKETING_2 },
            { "personalized", "PERSONALIZED", MODEL_TERMS_FOR_MARKETING_2 },
        };

        int checked = 0;
        for (String[] one : cases) {
            String name = one[0];
            Usage usage = Usage.valueOf(one[1]);
            String terms = one[2];

            List<FodId> identifiers = identifiersFor("id.usage", name);
            if (identifiers.isEmpty()) {
                System.err.println("id.usage=" + name + ": no identifier "
                    + "returned, so this key is not entitled to that usage.");
                continue;
            }
            for (int i = 0; i < identifiers.size(); i++) {
                assertAligned(name + "[" + i + "]", identifiers.get(i),
                    usage, terms, false);
            }
            if (terms != null) {
                checked += identifiers.size();
            }
        }

        if (checked == 0) {
            System.err.println("NOTHING PROVEN: this resource key returned "
                + "no marketing 51Did, so no terms address was read. Use a "
                + "key entitled to the standard or personalized usage.");
        }
    }

    /**
     * A consent management platform sends an IAB TCF consent string and no
     * usage of its own. The service decodes the string, decides the usage
     * from the purposes it grants, and records in the identifier that it
     * did so, which is bit 3 of the flags byte.
     * <p>
     * This is the half a caller cannot state for itself. An identifier
     * whose usage was stated in the request and one whose usage was decoded
     * from a consent string are both legitimate, and they are different
     * assertions about how the permission was obtained, so a receiver has
     * to be able to tell them apart.
     * <p>
     * The strings are the ones the cloud's own IabTcfElement tests use,
     * repeated here rather than shared, for the same reason as the address
     * above. The first grants all twelve purposes and the second the
     * Appendix 1 standard set of 1, 2, 7, 8 and 11.
     */
    @Test
    public void consentStringSetsTheUsageFromConsentBit() throws Exception {
        String[][] cases = {
            { "AAAAAAAAAAAAAAAAAAAAAAAAAP_w", "PERSONALIZED" },
            { "AAAAAAAAAAAAAAAAAAAAAAAAAMMg", "STANDARD" },
        };

        int proven = 0;
        for (String[] one : cases) {
            Usage usage = Usage.valueOf(one[1]);

            // No id.usage is sent. A stated usage wins over a consent
            // string, so sending one would leave the bit clear and this
            // would prove the opposite of what it says.
            List<FodId> identifiers = identifiersFor("tcstring", one[0]);
            if (identifiers.isEmpty()) {
                System.err.println("consent string granting " + usage
                    + ": no identifier returned, so this key is not entitled "
                    + "to that marketing usage.");
                continue;
            }
            for (int i = 0; i < identifiers.size(); i++) {
                // A consent string granting a marketing usage produces a
                // marketing identifier, so the terms travel with it too.
                assertAligned("consent/" + usage + "[" + i + "]",
                    identifiers.get(i), usage,
                    MODEL_TERMS_FOR_MARKETING_2, true);
            }
            proven += identifiers.size();
        }

        if (proven == 0) {
            System.err.println("NOTHING PROVEN: this resource key returned "
                + "no identifier for either consent string, so the "
                + "usage-from-consent bit was never read.");
        }
    }
}
