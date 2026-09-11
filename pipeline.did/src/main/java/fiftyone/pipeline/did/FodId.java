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

import com.swancommunity.owid.Owid;
import com.swancommunity.owid.OwidException;
import com.swancommunity.owid.OwidParseResult;
import com.swancommunity.owid.OwidVerificationResult;
import com.swancommunity.owid.Version;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;

/**
 * A strongly typed reader for the 51Did (51Degrees Identifier) value returned
 * by the 51Degrees Cloud service.
 * <p>
 * A 51Did is described at three levels, and the wording here is deliberate.
 * The <b>51Did</b> is the identifier as a whole. The <b>envelope</b> is the
 * signed {@link Owid} that carries it (version, domain, date, payload,
 * signature), re-issued fresh on every call. The <b>match key</b> is the
 * stable, comparable part of the payload after the Flags and License Id,
 * exposed as {@link #getMatchKey()}. Two 51Dids for the same inputs share the
 * same match key even though their envelopes differ on every issue.
 * <b>Compare match keys, never envelopes.</b>
 * <p>
 * Payload layout. Read a 51Did through the typed accessors below, never by
 * walking the payload bytes. The identifier carries a five byte header of
 * Flags and License Id, then the match key, whose length the identifier
 * type in bits 6-7 of Flags decides, then the Terms byte naming the terms
 * document it was created under, and then an optional creator context
 * section that binds the identifier to the browser and connection it was
 * created on. Only 51Degrees can read that section, so this reader exposes
 * it only as the part of {@link #getPayload()} beyond the Terms, its
 * lengths belong to the cloud, and this reader therefore puts no upper
 * bound on a payload. A payload that ends at the match key has no Terms
 * byte, and a missing byte reads as a Terms of zero, so absence and zero
 * mean the same thing.
 * <p>
 * Bits 4 and 5 of the Flags byte say which payload layout the identifier
 * follows, and this package reads version 0. A payload naming any other
 * version is refused with
 * {@link FodIdParseStatus#UNSUPPORTED_PAYLOAD_VERSION} rather than read
 * under the layout this package knows, because a later version exists
 * precisely because a field moved, so reading one here would answer with
 * values that are wrong rather than absent. The version is not exposed,
 * because a caller has nothing to decide with it.
 * The byte layout is specified at
 * <a href="https://github.com/51Degrees/specifications/blob/main/did-specification/identifier-layout.md">identifier-layout.md</a>,
 * which is the authority for it, and the surface every 51Did package
 * offers is specified at
 * <a href="https://github.com/51Degrees/specifications/blob/main/did-specification/package-surface.md">package-surface.md</a>.
 * <p>
 * Reading and verifying are two separate steps. {@link #tryFromBase64(String)}
 * and {@link #tryFromByteArray(byte[])} read a 51Did from external input
 * without throwing, answering with a {@link FodIdParseResult} that says
 * whether the input was a 51Did and, when it was not, a
 * {@link FodIdParseStatus} naming the reason. {@link #fromBase64(String)},
 * {@link #fromByteArray(byte[])} and {@link #fromOwid(Owid)} do the same read
 * and throw instead for a caller who prefers an exception. A 51Did that
 * reads successfully is structurally valid and nothing more. Its signature
 * has not been checked, so call {@link #verify(String)} or
 * {@link #verifyDetailed(String)} explicitly, or use {@link DidClient} to
 * verify against the cloud's published keys.
 * <p>
 * The cloud issues a 51Did in standard base64 with padding, and a page that
 * puts one in a link converts it to the URL-safe alphabet without padding.
 * Both readers of a string accept either form.
 * <p>
 * Java's {@link Owid} is {@code final}, so this type <b>composes</b> an OWID
 * rather than inheriting from it: it holds the envelope and delegates
 * OWID-level concerns (domain, date, payload, signature, base64 round-trip,
 * verification) to it, adding the strongly typed 51Did accessors on top.
 */
public final class FodId {

