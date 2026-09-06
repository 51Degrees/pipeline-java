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

/**
 * The usage a 51Did was created for, carried in bits 0-2 of the Flags
 * byte. It decides where the identifier may go, so one
 * created for {@link #NON_MARKETING} must never be passed to a demand
 * source, and one created for {@link #STANDARD} or {@link #PERSONALIZED}
 * may be passed only to a recipient that has accepted the applicable
 * terms.
 * <p>
 * The three usages are cumulative rather than exclusive in the byte.
 * Non-marketing sets bit 0, standard sets bits 0 and 1, and personalized
 * sets bits 0, 1 and 2, so every marketing identifier also carries the
 * non-marketing bit. A caller who masked the byte for that bit alone would
 * read every marketing identifier as non-marketing, which is the wrong way
 * round for a data protection decision. {@link FodId#getUsage()} answers
 * with the highest usage granted, so that mistake cannot be made.
 * <p>
 * The names match the cloud's {@code id.usage} values, {@code non-marketing},
 * {@code standard} and {@code personalized}, and are the same in every 51Did
 * package.
 */
public enum Usage {
    /**
     * No usage bit is set. The cloud never issues such an identifier, so
     * this is an identifier from somewhere else or a damaged one, and it
     * should be treated as though it may not be passed on.
     */
    NONE(null),
    /** Created for use that is not marketing. Must not be passed to a demand source. */
    NON_MARKETING("non-marketing"),
    /** Created for standard marketing, being targeting unrelated to the person's browsing history or interactions. */
    STANDARD("standard"),
    /** Created for personalized marketing, being targeting related to the person's browsing history or interactions. */
    PERSONALIZED("personalized");

    private final String idUsage;

    Usage(String idUsage) {
        this.idUsage = idUsage;
    }

    /**
     * Decodes the usage from a flags byte (bits 0-2), answering the highest
     * usage granted.
     *
     * @param flags the 1-byte flags value (0-255)
     * @return the usage
     */
    public static Usage fromFlags(int flags) {
        if ((flags & 0b100) != 0) {
            return PERSONALIZED;
        }
        if ((flags & 0b010) != 0) {
            return STANDARD;
        }
        if ((flags & 0b001) != 0) {
            return NON_MARKETING;
        }
        return NONE;
    }

    /**
     * @return the cloud's {@code id.usage} value for this usage, or null for
     *         {@link #NONE}
     */
    public String getIdUsage() {
        return idUsage;
    }
}
