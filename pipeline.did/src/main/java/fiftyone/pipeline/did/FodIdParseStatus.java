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

import com.swancommunity.owid.OwidParseStatus;

/**
 * Why a read of a 51Did produced what it did.
 * <p>
 * A 51Did is an OWID envelope around a typed payload, so a read can fail at
 * either layer. The members up to {@link #ABSENT_NODE} are the OWID
 * library's own {@link OwidParseStatus} vocabulary carried across one for
 * one, under the same names and with the same meanings, so a caller reading
 * a 51Did learns exactly what the envelope reader found. The two members
 * after them are the 51Did payload rules that apply once the envelope has
 * been read.
 * <p>
 * Java cannot extend an enum, so the OWID members are mirrored rather than
 * inherited. {@link #fromOwid(OwidParseStatus)} maps by name and refuses to
 * map anything the mirror does not know, so an OWID status is never quietly
 * folded into a more general one.
 */
public enum FodIdParseStatus {

    /**
     * The bytes are a structurally valid 51Did. Says nothing about the
     * signature, which {@link FodId#verify(String)} and
     * {@link DidClient#verifySignature(FodId)} answer separately.
     */
    PARSED,

    /** Nothing was supplied to read, being a null or empty value. */
    MISSING_INPUT,

    /**
     * The input was supplied in a form the reader cannot take. Kept for the
     * cross language vocabulary. Not reachable in Java, where the compiler
     * already refuses anything that is not a string or a byte array.
     */
    INVALID_INPUT_TYPE,

    /** The string is not valid base64, so there are no bytes to read. */
    INVALID_BASE64,

    /** The first byte names an envelope version this reader does not know. */
    UNSUPPORTED_VERSION,

    /** The data stopped in the middle of an envelope field. */
    UNEXPECTED_END,

    /**
     * The creator domain is not terminated, or is longer than the maximum
     * published for a domain name.
     */
    INVALID_DOMAIN_ENCODING,

    /**
     * The declared payload byte count disagrees with the bytes actually
     * present. See {@link OwidParseStatus#BYTE_COUNT_MISMATCH}.
     */
    BYTE_COUNT_MISMATCH,

    /**
     * The envelope is consistent but larger than this runtime can hold. See
     * {@link OwidParseStatus#IMPLEMENTATION_CAPACITY_EXCEEDED}.
     */
    IMPLEMENTATION_CAPACITY_EXCEEDED,

    /**
     * The envelope is malformed in a way none of the others describes. See
     * {@link OwidParseStatus#MALFORMED_ENVELOPE}.
     */
    MALFORMED_ENVELOPE,

    /**
     * The bytes are the marker for an absent optional OWID, the single byte
     * zero, so there is deliberately no identifier here. Not a fault, and
     * not a 51Did either. See {@link OwidParseStatus#ABSENT_NODE}.
     */
    ABSENT_NODE,

    /**
     * The envelope read, but its payload is shorter than the five byte 51Did
     * header (one byte of flags and a four byte licence id), so the
     * identifier type cannot even be read.
     */
    PAYLOAD_TOO_SHORT,

    /**
     * The envelope read and the header names a type, but the payload is
     * shorter than the minimum for that type. Random needs the header plus
     * 16 GUID bytes, and Probabilistic and HashedEmail need the header plus
     * a 32 byte hash. A longer payload is never refused here, because
     * anything past the value is a creator context section whose lengths
     * belong to the cloud.
     */
    INVALID_TYPE_PAYLOAD_LENGTH;

    /**
     * Carries an OWID status across unchanged.
     *
     * @param status what the OWID reader reported
     * @return the member of the same name
     * @throws IllegalArgumentException if the OWID library reports a status
     *                                  this enum has no member for, which
     *                                  means the mirror needs the new member
     *                                  added rather than the status being
     *                                  mapped to a more general one
     */
    public static FodIdParseStatus fromOwid(OwidParseStatus status) {
        return valueOf(status.name());
    }
}
