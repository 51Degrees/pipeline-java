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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The typed answer to {@link DidClient#redeem(FodId, String, String)}: the
 * creator context verdict, the signature outcome, the per-factor detail
 * where the cloud gave it, and when the verification behind the sealed
 * result happened.
 * <p>
 * Every cryptographic failure comes back from the cloud as the one word
 * {@code unreadable}, by design, so that a forger probing the endpoint
 * learns nothing about which part of a guess was wrong. This type does not
 * try to distinguish them either, and it maps any context value it does
 * not know to {@link Context#UNREADABLE} for the same reason, keeping the
 * raw value in {@link #getContextValue()}.
 */
public final class RedeemResult {

    /** The creator context verdict. */
    public enum Context {
        /** The identifier is being presented from where it was created. */
        VERIFIED("verified"),
        /**
         * At least one factor differs from creation. {@link #getFactors()}
         * says which.
         */
        MISMATCH("mismatch"),
        /** The identifier carries no creator context to check. */
        NO_CONTEXT("nocontext"),
        /**
         * No longer sent by the service, and kept only because it has been
         * public since this enum was added. What used to give this answer
         * now gives {@link #MISCONFIGURED} where the service is at fault, or
         * {@link #INVALID_DATE} where the identifier could not have been
         * created.
         */
        NOT_CHECKABLE("notcheckable"),
        /** The sealed result was redeemed outside the freshness window. */
        EXPIRED("expired"),
        /** The sealed result had already been redeemed. */
        REPLAYED("replayed"),
        /**
         * The sealed result could not be read: tampered, made for another
         * identifier, sealed under another challenge or licence key, or an
         * answer this client did not recognise.
         */
        UNREADABLE("unreadable"),
        /**
         * First use could not be confirmed (answered with 503). Not a
         * verdict, and the caller may retry.
         */
        UNCONFIRMED("unconfirmed"),
        /**
         * The service that checked the identifier could not complete the
         * check, and the reason is that service rather than the identifier.
         * It either compared nothing, or compared some factors and reports
         * at least one as {@link Factor#MISCONFIGURED}.
         *
         * <p>Nothing a caller sends can produce this. Against 51Degrees
         * public cloud it should not occur. Against a self-hosted service it
         * means that service is not reading the client's own connection, or
         * is missing an engine it needs, and its own logs name what to set.
         */
        MISCONFIGURED("misconfigured"),
        /**
         * The identifier's creation date is one the scheme could not have
         * produced, being in the future or before the creator context scheme
         * began. It says the identifier is fabricated rather than that
         * anything is wrong with the service.
         */
        INVALID_DATE("invaliddate");

        private final String value;

        Context(String value) {
            this.value = value;
        }

        /** @return the word the cloud uses for this verdict */
        public String getValue() {
            return value;
        }

        /**
         * Maps the cloud's word to a verdict. Anything unrecognised,
         * including a missing value, is {@link #UNREADABLE}, so an answer
         * this client does not know fails closed.
         *
         * @param value the {@code context} string from the cloud, or null
         * @return the matching verdict, or {@link #UNREADABLE}
         */
        public static Context fromValue(String value) {
            if (value != null) {
                for (Context context : values()) {
                    if (context.value.equals(value)) {
                        return context;
                    }
                }
            }
            return UNREADABLE;
        }
    }

    /** The signature outcome, reported by the cloud out of the seal. */
    public enum Signature {
        /** The identifier is a genuine 51Degrees identifier. */
        VERIFIED,
        /** The signature did not verify. */
        INVALID,
        /**
         * The cloud did not report the signature, which it only does on the
         * redeemed outcomes.
         */
        UNKNOWN;

        /**
         * Maps the cloud's word to an outcome. Absent is {@link #UNKNOWN},
         * {@code verified} is {@link #VERIFIED}, and any other word is
         * {@link #INVALID}, so a word this client does not know fails
         * closed.
         *
         * @param value the {@code signature} string from the cloud, or null
         * @return the matching outcome
         */
        public static Signature fromValue(String value) {
            if (value == null) {
                return UNKNOWN;
            }
            return "verified".equals(value) ? VERIFIED : INVALID;
        }
    }

    /** The outcome for one factor of the creator context. */
    public enum Factor {
        /** The factor matches creation. */
        VERIFIED,
        /** The factor differs from creation. */
        MISMATCH,
        /**
         * The service that checked the identifier is not configured to
         * determine this factor, so it could not have checked it for any
         * request. This is NOT a mismatch and must not be read as one, since
         * the identifier says nothing about it either way.
         */
        MISCONFIGURED;

        /**
         * Misconfigured is read on its own, because it is the one value that
         * must not fall through to a mismatch. Everything else that is not
         * the word {@code verified} is a mismatch, so an unexpected value
         * never reads as a pass.
         *
         * @param value the factor's string from the cloud
         * @return {@link #VERIFIED} for {@code verified},
         *         {@link #MISCONFIGURED} for {@code misconfigured},
         *         otherwise {@link #MISMATCH}
         */
        public static Factor fromValue(String value) {
            if ("verified".equals(value)) {
                return VERIFIED;
            }
            return "misconfigured".equals(value) ? MISCONFIGURED : MISMATCH;
        }
    }

    /**
     * The factor names in the order the cloud reports them. Kept so that
     * {@link #getFactors()} iterates in that order whatever order the JSON
     * parser hands the keys back in.
     */
    private static final String[] FACTOR_ORDER = {
        "transport", "device", "browserip", "connectionip", "asn", "browser",
    };

    private final Context context;
    private final String contextValue;
    private final Signature signature;
    private final Map<String, Factor> factors;
    private final boolean hasFactors;
    private final Instant verifiedAt;
    private final Integer secondsSinceVerified;
    private final int statusCode;
    private final String raw;

    private RedeemResult(
            Context context,
            String contextValue,
            Signature signature,
            Map<String, Factor> factors,
            boolean hasFactors,
            Instant verifiedAt,
            Integer secondsSinceVerified,
            int statusCode,
            String raw) {
        this.context = context;
        this.contextValue = contextValue;
        this.signature = signature;
        this.factors = factors;
        this.hasFactors = hasFactors;
        this.verifiedAt = verifiedAt;
        this.secondsSinceVerified = secondsSinceVerified;
        this.statusCode = statusCode;
        this.raw = raw;
    }

    /**
     * Builds a result from the cloud's answer.
     *
     * @param statusCode the HTTP status, 200 or 503
     * @param json       the body parsed as a JSON object
     * @param raw        the body as received
     * @return the typed result
     */
    static RedeemResult parse(int statusCode, JSONObject json, String raw) {
        String contextValue = json.optString("context", null);
        Context context = Context.fromValue(contextValue);
        Signature signature = Signature.fromValue(
            json.optString("signature", null));

        Map<String, Factor> factors = new LinkedHashMap<String, Factor>();
        JSONObject factorsJson = json.optJSONObject("factors");
        boolean hasFactors = factorsJson != null;
        if (hasFactors) {
            for (String name : FACTOR_ORDER) {
                if (factorsJson.has(name)) {
                    factors.put(name, Factor.fromValue(
                        factorsJson.optString(name, null)));
                }
            }
            for (String name : factorsJson.keySet()) {
                if (factors.containsKey(name) == false) {
                    factors.put(name, Factor.fromValue(
                        factorsJson.optString(name, null)));
                }
            }
        }

        Instant verifiedAt = null;
        String verifiedAtValue = json.optString("verifiedAt", null);
        if (verifiedAtValue != null) {
            try {
                verifiedAt = Instant.parse(verifiedAtValue);
            } catch (DateTimeParseException unreadable) {
                verifiedAt = null;
            }
        }
        Integer secondsSinceVerified = null;
        if (json.has("secondsSinceVerified")
                && json.isNull("secondsSinceVerified") == false) {
            secondsSinceVerified = json.optInt("secondsSinceVerified");
        }

        return new RedeemResult(
            context,
            contextValue == null ? context.getValue() : contextValue,
            signature,
            Collections.unmodifiableMap(factors),
            hasFactors,
            verifiedAt,
            secondsSinceVerified,
            statusCode,
            raw);
    }

    /** @return the creator context verdict */
    public Context getContext() {
        return context;
    }

    /**
     * @return the {@code context} word exactly as the cloud sent it, so an
     *         unrecognised word (mapped to {@link Context#UNREADABLE}) is
     *         still visible; the verdict's own word when the cloud sent none
     */
    public String getContextValue() {
        return contextValue;
    }

    /** @return the signature outcome */
    public Signature getSignature() {
        return signature;
    }

    /**
     * The per-factor outcomes, present only when the cloud sent
     * {@code factors}, which it does on the mismatch verdict, the one with
     * something to diagnose. Names are {@code transport}, {@code device},
     * {@code browserip}, {@code connectionip}, {@code asn} and
     * {@code browser}.
     *
     * @return factor name to outcome, read only, empty when the cloud sent
     *         none (see {@link #hasFactors()})
     */
    public Map<String, Factor> getFactors() {
        return factors;
    }

    /** @return whether the cloud sent {@code factors} */
    public boolean hasFactors() {
        return hasFactors;
    }

    /**
     * When the verify endpoint checked the context and sealed the result,
     * UTC to the second. Present on the redeemed and expired outcomes.
     *
     * @return the verification time, or null when the cloud did not send one
     */
    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    /**
     * How long before this redemption the verification happened, in whole
     * seconds by the cloud's clock. Present on the redeemed and expired
     * outcomes. A caller wanting a stricter freshness rule than the cloud's
     * own window applies it to this value.
     *
     * @return the age in seconds, or null when the cloud did not send one
     */
    public Integer getSecondsSinceVerified() {
        return secondsSinceVerified;
    }

    /** @return the HTTP status the cloud answered with, 200 or 503 */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return the response body as received */
    public String getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return "RedeemResult " + statusCode + " context=" + contextValue
            + " signature=" + signature
            + (hasFactors ? " factors=" + factors : "");
    }
}