    // The byte layout below is not part of the public surface. A caller
    // reads a 51Did through the typed accessors, because every field and
    // every bit already has a name, and reading the payload by hand is how
    // the usage bits get misread. The constants stay package-private so
    // that this package's own readers and tests can build and walk a
    // payload. The layout itself is specified at
    // https://github.com/51Degrees/specifications/blob/main/did-specification/identifier-layout.md

    /** Byte offset of the Flags field within the payload. */
    static final int FLAGS_OFFSET = 0;

    /** Byte offset of the License Id field within the payload. */
    static final int LICENSE_ID_OFFSET = 1;

    /** Byte length of the License Id field. */
    static final int LICENSE_ID_LENGTH = 4;

    /** Byte offset of the match key field within the payload. */
    static final int MATCH_KEY_OFFSET = 5;

    /** Byte length of the match key field (SHA-256). */
    static final int MATCH_KEY_LENGTH = 32;

    /**
     * Byte length of the payload header (Flags + License Id) common to every
     * identifier type.
     */
    static final int HEADER_LENGTH = MATCH_KEY_OFFSET;

    /** Byte length of the GUID match key carried by Random identifiers. */
    static final int GUID_LENGTH = 16;

    /**
     * Minimum byte length of a Random 51Did payload
     * (Flags + License Id + GUID).
     */
    static final int RANDOM_PAYLOAD_LENGTH = HEADER_LENGTH + GUID_LENGTH;

    /**
     * Minimum byte length of a Probabilistic or HashedEmail 51Did payload
     * (Flags + License Id + match key). Random payloads are shorter, see
     * {@link #RANDOM_PAYLOAD_LENGTH}.
     */
    static final int PAYLOAD_LENGTH =
        MATCH_KEY_OFFSET + MATCH_KEY_LENGTH;

    /**
     * Byte length of the Terms field, which follows the match key. It is
     * not part of any minimum above, because a payload that ends at the
     * match key reads as a Terms of zero.
     */
    static final int TERMS_LENGTH = 1;

    /**
     * The payload layout version this package reads, carried in bits 4 and
     * 5 of the Flags byte. Any other version is refused rather than read
     * under this layout.
     */
    static final int SUPPORTED_PAYLOAD_VERSION = 0;

    private final Owid owid;
    private final int flags;
    private final long licenseId;
    private final byte[] matchKey;
    private final int termsIndex;

    /**
     * Built only by {@link #read(Owid)} once the payload has passed the
     * 51Did rules, so an instance never exists for a payload that failed
     * them.
     */
    private FodId(
            Owid owid,
            int flags,
            long licenseId,
            byte[] matchKey,
            int termsIndex) {
        this.owid = owid;
        this.flags = flags;
        this.licenseId = licenseId;
        this.matchKey = matchKey;
        this.termsIndex = termsIndex;
    }

    // ----- Reading without throwing -----

    /**
     * Reads a 51Did from its base64 form without throwing, in either the
     * standard alphabet ({@code +} and {@code /}, as the cloud issues it) or
     * the URL-safe alphabet ({@code -} and {@code _}, as a page puts it in a
     * link), with or without padding, and with or without whitespace around
     * it.
     * <p>
     * The value may be anything at all, because it is external data and
     * failing to be a 51Did is an ordinary outcome rather than an error. The
     * result reports whether the read worked, the 51Did only when it did,
     * and a named reason either way. An envelope failure carries the OWID
     * library's own status unchanged, and a payload failure is one of the
     * two 51Did statuses. See {@link FodIdParseStatus}.
     * <p>
     * Success means the input is structurally a 51Did. The signature has
     * not been checked.
     *
     * @param base64 the encoded 51Did, which may be null
     * @return the 51Did and {@link FodIdParseStatus#PARSED}, or no value and
     *         the reason the string is not a 51Did
     */
    public static FodIdParseResult tryFromBase64(String base64) {
        if (base64 == null) {
            return FodIdParseResult.failed(FodIdParseStatus.MISSING_INPUT);
        }
        return read(Owid.parse(toStandardBase64(base64)));
    }

