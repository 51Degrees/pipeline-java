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
import com.swancommunity.owid.OwidSignatureStatus;
import com.swancommunity.owid.OwidVerificationResult;
import com.swancommunity.owid.Version;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The client for everything a server does with a 51Did against the
 * 51Degrees cloud, so that server code never hand-writes HTTP or key
 * handling:
 * <ol>
 * <li>{@link #publicKeys()} and {@link #publicKeyFor(FodId)} fetch the
 * cloud's published signing keys once, keep them, and pick the key in force
 * when a given 51Did was created.</li>
 * <li>{@link #verifySignature(FodId)} checks a 51Did's signature offline
 * against that key.</li>
 * <li>{@link #verify(FodId)} checks a 51Did's signature through the cloud's
 * verify endpoint.</li>
 * <li>{@link #redeem(FodId, String, String)} redeems a sealed creator
 * context result, with the licence key, and returns a typed
 * {@link RedeemResult}.</li>
 * </ol>
 * Every one of those may reach the cloud, so every one of them returns at
 * once with a {@link CompletableFuture} and never blocks the calling
 * thread. Nothing the client refuses is thrown either, so that a caller has
 * one place to look, and a failure arrives as the cause of a
 * {@link CompletionException} through {@code join()},
 * {@code exceptionally}, {@code handle} or {@code whenComplete}, and as the
 * cause of an {@code ExecutionException} through {@code get()}. The methods
 * that answer from what the client already holds, {@link #getResourceKey()},
 * {@link #getEndpoint()} and {@link #hasLicenceKey()}, answer directly.
 * <p>
 * Creating a 51Did is not part of this client. Creation is the cloud
 * {@code json} endpoint through the cloud request engine and pipeline, and
 * a page creates from the browser because the identifier describes the
 * browser's own connection. The {@code verify-context} and
 * {@code verify-full} endpoints are browser calls for the same reason.
 * <p>
 * Credentials never appear in a query string, because a query string is
 * written to access logs. The resource key travels in the route of the GET
 * calls ({@code id/key/{resource}}, {@code id/verify/{resource}}) and in
 * the form body of the POST to {@code id/redeem}, whose route has no
 * resource segment, and the licence key travels only in that form body.
 * <p>
 * One instance is safe to share across threads. The key list is kept per
 * instance, so share the instance rather than creating one per request.
 * Callers that arrive while the key list is being fetched wait on that one
 * fetch rather than starting another.
 */
public final class DidClient {

    /** The public cloud's API base, used when no other is given. */
    public static final String DEFAULT_ENDPOINT =
        "https://cloud.51degrees.com/api/v4/";

    /**
     * The environment variable read for the API base when the constructor
     * is given none, the same one the cloud request engine honours.
     */
    public static final String ENDPOINT_VARIABLE = "FOD_CLOUD_API_URL";

    /**
     * How old the held key list may be before a request for a key refetches
     * it.
     */
    public static final Duration KEY_LIST_MAX_AGE = Duration.ofDays(1);

    static final Duration BOUNDARY_TOLERANCE = Duration.ofMinutes(15);

    private static final String USER_AGENT = "pipeline.did/" + version();

    /**
     * Longest encoded identifier this client will take from a caller. The
     * figure is arbitrary and deliberately generous, far above anything the
     * cloud issues, because its only job is to turn away obviously
     * malformed input before the client decodes it, fetches a key or calls
     * the cloud. It says nothing about how long a 51Did is, and the cloud
     * remains the judge of that.
     */
    private static final int MAXIMUM_ENCODED_LENGTH = 4096;

    /** The outcome of an offline signature check, in detail. */
    public enum SignatureCheck {
        /** The signature verifies with the key in force at its date. */
        VERIFIED,
        /** No candidate key verifies the signature. */
        INVALID,
        /** The identifier's date precedes every key the cloud publishes. */
        NO_KEY_COVERS_DATE,
        /** The envelope version is not 3. */
        UNSUPPORTED_VERSION,
        /** The payload is shorter than the base length for its type. */
        MALFORMED_PAYLOAD,
    }

    private final String resourceKey;
    private final String licenceKey;
    private final String endpoint;
    private final HttpTransport transport;
    private final Clock clock;

    private final Object lock = new Object();
    private List<SigningKey> keys;
    private Instant keysFetchedAt;
    /** The key list fetch under way, shared by every caller waiting. */
    private CompletableFuture<List<SigningKey>> inFlight;

    /**
     * A client for the public cloud, or the host named by
     * {@value #ENDPOINT_VARIABLE}, with no licence key.
     *
     * @param resourceKey the page's resource key, public by nature
     */
    public DidClient(String resourceKey) {
        this(builder(resourceKey));
    }

    /**
     * A client for the public cloud, or the host named by
     * {@value #ENDPOINT_VARIABLE}, with a licence key.
     *
     * @param resourceKey the page's resource key, public by nature
     * @param licenceKey  a licence key of the same account, server side
     *                    only, needed to redeem where the account holds
     *                    licence keys, or null
     */
    public DidClient(String resourceKey, String licenceKey) {
        this(builder(resourceKey).licenceKey(licenceKey));
    }

    /**
     * A client for the given host.
     *
     * @param resourceKey the page's resource key, public by nature
     * @param licenceKey  a licence key of the same account, or null
     * @param endpoint    the API base including {@code /api/v4/}, or null
     *                    to read {@value #ENDPOINT_VARIABLE} and fall back
     *                    to {@link #DEFAULT_ENDPOINT}. A trailing slash is
     *                    added where missing.
     */
    public DidClient(String resourceKey, String licenceKey, String endpoint) {
        this(builder(resourceKey).licenceKey(licenceKey).endpoint(endpoint));
    }

    private DidClient(Builder builder) {
        this.resourceKey = builder.resourceKey;
        this.licenceKey = blankToNull(builder.licenceKey);
        this.endpoint = resolveEndpoint(builder.endpoint);
        this.transport = builder.transport == null
            ? new UrlConnectionTransport(builder.executor == null
                ? SharedPool.INSTANCE
                : builder.executor)
            : builder.transport;
        this.clock = builder.clock == null ? Clock.systemUTC() : builder.clock;
    }

    /**
     * Starts a builder for the cases the constructors do not cover, being
     * an HTTP transport of the caller's own, the executor the default
     * transport runs on or, in tests, a clock.
     *
     * @param resourceKey the page's resource key, public by nature
     * @return the builder
     */
    public static Builder builder(String resourceKey) {
        return new Builder(resourceKey);
    }

    /** Builds a {@link DidClient}. */
    public static final class Builder {

        private final String resourceKey;
        private String licenceKey;
        private String endpoint;
        private HttpTransport transport;
        private Executor executor;
        private Clock clock;

        private Builder(String resourceKey) {
            if (resourceKey == null || resourceKey.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "A resource key is required.");
            }
            this.resourceKey = resourceKey.trim();
        }

        /**
         * @param licenceKey a licence key of the account, or null
         * @return this builder
         */
        public Builder licenceKey(String licenceKey) {
            this.licenceKey = licenceKey;
            return this;
        }

        /**
         * @param endpoint the API base including {@code /api/v4/}, or null
         *                 to read {@value #ENDPOINT_VARIABLE} and fall back
         *                 to {@link #DEFAULT_ENDPOINT}
         * @return this builder
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * @param transport the HTTP transport to send through, or null for
         *                  the default {@link java.net.HttpURLConnection}
         *                  one
         * @return this builder
         */
        public Builder transport(HttpTransport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * The executor the default transport runs its blocking exchanges
         * on. It does nothing when a transport of your own is given,
         * because that transport schedules its own work, so give one or
         * the other and not both.
         *
         * @param executor the executor to run blocking exchanges on, or
         *                 null for a small shared pool of daemon threads
         * @return this builder
         */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * @param clock the clock the key list's age is measured by, or null
         *              for the system clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /** @return the client */
        public DidClient build() {
            return new DidClient(this);
        }
    }

    /** @return the resource key the client sends */
    public String getResourceKey() {
        return resourceKey;
    }

    /** @return the API base every request is made under, ending in a slash */
    public String getEndpoint() {
        return endpoint;
    }

    /** @return whether the client was given a licence key */
    public boolean hasLicenceKey() {
        return licenceKey != null;
    }

    // ----- Public keys -----

    /**
     * The cloud's published signing keys, fetched on first use and then
     * held, in order of start. Keys are published ahead of their start, so
     * the list normally reaches months into the future. A fetch already
     * under way is shared rather than repeated.
     *
     * @return the keys, read only, or a future failed with
     *         {@link IOException} where the list is not held and cannot be
     *         fetched
     */
    public CompletableFuture<List<SigningKey>> publicKeys() {
        synchronized (lock) {
            if (keys == null) {
                return fetchKeys();
            }
            return CompletableFuture.completedFuture(keys);
        }
    }

    /**
     * The key in force when the identifier was created, being the entry
     * whose start is latest on or before the identifier's date. A held list
     * is refetched, once, before answering when it has no entry on or
     * before the date, when the date is later than the newest start held,
     * or when the list is more than {@link #KEY_LIST_MAX_AGE} old.
     * Otherwise the answer comes from the held list. A list fetched for
     * this very call is not fetched again, because it cannot get better.
     *
     * @param fodId the identifier
     * @return the key in force, or null where the date precedes every key,
     *         or a future failed with {@link IOException} where the
     *         required key list cannot be fetched
     */
    public CompletableFuture<SigningKey> publicKeyFor(FodId fodId) {
        try {
            Objects.requireNonNull(fodId, "fodId");
            Instant date = fodId.getDate();
            return keysFor(date).thenApply(held -> inForceAt(held, date));
        } catch (RuntimeException refused) {
            return failed(refused);
        }
    }

    /**
     * Fetches the key list, records when it was fetched, and hands every
     * caller that arrives while it is under way the same future, so one
     * fetch answers them all. A failure leaves whatever was held in place.
     * Called with the lock held.
     */
    private CompletableFuture<List<SigningKey>> fetchKeys() {
        if (inFlight != null) {
            return inFlight;
        }
        CompletableFuture<List<SigningKey>> promise =
            new CompletableFuture<List<SigningKey>>();
        inFlight = promise;
        String url = endpoint + "id/key/" + encode(resourceKey);
        send("GET", url, null).whenComplete((response, failure) -> {
            List<SigningKey> fetched = null;
            Throwable error = unwrap(failure);
            if (error == null) {
                try {
                    fetched = readKeys(response);
                } catch (DidHttpException unreadable) {
                    error = unreadable;
                }
            }
            synchronized (lock) {
                inFlight = null;
                if (error == null) {
                    keys = fetched;
                    keysFetchedAt = clock.instant();
                }
            }
            // Completed only after the held list is in place, so a caller
            // waiting on this future never sees the client mid-update.
            if (error == null) {
                promise.complete(fetched);
            } else {
                promise.completeExceptionally(new CompletionException(error));
            }
        });
        return promise;
    }

    /** The key list a key endpoint answer carries. */
    private static List<SigningKey> readKeys(HttpTransport.Response response)
            throws DidHttpException {
        if (response.getStatusCode() != 200) {
            throw httpError("The public key list", response);
        }
        try {
            return parseKeys(response.getBody());
        } catch (JSONException unreadable) {
            throw new DidHttpException(
                "The public key list could not be read: "
                + unreadable.getMessage(),
                response.getStatusCode(), response.getBody());
        } catch (DateTimeException unreadable) {
            throw new DidHttpException(
                "The public key list could not be read: "
                + unreadable.getMessage(),
                response.getStatusCode(), response.getBody());
        }
    }

    /**
     * Reads the key list the cloud answers with. Each entry carries
     * {@code startsAt} and {@code publicKey}. Where {@code startsAt} is
     * absent, the compatibility field {@code created} is read instead.
     * {@code weekStart} is ignored.
     */
    static List<SigningKey> parseKeys(String body) {
        JSONArray array = new JSONArray(body);
        List<SigningKey> parsed = new ArrayList<SigningKey>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.getJSONObject(i);
            String startsAt = entry.optString("startsAt", null);
            if (startsAt == null) {
                startsAt = entry.optString("created", null);
            }
            String publicKey = entry.optString("publicKey", null);
            if (startsAt == null || publicKey == null) {
                throw new JSONException(
                    "entry " + i + " has no start or no publicKey");
            }
            parsed.add(new SigningKey(parseInstant(startsAt), publicKey));
        }
        Collections.sort(parsed, new Comparator<SigningKey>() {
            @Override
            public int compare(SigningKey a, SigningKey b) {
                return a.getStartsAt().compareTo(b.getStartsAt());
            }
        });
        return Collections.unmodifiableList(parsed);
    }

    /** An ISO 8601 date and time with a zone, {@code Z} or an offset. */
    private static Instant parseInstant(String value) {
        return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(value));
    }

    /**
     * The key list to answer a question about the given date from,
     * refetching once first where the held list may not have the answer. A
     * failed fetch fails the future, because the held list may not contain
     * the key needed for that date.
     */
    private CompletableFuture<List<SigningKey>> keysFor(Instant date) {
        synchronized (lock) {
            if (keys == null || needsRefetch(date)) {
                return fetchKeys();
            }
            return CompletableFuture.completedFuture(keys);
        }
    }

    /** Called under the lock with a list held. */
    private boolean needsRefetch(Instant date) {
        boolean stale = Duration.between(keysFetchedAt, clock.instant())
            .compareTo(KEY_LIST_MAX_AGE) > 0;
        boolean uncovered = inForceAt(keys, date) == null;
        boolean beyond = keys.isEmpty() == false
            && date.isAfter(keys.get(keys.size() - 1).getStartsAt());
        return stale || uncovered || beyond;
    }

    /**
     * The entry in force at the moment, being the newest whose start has
     * passed, or null when the moment precedes every entry. Because an entry
     * is in force until the next one starts, this can only be null before
     * the schedule begins.
     */
    static SigningKey inForceAt(List<SigningKey> entries, Instant at) {
        SigningKey best = null;
        for (SigningKey entry : entries) {
            if (entry.getStartsAt().isAfter(at)) {
                continue;
            }
            if (best == null || entry.getStartsAt().isAfter(best.getStartsAt())) {
                best = entry;
            }
        }
        return best;
    }

    /**
     * The entries that may have signed something created at the moment,
     * best first, being the entry in force and then the neighbouring entry
     * either side of a nearby key boundary where those differ.
     * Deliberately not every earlier entry, and not a rule to relax here,
     * because the cloud applies the same one.
     */
    static List<SigningKey> candidatesFor(
            List<SigningKey> entries, Instant at) {
        List<SigningKey> candidates = new ArrayList<SigningKey>(3);
        addIfNew(candidates, inForceAt(entries, at));
        addIfNew(candidates, inForceAt(entries, at.minus(BOUNDARY_TOLERANCE)));
        addIfNew(candidates, inForceAt(entries, at.plus(BOUNDARY_TOLERANCE)));
        return candidates;
    }

    private static void addIfNew(List<SigningKey> candidates, SigningKey entry) {
        if (entry == null) {
            return;
        }
        for (SigningKey held : candidates) {
            if (held == entry) {
                return;
            }
        }
        candidates.add(entry);
    }

    // ----- Offline signature verification -----

    /**
     * Whether the identifier's signature verifies offline against the
     * cloud's published key for its date. See
     * {@link #verifySignatureDetailed(FodId)} for why not.
     *
     * @param fodId the identifier
     * @return true where the signature verifies, or a future failed with
     *         {@link IOException} where the required key list cannot be
     *         fetched
     */
    public CompletableFuture<Boolean> verifySignature(FodId fodId) {
        return verifySignatureDetailed(fodId)
            .thenApply(check -> check == SignatureCheck.VERIFIED);
    }

    /**
     * Checks the identifier's signature offline, mirroring the cloud's own
     * verify endpoint: the envelope version must be 3, the payload must be
     * at least the base length for its type (a longer payload carries a
     * creator context section and is accepted), and the signature must
     * verify with the key in force at the identifier's date or, within a
     * short tolerance either side of a key boundary, the neighbouring key.
     *
     * @param fodId the identifier
     * @return the outcome, or a future failed with {@link IOException}
     *         where the required key list cannot be fetched
     */
    public CompletableFuture<SignatureCheck> verifySignatureDetailed(
            FodId fodId) {
        try {
            Objects.requireNonNull(fodId, "fodId");
            if (fodId.getVersion() != Version.VERSION3) {
                return CompletableFuture.completedFuture(
                    SignatureCheck.UNSUPPORTED_VERSION);
            }
            boolean isRandom = fodId.getType() == IdType.RANDOM;
            int baseLength = FodId.HEADER_LENGTH
                + (isRandom ? FodId.GUID_LENGTH : FodId.MATCH_KEY_LENGTH);
            if (fodId.getPayload().length < baseLength) {
                return CompletableFuture.completedFuture(
                    SignatureCheck.MALFORMED_PAYLOAD);
            }
            Instant date = fodId.getDate();
            return keysFor(date)
                .thenApply(held -> checkSignature(fodId, held, date));
        } catch (RuntimeException refused) {
            return failed(refused);
        }
    }

    /** The offline check against the candidate keys for the date. */
    private static SignatureCheck checkSignature(
            FodId fodId, List<SigningKey> held, Instant date) {
        List<SigningKey> candidates = candidatesFor(held, date);
        if (candidates.isEmpty()) {
            return SignatureCheck.NO_KEY_COVERS_DATE;
        }
        for (SigningKey candidate : candidates) {
            OwidVerificationResult check =
                fodId.verifyDetailed(candidate.getPublicKeyPem());
            if (check.getStatus() == OwidSignatureStatus.SIGNATURE_VALID) {
                return SignatureCheck.VERIFIED;
            }
            // Any other answer, whether the signature does not match this
            // key or the key itself cannot be read, leaves the next
            // candidate to try. Only when none verifies is the signature
            // reported invalid.
        }
        return SignatureCheck.INVALID;
    }

    // ----- Cloud signature verification -----

    /**
     * Whether the identifier's signature verifies according to the cloud's
     * verify endpoint, the open endpoint that needs no licence key. One use
     * against the resource key.
     *
     * @param fodId the identifier
     * @return true where the cloud answers valid, or a future failed with
     *         {@link IOException} where the cloud cannot be reached or
     *         answers with a status the client does not map
     */
    public CompletableFuture<Boolean> verify(FodId fodId) {
        try {
            Objects.requireNonNull(fodId, "fodId");
            return verify(base64Url(fodId));
        } catch (RuntimeException refused) {
            return failed(refused);
        }
    }

    /**
     * Whether the identifier's signature verifies according to the cloud's
     * verify endpoint. The identifier may be in either base64 alphabet. It
     * is sent under both parameter names the endpoint accepts, {@code 51did}
     * and {@code owid}, so a cloud that reads only the older name answers.
     * <p>
     * The future fails with {@link IllegalArgumentException} where the
     * value is too long to be an identifier at all, where it does not read
     * as a 51Did, with the {@link FodIdParseStatus} in the message, or
     * where the cloud says it is not a 51Did, with the cloud's message. It
     * fails with {@link IOException} where the cloud cannot be reached or
     * answers with a status the client does not map.
     *
     * @param fodId the identifier as base64
     * @return true where the cloud answers valid, false where it answers
     *         invalid
     */
    public CompletableFuture<Boolean> verify(String fodId) {
        try {
            Objects.requireNonNull(fodId, "fodId");
            ensureEncodedLength(fodId);
            ensureReadsAs51Did(fodId);
            // Under both names so the request works with hosts that read
            // either parameter. Hosts that recognise both prefer 51did and
            // keep owid as a compatibility alias.
            String encoded = encode(fodId);
            String url = endpoint + "id/verify/" + encode(resourceKey)
                + "?51did=" + encoded + "&owid=" + encoded;
            return send("GET", url, null).thenApply(DidClient::readVerify);
        } catch (RuntimeException refused) {
            return failed(refused);
        }
    }

    /** The verdict a verify endpoint answer carries. */
    private static boolean readVerify(HttpTransport.Response response) {
        JSONObject json = asObject(response.getBody());
        int status = response.getStatusCode();
        if (status == 200 && json != null && json.has("valid")) {
            return json.getBoolean("valid");
        }
        if (status == 400 && json != null) {
            if (json.has("valid")) {
                return json.getBoolean("valid");
            }
            if (json.has("errors")) {
                throw new IllegalArgumentException(errorsText(json));
            }
        }
        throw new CompletionException(
            httpError("Signature verification", response));
    }

    // ----- Redeem -----

    /**
     * Redeems a sealed creator context result against the identifier the
     * caller knows independently, sending the licence key where one was
     * given. One use against the resource key, the second of the two a
     * browser-based context check costs.
     * <p>
     * The future fails with {@link IllegalArgumentException} where the
     * cloud says the identifier is not a 51Did, with the cloud's message,
     * with {@link DidNotSupportedException} where the host does not offer
     * the creator context, with {@link DidHttpException} where the cloud
     * answers with any other status or a body that is not its own shape,
     * and with {@link IOException} where the cloud cannot be reached.
     *
     * @param fodId     the identifier the sealed result was made for
     * @param result    the sealed result exactly as the verify endpoint
     *                  returned it
     * @param challenge the single-use challenge given to the verify
     *                  endpoint, or null where none was
     * @return the typed result, for a 200 or a 503 answer
     */
    public CompletableFuture<RedeemResult> redeem(
            FodId fodId, String result, String challenge) {
        try {
            Objects.requireNonNull(fodId, "fodId");
            return redeem(base64(fodId), result, challenge);
        } catch (RuntimeException refused) {
            return failed(refused);
        }
    }

    /**
     * Redeems a sealed creator context result. The identifier may be in
     * either base64 alphabet. See {@link #redeem(FodId, String, String)}.
     * The future fails with {@link IllegalArgumentException} where the
     * value is too long to be an identifier at all or does not read as a
     * 51Did, with the {@link FodIdParseStatus} in the message, and
     * otherwise as {@link #redeem(FodId, String, String)}.
     *
     * @param fodId     the identifier as base64
     * @param result    the sealed result
     * @param challenge the challenge, or null
     * @return the typed result, for a 200 or a 503 answer
     */
    public CompletableFuture<RedeemResult> redeem(
            String fodId, String result, String challenge) {
        try {
            Objects.requireNonNull(fodId, "fodId");
            ensureEncodedLength(fodId);
            ensureReadsAs51Did(fodId);
            // The POST route has no {resource} segment, so the resource key
            // goes in the form with everything else.
            StringBuilder form = new StringBuilder()
                .append("resource=").append(encode(resourceKey))
                .append("&51did=").append(encode(fodId))
                .append("&result=").append(encode(nullToEmpty(result)))
                .append("&challenge=").append(encode(nullToEmpty(challenge)));
            if (licenceKey != null) {
                form.append("&license=").append(encode(licenceKey));
            }
            String url = endpoint + "id/redeem";
            return send("POST", url,
                form.toString().getBytes(StandardCharsets.UTF_8))
                .thenApply(this::readRedeem);
        } catch (RuntimeException refused) {
            return failed(refused);
        }
    }

    /** The typed result a redeem answer carries. */
    private RedeemResult readRedeem(HttpTransport.Response response) {
        int status = response.getStatusCode();
        JSONObject json = asObject(response.getBody());
        switch (status) {
            case 200:
            case 503:
                if (json == null) {
                    throw new CompletionException(
                        httpError("Redemption", response));
                }
                return RedeemResult.parse(status, json, response.getBody());
            case 400:
                if (json != null && json.has("errors")) {
                    throw new IllegalArgumentException(errorsText(json));
                }
                throw new CompletionException(
                    httpError("Redemption", response));
            case 404:
                throw new CompletionException(new DidNotSupportedException(
                    endpoint, response.getBody()));
            default:
                throw new CompletionException(
                    httpError("Redemption", response));
        }
    }

    // ----- HTTP -----

    private CompletableFuture<HttpTransport.Response> send(
            String method, String url, byte[] body) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", "application/json");
        if (body != null) {
            headers.put("Content-Type",
                "application/x-www-form-urlencoded; charset=utf-8");
        }
        try {
            CompletableFuture<HttpTransport.Response> sent = transport.send(
                new HttpTransport.Request(method, url, headers, body));
            if (sent == null) {
                return failed(new IOException(
                    "The transport answered no future for " + url + "."));
            }
            return sent;
        } catch (RuntimeException broken) {
            // A transport that throws where it should have failed its
            // future is still reported through the future, so a caller has
            // one place to look.
            return failed(broken);
        }
    }

    /**
     * A future already failed with the given cause, wrapped so that every
     * failure the client reports arrives the same way, as the cause of a
     * {@link CompletionException}.
     */
    private static <T> CompletableFuture<T> failed(Throwable cause) {
        CompletableFuture<T> answer = new CompletableFuture<T>();
        answer.completeExceptionally(cause instanceof CompletionException
            ? cause
            : new CompletionException(cause));
        return answer;
    }

    /** The real failure behind a {@link CompletionException}, or null. */
    private static Throwable unwrap(Throwable failure) {
        if (failure instanceof CompletionException
                && failure.getCause() != null) {
            return failure.getCause();
        }
        return failure;
    }

    /**
     * Refuses input that cannot be an identifier at all, before any work is
     * done on it. See {@link #MAXIMUM_ENCODED_LENGTH}.
     */
    private static void ensureEncodedLength(String fodId) {
        if (fodId.length() > MAXIMUM_ENCODED_LENGTH) {
            throw new IllegalArgumentException(
                "The value is too long to be a 51Did.");
        }
    }

    /**
     * Refuses a value that does not read as a 51Did, before any key is
     * fetched or the cloud is called, so malformed input costs no network
     * round trip and no use. The value is still sent to the cloud exactly
     * as the caller gave it. The signature is not checked here, because the
     * signature is the question the cloud is about to be asked.
     */
    private static void ensureReadsAs51Did(String fodId) {
        FodIdParseResult read = FodId.tryFromBase64(fodId);
        if (read.isSuccess() == false) {
            throw new IllegalArgumentException(
                "The value does not read as a 51Did: " + read.getStatus()
                + ".");
        }
    }

    private static DidHttpException httpError(
            String what, HttpTransport.Response response) {
        String body = response.getBody();
        String summary = body.length() > 200 ? body.substring(0, 200) : body;
        return new DidHttpException(
            what + " answered HTTP " + response.getStatusCode() + ": "
            + summary, response.getStatusCode(), body);
    }

    /** The body as a JSON object, or null when it is not one. */
    private static JSONObject asObject(String body) {
        try {
            return new JSONObject(body);
        } catch (JSONException notAnObject) {
            return null;
        }
    }

    /** The cloud's {@code errors} array as one message. */
    private static String errorsText(JSONObject json) {
        JSONArray errors = json.optJSONArray("errors");
        if (errors == null) {
            return json.toString();
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < errors.length(); i++) {
            if (i > 0) {
                text.append(' ');
            }
            text.append(errors.optString(i));
        }
        return text.toString();
    }

    private static String base64(FodId fodId) {
        try {
            return fodId.asBase64();
        } catch (OwidException impossible) {
            // A FodId is only ever built from a serialised envelope, so it
            // always serialises again.
            throw new IllegalStateException(impossible);
        }
    }

    private static String base64Url(FodId fodId) {
        try {
            return fodId.asBase64Url();
        } catch (OwidException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            // UTF-8 is part of every Java runtime. The checked exception
            // is a formality of the Java 8 signature.
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * The API base to use: the argument, else {@value #ENDPOINT_VARIABLE},
     * else {@link #DEFAULT_ENDPOINT}, ending in exactly one slash so every
     * route can be appended directly.
     */
    static String resolveEndpoint(String endpoint) {
        String value = blankToNull(endpoint);
        if (value == null) {
            value = blankToNull(System.getenv(ENDPOINT_VARIABLE));
        }
        if (value == null) {
            value = DEFAULT_ENDPOINT;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "/";
    }

    private static String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * The package version from the jar manifest, which the build writes,
     * or {@code dev} when running from class files.
     */
    private static String version() {
        Package pkg = DidClient.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        return version == null ? "dev" : version;
    }

    /**
     * The default transport, over {@link HttpURLConnection}. Java 8 has no
     * non-blocking HTTP client in its standard library, so this is blocking
     * I/O on a background thread, meaning the exchange runs on the executor
     * and completes the future from there. On Java 11 and later supply a
     * transport over {@code java.net.http.HttpClient.sendAsync} instead,
     * which needs no thread per request. Ten seconds to connect and ten to
     * read, which is generous for the cloud and short enough that a pooled
     * thread is not held for long by a host that is down.
     */
    static final class UrlConnectionTransport implements HttpTransport {

        private static final int TIMEOUT_MILLIS = 10_000;

        private final Executor executor;

        UrlConnectionTransport(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
        }

        @Override
        public CompletableFuture<Response> send(Request request) {
            CompletableFuture<Response> answer =
                new CompletableFuture<Response>();
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            answer.complete(exchange(request));
                        } catch (Throwable failure) {
                            // Anything at all, so that a caller waiting on
                            // this future is never left waiting for ever.
                            answer.completeExceptionally(failure);
                        }
                    }
                });
            } catch (RuntimeException refused) {
                // An executor that is shut down or full refuses the work.
                // That is a failure of the request like any other.
                answer.completeExceptionally(refused);
            }
            return answer;
        }

        /** The blocking exchange, run on the executor's thread. */
        private static Response exchange(Request request) throws IOException {
            HttpURLConnection connection = (HttpURLConnection)
                URI.create(request.getUrl()).toURL().openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestMethod(request.getMethod());
            for (Map.Entry<String, String> header
                    : request.getHeaders().entrySet()) {
                connection.setRequestProperty(
                    header.getKey(), header.getValue());
            }
            byte[] body = request.getBody();
            if (body != null) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                OutputStream out = connection.getOutputStream();
                try {
                    out.write(body);
                } finally {
                    out.close();
                }
            }
            int status = connection.getResponseCode();
            // A refusal is explained in the body, which HttpURLConnection
            // puts on the error stream, so read whichever stream the
            // status selected.
            InputStream stream = status >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
            return new Response(status, readAll(stream));
        }

        /** Reads a stream to its end as UTF-8 and closes it. */
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
    }

    /**
     * The threads the default transport blocks on where the builder is
     * given no executor, being a small bounded pool of daemon threads,
     * created as they are needed and retired when they have been idle a
     * while, so a program that has finished its work still exits. Held in a
     * class of its own so that no pool exists at all until a client is
     * built without a transport and without an executor.
     */
    private static final class SharedPool {

        private static final int MINIMUM_THREADS = 2;
        private static final int MAXIMUM_THREADS = 8;
        private static final long IDLE_SECONDS = 60L;

        static final Executor INSTANCE = create();

        private static Executor create() {
            int threads = Math.min(MAXIMUM_THREADS, Math.max(MINIMUM_THREADS,
                Runtime.getRuntime().availableProcessors()));
            ThreadPoolExecutor pool = new ThreadPoolExecutor(
                threads, threads, IDLE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {

                    private final AtomicInteger counter = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable work) {
                        Thread thread = new Thread(work,
                            "51did-http-" + counter.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                });
            // Without this the pool keeps its threads for the life of the
            // program, which a library has no business doing.
            pool.allowCoreThreadTimeOut(true);
            return pool;
        }
    }
}
