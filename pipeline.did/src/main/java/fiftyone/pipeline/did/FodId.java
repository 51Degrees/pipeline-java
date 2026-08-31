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

import java.time.Duration;
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
 * signature), re-issued fresh on every call. The <b>value</b> is the stable,
 * comparable part of the payload after the Flags and License Id, exposed as
 * {@link #getHash()}. Two 51Dids for the same inputs share the same value even
 * though their envelopes differ on every issue. <b>Compare values, never
 * envelopes.</b>
 * <p>
 * Payload layout. The header (offsets 0-4) is shared by every identifier type;
 * bits 6-7 of Flags select the {@link IdType} and the length of the value that
 * follows:
 * <ul>
 *   <li>offset 0, length 1: Flags (bits 0-2 usage, bits 6-7 type)</li>
 *   <li>offset 1, length 4: License Id (uint32, little-endian)</li>
 *   <li>offset 5: value - 32-byte SHA-256 (Probabilistic, HashedEmail) or
 *       16 GUID bytes (Random)</li>
 *   <li>after the value, optionally: a creator context section, which binds
 *       the identifier to the browser and connection it was created on.
 *       Only 51Degrees can read it, so this reader exposes it only as the
 *       part of {@link #getPayload()} beyond the value. Its lengths belong
 *       to the cloud, so this reader puts no upper bound on a payload.</li>
 * </ul>
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

    /** Byte offset of the Flags field within the payload. */
    public static final int FLAGS_OFFSET = 0;

    /** Byte offset of the License Id field within the payload. */
    public static final int LICENSE_ID_OFFSET = 1;

    /** Byte length of the License Id field. */
    public static final int LICENSE_ID_LENGTH = 4;

    /** Byte offset of the value (Hash) field within the payload. */
    public static final int HASH_OFFSET = 5;

    /** Byte length of the SHA-256 value. */
    public static final int HASH_LENGTH = 32;

    /**
     * Byte length of the payload header (Flags + License Id) common to every
     * identifier type.
     */
    public static final int HEADER_LENGTH = HASH_OFFSET;

    /** Byte length of the GUID value carried by Random identifiers. */
    public static final int GUID_LENGTH = 16;

    /**
     * Minimum byte length of a Random 51Did payload
     * (Flags + License Id + GUID).
     */
    public static final int RANDOM_PAYLOAD_LENGTH = HEADER_LENGTH + GUID_LENGTH;

    /**
     * Minimum byte length of a Probabilistic or HashedEmail 51Did payload
     * (Flags + License Id + Hash). Random payloads are shorter - see
     * {@link #RANDOM_PAYLOAD_LENGTH}.
     */
    public static final int PAYLOAD_LENGTH = HASH_OFFSET + HASH_LENGTH;

    /**
     * The origin the envelope's date counts from, 2020-01-01T00:00:00Z, as
     * epoch seconds. See {@link #getDateMinutes()}.
     */
    private static final long DATE_ORIGIN_EPOCH_SECONDS = 1_577_836_800L;

    private final Owid owid;
    private final int flags;
    private final long licenseId;
    private final byte[] hash;

    /**
     * Built only by {@link #read(Owid)} once the payload has passed the
     * 51Did rules, so an instance never exists for a payload that failed
     * them.
     */
    private FodId(Owid owid, int flags, long licenseId, byte[] hash) {
        this.owid = owid;
        this.flags = flags;
        this.licenseId = licenseId;
        this.hash = hash;
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
     * the value are a creator context section whose shape the cloud judges.
     */
    private static FodIdParseResult read(Owid owid) {
        byte[] payload = owid.getPayload();
        if (payload.length < HEADER_LENGTH) {
            return FodIdParseResult.failed(FodIdParseStatus.PAYLOAD_TOO_SHORT);
        }
        int flags = payload[FLAGS_OFFSET] & 0xFF;
        int valueLength;
        switch (IdType.fromFlags(flags)) {
            case RANDOM:
                valueLength = GUID_LENGTH;
                break;
            case RESERVED:
                // Not yet assigned, so read best-effort. The header fields
                // are unpacked and whatever follows is exposed as the value.
                valueLength = payload.length - HEADER_LENGTH;
                break;
            default:
                valueLength = HASH_LENGTH;
                break;
        }
        if (payload.length < HEADER_LENGTH + valueLength) {
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
        // The value is copied out so that mutating the array a caller gets
        // back from getHash() can never reach the envelope's own bytes.
        byte[] hash = Arrays.copyOfRange(
            payload, HASH_OFFSET, HASH_OFFSET + valueLength);
        return FodIdParseResult.parsed(
            new FodId(owid, flags, licenseId, hash));
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
            default:
                throw new OwidException(
                    "The value is not an OWID envelope: "
                    + result.getStatus() + " (" + paramName + ").");
        }
    }

    // ----- Fields -----

    /**
     * @return the 1-byte usage flags bit-mask from the payload (0-255)
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return the identifier type carried in bits 6-7 of {@link #getFlags()}
     */
    public IdType getType() {
        return IdType.fromFlags(flags);
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
     * Returns the value bytes from the payload (a 32-byte SHA-256 for
     * Probabilistic and HashedEmail identifiers, or 16 GUID bytes for Random).
     * This is the stable, comparable part of the envelope - use it as the
     * cache / dedup key.
     *
     * @return a defensive copy of the value bytes
     */
    public byte[] getHash() {
        return hash.clone();
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
     * The envelope's creation date, to the minute. See
     * {@link #getDateMinutes()} for the same date as the envelope stores it.
     *
     * @return the OWID creation date
     */
    public Instant getDate() {
        return owid.getDate();
    }

    /**
     * The envelope's own date field, the unsigned 32-bit count of minutes
     * since 2020-01-01T00:00:00Z. It is the value the OWID
     * {@code public-key?date=} parameter takes, and the integer a caller
     * comparing creation times wants rather than a converted date.
     *
     * @return minutes since 2020-01-01T00:00:00Z, 0 to 4294967295
     */
    public long getDateMinutes() {
        return Duration.between(
            Instant.ofEpochSecond(DATE_ORIGIN_EPOCH_SECONDS),
            owid.getDate()).toMinutes();
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