    /**
     * Reads a 51Did from the raw bytes of an OWID envelope without throwing.
     * The buffer must hold exactly one envelope. See
     * {@link #tryFromBase64(String)} for what the result reports.
     *
     * @param buffer the envelope bytes, which may be null
     * @return the 51Did and {@link FodIdParseStatus#PARSED}, or no value and
     *         the reason the bytes are not a 51Did
     */
    public static FodIdParseResult tryFromByteArray(byte[] buffer) {
        return read(Owid.parse(buffer));
    }

    private static FodIdParseResult read(OwidParseResult envelope) {
        if (envelope.isSuccess() == false) {
            return FodIdParseResult.failed(
                FodIdParseStatus.fromOwid(envelope.getStatus()));
        }
        return read(envelope.getValue());
    }

    /**
     * Applies the 51Did payload rules to an envelope the OWID library has
     * already read or signed. This is the one walk of the payload that every
     * reader, throwing or not, goes through.
     * <p>
     * The rules are lower bounds only. The header must be present before the
     * type can be read, and the type then sets the least the payload can
     * hold. Anything longer is accepted as it stands, because the bytes past
     * the match key are the Terms and then a creator context section whose
     * shape the cloud judges. The Terms adds nothing to those bounds, since
     * a payload that ends at the match key reads as a Terms of zero.
     */
    private static FodIdParseResult read(Owid owid) {
        byte[] payload = owid.getPayload();
        if (payload.length < HEADER_LENGTH) {
            return FodIdParseResult.failed(FodIdParseStatus.PAYLOAD_TOO_SHORT);
        }
        int flags = payload[FLAGS_OFFSET] & 0xFF;
        // The version is read before any field, because a later version
        // exists precisely because a field moved. Reading a payload of a
        // version this package does not know under the layout it does know
        // would answer with values that are wrong rather than absent,
        // which is worse than refusing, and a version that nothing checks
        // protects nothing.
        int payloadVersion = payloadVersionOf(flags);
        if (payloadVersion != SUPPORTED_PAYLOAD_VERSION) {
            return FodIdParseResult.unsupportedPayloadVersion(payloadVersion);
        }
        int matchKeyLength;
        switch (IdType.fromFlags(flags)) {
            case RANDOM:
                matchKeyLength = GUID_LENGTH;
                break;
            case RESERVED:
                // Not yet assigned, so read best-effort. The header fields
                // are unpacked and whatever follows is exposed as the
                // match key.
                matchKeyLength = payload.length - HEADER_LENGTH;
                break;
            default:
                matchKeyLength = MATCH_KEY_LENGTH;
                break;
        }
        if (payload.length < HEADER_LENGTH + matchKeyLength) {
            return FodIdParseResult.failed(
                FodIdParseStatus.INVALID_TYPE_PAYLOAD_LENGTH);
        }
        // Little-endian uint32, kept unsigned in a long so the high bit does
        // not sign-extend into a negative value.
        long licenseId =
              (payload[LICENSE_ID_OFFSET] & 0xFFL)
            | ((payload[LICENSE_ID_OFFSET + 1] & 0xFFL) << 8)
            | ((payload[LICENSE_ID_OFFSET + 2] & 0xFFL) << 16)
            | ((payload[LICENSE_ID_OFFSET + 3] & 0xFFL) << 24);
        // The match key is copied out so that mutating the array a caller
        // gets back from getMatchKey() can never reach the envelope's own
        // bytes.
        byte[] matchKey = Arrays.copyOfRange(
            payload, MATCH_KEY_OFFSET, MATCH_KEY_OFFSET + matchKeyLength);
        // The Terms byte follows the match key, wherever the type put its
        // end. A payload that stops there has no Terms byte, and a missing
        // byte is read as zero, which says the terms are not stated in the
        // identifier. Absence and zero therefore mean the same thing and
        // nothing has to tell them apart.
        int termsOffset = MATCH_KEY_OFFSET + matchKeyLength;
        int termsIndex = payload.length > termsOffset
            ? payload[termsOffset] & 0xFF
            : 0;
        return FodIdParseResult.parsed(
            new FodId(owid, flags, licenseId, matchKey, termsIndex));
    }

