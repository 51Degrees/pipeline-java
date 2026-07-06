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
import com.swancommunity.owid.Version;

import java.time.Instant;
import java.util.Arrays;
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
 * </ul>
 * <p>
 * Java's {@link Owid} is {@code final}, so this type <b>composes</b> an OWID
 * rather than inheriting from it: it holds the wrapped envelope and delegates
 * OWID-level concerns (domain, date, payload, signature, base64 round-trip,
 * verification) to it, adding the strongly typed 51Did accessors on top.
 * <p>
 * Constructing a {@code FodId} does <b>not</b> verify the OWID signature. Call
 * {@link #verify(String)} explicitly when cryptographic verification is needed.
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

    private final Owid owid;
    private final int flags;
    private final long licenseId;
    private final byte[] hash;

    private FodId(Owid owid, String paramName) {
        this.owid = owid;
        byte[] payload = owid.getPayload();
        if (payload == null || payload.length < HEADER_LENGTH) {
            throw new IllegalArgumentException(
                "51Did payload must be at least " + HEADER_LENGTH
                + " bytes; got " + (payload == null ? 0 : payload.length)
                + " (" + paramName + ").");
        }
        this.flags = payload[FLAGS_OFFSET] & 0xFF;
        // Little-endian uint32, kept unsigned in a long so the high bit does
        // not sign-extend into a negative value.
        this.licenseId =
              (payload[LICENSE_ID_OFFSET] & 0xFFL)
            | ((payload[LICENSE_ID_OFFSET + 1] & 0xFFL) << 8)
            | ((payload[LICENSE_ID_OFFSET + 2] & 0xFFL) << 16)
            | ((payload[LICENSE_ID_OFFSET + 3] & 0xFFL) << 24);
        int valueLength;
        switch (IdType.fromFlags(flags)) {
            case RANDOM:
                valueLength = GUID_LENGTH;
                break;
            case RESERVED:
                valueLength = payload.length - HEADER_LENGTH;
                break;
            default:
                valueLength = HASH_LENGTH;
                break;
        }
        if (payload.length < HEADER_LENGTH + valueLength) {
            throw new IllegalArgumentException(
                "51Did payload for the " + IdType.fromFlags(flags)
                + " type must be at least " + (HEADER_LENGTH + valueLength)
                + " bytes; got " + payload.length + " (" + paramName + ").");
        }
        // Defensive copy: mutating the returned hash must not change the
        // underlying OWID payload bytes.
        this.hash = Arrays.copyOfRange(
            payload, HASH_OFFSET, HASH_OFFSET + valueLength);
    }

    /**
     * Parses a 51Did from its base64-encoded OWID string.
     *
     * @param base64 base64 of the full OWID envelope
     * @return the parsed 51Did
     * @throws NullPointerException if {@code base64} is null
     * @throws OwidException        if the string is not valid base64 or not a
     *                              valid OWID
     * @throws IllegalArgumentException if the payload is shorter than the
     *                              minimum for its identifier type
     */
    public static FodId fromBase64(String base64) throws OwidException {
        Objects.requireNonNull(base64, "base64");
        return new FodId(Owid.fromBase64(base64), "base64");
    }

    /**
     * Parses a 51Did from the raw bytes of an OWID envelope.
     *
     * @param buffer the OWID envelope bytes
     * @return the parsed 51Did
     * @throws NullPointerException if {@code buffer} is null
     * @throws OwidException        if the bytes are not a valid OWID
     * @throws IllegalArgumentException if the payload is shorter than the
     *                              minimum for its identifier type
     */
    public static FodId fromByteArray(byte[] buffer) throws OwidException {
        Objects.requireNonNull(buffer, "buffer");
        return new FodId(Owid.fromByteArray(buffer), "buffer");
    }

    /**
     * Promotes an already-parsed OWID into a 51Did by unpacking its payload.
     * The OWID is <b>copied</b> (round-tripped through its byte form), not
     * aliased, so that a {@code FodId} can never desync from its envelope if
     * the caller later mutates the OWID it passed in. The supplied OWID must
     * therefore be signed (serializable).
     *
     * @param owid the already-parsed OWID envelope
     * @return a 51Did wrapping an independent copy of {@code owid}
     * @throws NullPointerException if {@code owid} is null
     * @throws OwidException        if {@code owid} cannot be serialized (e.g.
     *                              it has not been signed)
     * @throws IllegalArgumentException if the payload is shorter than the
     *                              minimum for its identifier type
     */
    public static FodId fromOwid(Owid owid) throws OwidException {
        Objects.requireNonNull(owid, "owid");
        return new FodId(Owid.fromByteArray(owid.asByteArray()), "owid");
    }

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
     * @return the 4-byte little-endian License Id (0 to 4294967295)
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

    /** @return the OWID creation date. */
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

    /**
     * @return the OWID as a base64 string
     * @throws OwidException if the OWID has not been signed or cannot be encoded
     */
    public String asBase64() throws OwidException {
        return owid.asBase64();
    }

    /**
     * @return the OWID as a byte array including the signature
     * @throws OwidException if the OWID has not been signed or cannot be encoded
     */
    public byte[] asByteArray() throws OwidException {
        return owid.asByteArray();
    }

    /**
     * Verifies the OWID signature against the supplied public key. This is an
     * explicit, separate step - construction never verifies.
     *
     * @param publicPem the creator's public key in SPKI PEM form
     * @return true if the signature verifies, false otherwise
     * @throws OwidException if the PEM is not a valid public key or a field
     *                       cannot be encoded
     */
    public boolean verify(String publicPem) throws OwidException {
        return owid.verifyWithPublicKey(publicPem, Collections.<Owid>emptyList());
    }
}
