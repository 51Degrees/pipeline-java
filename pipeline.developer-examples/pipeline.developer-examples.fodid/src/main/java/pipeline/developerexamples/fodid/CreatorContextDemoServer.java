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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.swancommunity.owid.OwidException;
import fiftyone.pipeline.did.DidClient;
import fiftyone.pipeline.did.DidHttpException;
import fiftyone.pipeline.did.DidNotSupportedException;
import fiftyone.pipeline.did.FodId;
import fiftyone.pipeline.did.RedeemResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 51Did creator context demo server. Serves a page that runs the 51Did
 * flow the way production does, and redeems the encrypted result server
 * side with {@link DidClient}, adding the licence key the browser never
 * sees.
 * <p>
 * Every 51Did the 51Degrees cloud issues carries a creator context, which
 * binds the identifier to the browser and connection it was created on.
 * The flow has three steps:
 * <ol>
 * <li><b>Create</b> a 51Did by calling the {@code json} endpoint, which
 * issues an identifier for the calling connection.</li>
 * <li><b>Verify</b> it with {@code verify-full}, which returns both the
 * signature outcome and the creator context verdict only as an encrypted
 * {@code result} that the caller cannot read or forge. (A deployment
 * holding no context secret answers in the open instead.)</li>
 * <li><b>Redeem</b> the encrypted result with {@code redeem}, presenting
 * the 51Did, the encrypted result and the account's licence key, and
 * receive the true creator context verdict, when the verification
 * happened ({@code verifiedAt}) and how long ago that was
 * ({@code secondsSinceVerified}).</li>
 * </ol>
 * The browser creates the 51Did and calls {@code verify-full}, so the
 * cloud observes the browser's live connection, then the page hands the
 * encrypted result to this server, which redeems it with the licence key.
 * A fresh challenge is issued per page load and bound through both steps
 * by the cloud. A production server would also remember the value it
 * issued and reject a redemption carrying any other, which this demo
 * keeps out of scope. Once the 51Did has validated, the page offers a link
 * carrying the same identifier. Opened in a different browser, the
 * signature still verifies but the creator context does not validate,
 * which is a copied or stolen identifier caught at presentation with
 * nothing stored server side.
 * <p>
 * <b>What a run costs.</b> Every call made to the cloud is one use against
 * the subscription behind the resource key. A browser check of a 51Did
 * makes two, verify-full from the page and redeem from this server, so
 * each browser-based context check is two uses. The offline signature
 * check this server also makes costs nothing, because the client fetches
 * the cloud's public keys once and holds them.
 * <p>
 * The web server is the one that ships with the JDK. The page and the
 * stylesheet are read from the classpath, under
 * {@code fodid/creator-context/} in this module's resources. Environment
 * variables:
 * <ul>
 * <li>{@code _51DEGREES_RESOURCE_KEY}, or the older {@code RESOURCE_KEY},
 * required. The resource key of the page, public by nature.</li>
 * <li>{@code _51DEGREES_LICENSE_KEY}, or the older {@code LICENSE_KEY},
 * optional. A licence key of the same account, server side only.</li>
 * <li>{@code FOD_CLOUD_API_URL}, optional. The cloud API base including
 * the {@code /api/v4/} segment, defaulting to
 * {@code https://cloud.51degrees.com/api/v4/}. This is the same variable
 * the cloud request engine of this package honours.</li>
 * <li>{@code PORT}, optional, defaulting to {@code 5100}.</li>
 * </ul>
 * Then open {@code http://localhost:5100/}.
 */
public class CreatorContextDemoServer {

    /** Where the page and stylesheet live on the classpath. */
    static final String RESOURCES = "/fodid/creator-context/";

    static final String RESOURCE =
        env("_51DEGREES_RESOURCE_KEY", "RESOURCE_KEY");
    static final String LICENCE =
        env("_51DEGREES_LICENSE_KEY", "LICENSE_KEY");

    /**
     * The one client, built at start-up and shared by every request,
     * because it holds the cloud's public keys.
     */
    static DidClient client;

    public static void main(String[] args) throws Exception {
        if (RESOURCE == null) {
            System.err.println("Set _51DEGREES_RESOURCE_KEY (or the older "
                + "RESOURCE_KEY) to the resource key of the page.");
            System.exit(1);
        }
        if (LICENCE == null) {
            // Only an account that holds licence keys needs one to
            // redeem, because the licence key is what keeps redemption to
            // the acting party's own servers. An account holding none has
            // nothing to check against, so the demo runs without it.
            // Saying so here means an account that DOES hold licence
            // keys, run without one, is diagnosed at start-up rather than
            // by an unreadable verdict three steps later that looks like
            // a cryptographic failure.
            System.out.println("No _51DEGREES_LICENSE_KEY set. Redemption "
                + "will work where the account holds no licence keys, and "
                + "will report the context unreadable where it holds "
                + "some.");
        }
        // The client reads FOD_CLOUD_API_URL itself when no endpoint is
        // given, and falls back to the public cloud. A host other than
        // cloud.51degrees.com would be used to (a) use an on premise web
        // server, or (b) use a privately hosted version of the 51Degrees
        // cloud for performance reasons, which is the private hosting
        // option of the cloud service. Both run the same service, so the
        // example works unchanged.
        client = new DidClient(RESOURCE, LICENCE);
        // Read once here only to fail fast when either resource is
        // missing from the classpath. The stylesheet is the design system
        // build, vendored beside the page exactly as the other 51Degrees
        // web examples vendor it.
        resource("page.html");
        resource("examples-main.min.css");
        int port = Integer.parseInt(envOr("PORT", "5100"));
        HttpServer server = HttpServer.create(
            new InetSocketAddress(port), 0);
        server.createContext("/", CreatorContextDemoServer::servePage);
        server.createContext("/examples-main.min.css", exchange -> {
            byte[] css = resource("examples-main.min.css");
            exchange.getResponseHeaders().set("Content-Type", "text/css");
            exchange.sendResponseHeaders(200, css.length);
            exchange.getResponseBody().write(css);
            exchange.close();
        });
        server.createContext("/redeem", CreatorContextDemoServer::redeem);
        server.start();
        System.out.println("51Did demo on http://localhost:" + port + "/");
    }

    static void servePage(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/")) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        String page = new String(
            resource("page.html"), StandardCharsets.UTF_8);
        byte[] body = page
            .replace("__RESOURCE__", RESOURCE)
            .replace("__CHALLENGE__", hex(random))
            .replace("__API__", client.getEndpoint())
            .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    static void redeem(HttpExchange exchange) throws IOException {
        Map<String, String> query = parse(
            exchange.getRequestURI().getRawQuery());
        Answer answer = redeem(
            client,
            decode(valueOr(query, "51did")),
            decode(valueOr(query, "result")),
            decode(valueOr(query, "challenge")));
        exchange.getResponseHeaders().set("Content-Type", answer.type);
        exchange.sendResponseHeaders(answer.status, answer.body.length);
        exchange.getResponseBody().write(answer.body);
        exchange.close();
    }

    /**
     * The server-side step, and the lines a developer copies into their
     * own server. The 51Did arrives from the page in the URL-safe base64
     * alphabet, which {@link FodId#fromBase64(String)} accepts. The
     * signature is checked offline first, against the cloud's published
     * key for the identifier's date, then the encrypted result is redeemed
     * with the licence key, which is added by the client here and only
     * here, so the browser never sees it.
     * <p>
     * The page is answered in the cloud's own shape ({@code signature},
     * {@code context}, {@code factors} when present, {@code verifiedAt},
     * {@code secondsSinceVerified}) with one field added,
     * {@code serverSignature}, being this server's own offline check. The
     * page ignores fields it does not know.
     */
    static Answer redeem(
            DidClient client, String did, String result, String challenge) {
        FodId fodId;
        try {
            fodId = FodId.fromBase64(did);
        } catch (OwidException notA51Did) {
            return Answer.json(400, errors(
                "'" + did + "' is not a valid Base64-encoded 51Did."));
        } catch (IllegalArgumentException notA51Did) {
            return Answer.json(400, errors(
                "'" + did + "' is not a valid Base64-encoded 51Did."));
        }
        try {
            String serverSignature = client.verifySignature(fodId)
                ? "verified" : "invalid";
            RedeemResult redeemed = client.redeem(fodId, result, challenge);
            return Answer.json(
                redeemed.getStatusCode(), toJson(redeemed, serverSignature));
        } catch (DidNotSupportedException unsupported) {
            // The host does not offer the creator context. The same status
            // and a text body, which the page reports as not supported by
            // this host.
            return Answer.text(404, unsupported.getBody());
        } catch (IllegalArgumentException malformed) {
            return Answer.json(400, errors(malformed.getMessage()));
        } catch (DidHttpException other) {
            // Relayed as received, so the page sees what the cloud said.
            return Answer.text(other.getStatusCode(), other.getBody());
        } catch (IOException unreachable) {
            return Answer.json(502, new JSONObject()
                .put("error", String.valueOf(unreachable.getMessage())));
        }
    }

    /** The cloud's own shape, plus {@code serverSignature}. */
    static JSONObject toJson(RedeemResult redeemed, String serverSignature) {
        JSONObject json = new JSONObject();
        if (redeemed.getSignature() != RedeemResult.Signature.UNKNOWN) {
            json.put("signature",
                redeemed.getSignature() == RedeemResult.Signature.VERIFIED
                    ? "verified" : "invalid");
        }
        json.put("context", redeemed.getContextValue());
        if (redeemed.hasFactors()) {
            JSONObject factors = new JSONObject();
            for (Map.Entry<String, RedeemResult.Factor> factor
                    : redeemed.getFactors().entrySet()) {
                factors.put(factor.getKey(),
                    factor.getValue() == RedeemResult.Factor.VERIFIED
                        ? "verified" : "mismatch");
            }
            json.put("factors", factors);
        }
        if (redeemed.getVerifiedAt() != null) {
            json.put("verifiedAt", DateTimeFormatter.ISO_INSTANT
                .format(redeemed.getVerifiedAt()));
        }
        if (redeemed.getSecondsSinceVerified() != null) {
            json.put("secondsSinceVerified",
                redeemed.getSecondsSinceVerified().intValue());
        }
        json.put("serverSignature", serverSignature);
        return json;
    }

    static JSONObject errors(String message) {
        return new JSONObject().put("errors", new JSONArray().put(message));
    }

    /** What the route answers the page with. */
    static final class Answer {

        final int status;
        final String type;
        final byte[] body;

        private Answer(int status, String type, byte[] body) {
            this.status = status;
            this.type = type;
            this.body = body;
        }

        static Answer json(int status, JSONObject body) {
            return new Answer(status, "application/json",
                body.toString().getBytes(StandardCharsets.UTF_8));
        }

        static Answer text(int status, String body) {
            return new Answer(status, "text/plain; charset=utf-8",
                body.getBytes(StandardCharsets.UTF_8));
        }

        String bodyText() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    /**
     * A page or stylesheet from the classpath. Read per request rather
     * than once at start-up, so a demo left running while its page is
     * rebuilt serves the new copy rather than the version it started
     * with, which would look exactly like an edit that did not work.
     * The cost is one small read per request, which is nothing at demo
     * scale.
     */
    static byte[] resource(String name) throws IOException {
        InputStream stream = CreatorContextDemoServer.class
            .getResourceAsStream(RESOURCES + name);
        if (stream == null) {
            throw new IOException("Resource " + RESOURCES + name
                + " is missing from the classpath.");
        }
        return readAll(stream);
    }

    /** Reads a stream to its end and closes it. Null reads as empty. */
    static byte[] readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return new byte[0];
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            stream.close();
        }
    }

    /** Splits a raw query string, keeping values percent-encoded. */
    static Map<String, String> parse(String rawQuery) {
        Map<String, String> query = new HashMap<>();
        if (rawQuery == null) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            int at = pair.indexOf('=');
            if (at > 0) {
                query.put(pair.substring(0, at), pair.substring(at + 1));
            }
        }
        return query;
    }

    static String valueOr(Map<String, String> query, String name) {
        String value = query.get(name);
        return value == null ? "" : value;
    }

    /** Lower case hex of the bytes, two characters each. */
    static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b & 0xFF));
        }
        return builder.toString();
    }

    static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            // UTF-8 is part of every Java runtime. The checked exception
            // is a formality of the Java 8 signature.
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * The first of the named environment variables that is set to a
     * non-blank value, or null when none is.
     */
    static String env(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && value.trim().isEmpty() == false) {
                return value;
            }
        }
        return null;
    }

    static String envOr(String name, String fallback) {
        String value = env(name);
        return value == null ? fallback : value;
    }
}