    // ----- Reading with exceptions -----

    /**
     * Parses a 51Did from its base64 form, accepting the same inputs as
     * {@link #tryFromBase64(String)}, and throws when the input is not a
     * 51Did. The read is the same one, so the two never disagree about an
     * input. Parsing does not check the signature.
     *
     * @param base64 base64 of the full OWID envelope
     * @return the parsed 51Did
     * @throws NullPointerException if {@code base64} is null
     * @throws OwidException        if the string is not valid base64 or not a
     *                              valid OWID envelope, with the
     *                              {@link FodIdParseStatus} in the message
     * @throws IllegalArgumentException if the payload is shorter than the
     *                              minimum for its identifier type
     */
    public static FodId fromBase64(String base64) throws OwidException {
        Objects.requireNonNull(base64, "base64");
        return valueOrThrow(tryFromBase64(base64), "base64");
    }

    /**
     * Restores a base64 string that may use the URL-safe alphabet, with or
     * without padding, to the standard alphabet with padding, which is the
     * only alphabet the envelope library reads. Leading and trailing
     * whitespace is removed first, because a value read from a header, a
     * file or a form field often carries a newline or a space around it and
     * neither belongs to the identifier. Then {@code -} becomes {@code +},
     * {@code _} becomes {@code /}, and {@code ==} or {@code =} is appended
     * when the length modulo 4 is 2 or 3. That padding is worked out from
     * the trimmed length, so whitespace cannot push the value into the wrong
     * case. A value already in the standard padded form with no whitespace
     * around it is returned unchanged. Nothing here decides whether the
     * result is base64 at all, which is the envelope library's answer.
     *
     * @param value the base64 text in either alphabet
     * @return the same value in the standard alphabet with padding
     */
    static String toStandardBase64(String value) {
        String standard = value.trim().replace('-', '+').replace('_', '/');
        switch (standard.length() % 4) {
            case 2:
                return standard + "==";
            case 3:
                return standard + "=";
            default:
                return standard;
        }
    }

    /**
     * Parses a 51Did from the raw bytes of an OWID envelope, accepting the
     * same inputs as {@link #tryFromByteArray(byte[])}, and throws when the
     * bytes are not a 51Did. Parsing does not check the signature.
     *
     * @param buffer the OWID envelope bytes
     * @return the parsed 51Did
     * @throws NullPointerException if {@code buffer} is null
     * @throws OwidException        if the bytes are not a valid OWID
     *                              envelope, with the
     *                              {@link FodIdParseStatus} in the message
     * @throws IllegalArgumentException if the payload is shorter than the
     *                              minimum for its identifier type
     */
    public static FodId fromByteArray(byte[] buffer) throws OwidException {
        Objects.requireNonNull(buffer, "buffer");
        return valueOrThrow(tryFromByteArray(buffer), "buffer");
    }

    /**
     * Promotes an OWID the envelope library has already read or signed into
     * a 51Did by applying the payload rules to it. The OWID is held as it
     * is, because the library hands out only immutable envelopes that came
     * from a complete read or from a signer, so there is nothing a caller
     * can later change underneath the 51Did.
     *
     * @param owid the envelope
     * @return a 51Did over {@code owid}
     * @throws NullPointerException if {@code owid} is null
     * @throws OwidException        never thrown by the current envelope
     *                              library, which cannot hand out an
     *                              envelope this method fails to read.
     *                              Declared so that callers written against
     *                              the earlier library keep compiling.
     * @throws IllegalArgumentException if the payload is shorter than the
     *                              minimum for its identifier type
     */
    public static FodId fromOwid(Owid owid) throws OwidException {
        Objects.requireNonNull(owid, "owid");
        return valueOrThrow(read(owid), "owid");
    }

    /**
     * Turns a failed read into the exception the throwing readers document
     * for it. A payload rule failure is an argument failure and an envelope
     * failure is an OWID one, which is the split the readers have always
     * made. The message names the status and the parameter, never the input.
     */
    /**
     * Bits 4 and 5 of the Flags byte, being the version of the payload
     * layout the identifier follows. The envelope carries a version of its
     * own at its first byte, which versions the envelope, whilst this one
     * versions the payload.
     *
     * @param flags the Flags byte
     * @return the payload layout version (0 to 3)
     */
    private static int payloadVersionOf(int flags) {
        return (flags >> 4) & 0b11;
    }

    private static FodId valueOrThrow(FodIdParseResult result, String paramName)
            throws OwidException {
        switch (result.getStatus()) {
            case PARSED:
                return result.getValue();
            case PAYLOAD_TOO_SHORT:
                throw new IllegalArgumentException(
                    "51Did payload must be at least " + HEADER_LENGTH
                    + " bytes to carry the header (" + paramName + ").");
            case INVALID_TYPE_PAYLOAD_LENGTH:
                throw new IllegalArgumentException(
                    "51Did payload is shorter than the minimum for its "
                    + "identifier type (" + paramName + ").");
            case UNSUPPORTED_PAYLOAD_VERSION:
                throw new IllegalArgumentException(
                    "51Did payload version " + result.getPayloadVersion()
                    + " is not one this package can read ("
                    + paramName + ").");
            default:
                throw new OwidException(
                    "The value is not an OWID envelope: "
                    + result.getStatus() + " (" + paramName + ").");
        }
    }

    // ----- Fields -----

    /**
     * The raw Flags byte. Package-private on purpose, because
     * {@link #getType()}, {@link #getUsage()} and
     * {@link #isUsageFromConsent()} name every bit a caller needs and
     * masking the byte by hand is how the cumulative usage bits get read
     * backwards. Kept because those three accessors are built on it.
     *
     * @return the 1-byte flags bit-mask from the payload (0-255)
     */
    int getFlags() {
        return flags;
    }

    /**
     * @return the identifier type carried in bits 6-7 of the Flags byte
     */
    public IdType getType() {
        return IdType.fromFlags(flags);
    }

    /**
     * @return the usage carried in bits 0-2 of the Flags byte, as the
     *         highest usage granted; see {@link Usage} for why it is read
     *         that way
     */
    public Usage getUsage() {
        return Usage.fromFlags(flags);
    }

    /**
     * Whether the usage was derived from an IAB consent string the caller
     * sent, rather than stated by the caller directly. This is bit 3 of
     * the Flags byte. Both are legitimate ways to arrive at a usage,
     * and this says nothing about which usage it is.
     *
     * @return whether the usage came from a consent string
     */
    public boolean isUsageFromConsent() {
        return (flags & 0b1000) != 0;
    }

    /**
     * The 4-byte little-endian License Id field (0 to 4294967295).
     * <p>
     * On an identifier carrying a creator context, the four bytes at offset
     * 1 hold an encrypted value that only 51Degrees can turn back into a
     * licence identifier. This property is therefore the field's raw value,
     * and on such an identifier it identifies nothing outside 51Degrees.
     *
     * @return the raw License Id field
     */
    public long getLicenseId() {
        return licenseId;
    }

    /**
     * Returns the match key from the payload (a 32-byte SHA-256 for
     * Probabilistic and HashedEmail identifiers, or 16 GUID bytes for Random).
     * The match key is the stable, comparable part of the envelope. Two
     * 51Dids for the same inputs share the same match key even though their
     * envelopes (date, signature) differ on every issue, so use the match
     * key as the cache / dedup key.
     *
     * @return a defensive copy of the match key bytes
     */
    public byte[] getMatchKey() {
        return matchKey.clone();
    }

    /**
     * The address of the terms document this 51Did was created under, from
     * the Terms byte that follows the match key.
     * <p>
     * The byte is an index into a table in the specification and this
     * package turns the index into the address, so a caller never handles
     * the byte. The address is answered and never fetched, and the receiver
     * decides what to do with the document.
     * <p>
     * Null covers both an index of zero, which says the terms are not
     * stated in the identifier, and an index added to the specification
     * after this package was released, which it cannot name. A caller
     * cannot tell those two apart, which is deliberate, because both lead
     * to the same place, being that the identifier does not say which terms
     * it was created under and the answer has to come from somewhere else.
     * No package may build an address from an index it does not know, since
     * that would name a document nobody wrote.
     * <p>
     * No address does not mean the identifier is unrestricted. Where an
     * identifier may go is a separate question {@link #getUsage()} answers,
     * which still bars a non-marketing identifier from a demand source.
     *
     * @return the address of the terms document, or null where the
     *         identifier names no document this package knows, which is
     *         never an empty string and is never built from the index
     */
    public String getTerms() {
        return Terms.fromIndex(termsIndex).getUrl();
    }

    /** @return the OWID version. */
    public Version getVersion() {
        return owid.getVersion();
    }

    /** @return the domain of the OWID creator. */
    public String getDomain() {
        return owid.getDomain();
    }

    /**
     * The envelope's creation date, to the minute. The envelope stores it
     * as a count of minutes since 2020-01-01T00:00:00Z, and this reader
     * hands back the date itself rather than that count, because two dates
     * compare exactly as well as two counts do.
     *
     * @return the OWID creation date
     */
    public Instant getDate() {
        return owid.getDate();
    }

    /** @return a copy of the OWID payload bytes. */
    public byte[] getPayload() {
        return owid.getPayload();
    }

    /** @return a copy of the 64-byte OWID signature. */
    public byte[] getSignature() {
        return owid.getSignature();
    }

    // ----- Encoding -----

    /**
     * @return the OWID as a base64 string in the standard alphabet with
     *         padding, the form the cloud issues
     * @throws OwidException if a field cannot be encoded
     */
    public String asBase64() throws OwidException {
        return owid.asBase64();
    }

    /**
     * The OWID as a base64 string in the URL-safe alphabet ({@code -} and
     * {@code _}) without padding, the inverse of what
     * {@link #fromBase64(String)} restores, so the identifier can go into a
     * URL without any conversion by the caller.
     *
     * @return the URL-safe base64 form without padding
     * @throws OwidException if a field cannot be encoded
     */
    public String asBase64Url() throws OwidException {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(asByteArray());
    }

    /**
     * @return the OWID as a byte array including the signature
     * @throws OwidException if a field cannot be encoded
     */
    public byte[] asByteArray() throws OwidException {
        return owid.asByteArray();
    }

    // ----- Signature verification -----

    /**
     * Verifies the OWID signature against the supplied public key. This is an
     * explicit, separate step, because reading a 51Did never verifies it.
     *
     * @param publicPem the creator's public key in SPKI PEM form
     * @return true if the signature verifies, false otherwise
     * @throws OwidException if the PEM is not a valid public key or a field
     *                       cannot be encoded
     */
    public boolean verify(String publicPem) throws OwidException {
        return owid.verifyWithPublicKey(publicPem, Collections.<Owid>emptyList());
    }

    /**
     * Verifies the OWID signature against the supplied public key and says
     * why, keeping "the signature does not match" apart from "the signature
     * could not be checked". A missing key reports
     * {@code KEY_UNAVAILABLE} and an unreadable one {@code INVALID_KEY},
     * and neither is {@code SIGNATURE_INVALID}, because reporting an outage
     * as a forgery would be wrong in both directions.
     *
     * @param publicPem the creator's public key in SPKI PEM form, which may
     *                  be null when no key could be obtained
     * @return the outcome of the check
     */
    public OwidVerificationResult verifyDetailed(String publicPem) {
        return owid.verify(publicPem, Collections.<Owid>emptyList());
    }
}
