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
 * The terms document a 51Did was created under, carried in the byte after
 * the match key. It travels inside the identifier so that a receiver always
 * has the terms the identifier was created under, rather than depending on
 * the surrounding protocol to carry them alongside it, which any hop can
 * drop without the identifier looking any different.
 * <p>
 * The byte is an index into a table in the specification and is not a
 * version number, so that a later document can live at any address rather
 * than only at one a number could compose. An index is never reused and
 * never repointed once published, because repointing one would rewrite what
 * an identifier already issued says it agreed to. A new document is a new
 * index, and every 51Did package has to be released to know it, which is
 * the cost of a receiver being able to trust what it reads.
 * <p>
 * {@link #UNKNOWN} is not {@link #NOT_STATED}. This package will meet an
 * index added to the specification after it was released, and reading that
 * as {@link #NOT_STATED} would read an identifier created under terms as
 * one created under none. {@link FodId#getTermsIndex()} gives the index
 * whatever the answer is, so a caller meeting {@link #UNKNOWN} can look the
 * document up in the specification by hand and can report which index it
 * could not read, and it should treat the identifier as covered by terms it
 * cannot yet read and either take a newer package or refuse the identifier.
 * <p>
 * {@link #NOT_STATED} does not mean the identifier is unrestricted. It
 * means only that the identifier does not carry the answer, so the answer
 * has to come from somewhere else, being the Terms Document Locator in an
 * OpenRTB request or whatever the surrounding protocol provides. Carrying
 * the terms in the identifier does not remove the need to carry a locator
 * where a protocol has one, and where the two disagree the identifier's own
 * value is the one that describes the identifier, because it is inside the
 * signature and the accompanying data is not.
 * <p>
 * The terms and the {@link Usage} answer different questions and a receiver
 * needs both. The usage says where an identifier may go and the terms say
 * which document it was created under. An identifier created for
 * non-marketing carries {@link #NOT_STATED}, because the Model Terms govern
 * marketing use and a non-marketing identifier is not created under them,
 * and it stays barred from a demand source by its usage.
 * <p>
 * The values are the same in every 51Did package. The table is specified at
 * <a href="https://github.com/51Degrees/specifications/blob/main/did-specification/identifier-layout.md">identifier-layout.md</a>,
 * which is the authority for it.
 */
public enum Terms {
    /**
     * Index 0, the terms are not stated in the identifier and the receiver
     * has to take them from the data accompanying it. An identifier issued
     * before the byte existed ends at the match key and reads as this, so
     * absence and zero mean the same thing.
     */
    NOT_STATED(null),

    /**
     * Index 1, the Model Terms for Marketing, version 2.
     */
    MODEL_TERMS_FOR_MARKETING_2("https://m4ow.uk/mtm/2.txt"),

    /**
     * An index added to the specification after this package was released.
     * Terms are stated and this package cannot name them, so it answers
     * with no address, and {@link FodId#getTermsIndex()} says which index it
     * could not read.
     */
    UNKNOWN(null);

    private final String url;

    Terms(String url) {
        this.url = url;
    }

    /**
     * Reads the terms from the Terms index carried after the match key. An
     * index this package does not know answers {@link #UNKNOWN} rather than
     * {@link #NOT_STATED}, because the two say different things.
     *
     * @param index the 1-byte Terms index (0-255)
     * @return the terms document the index stands for
     */
    public static Terms fromIndex(int index) {
        switch (index) {
            case 0:
                return NOT_STATED;
            case 1:
                return MODEL_TERMS_FOR_MARKETING_2;
            default:
                return UNKNOWN;
        }
    }

    /**
     * The address of the terms document. No package ever fetches it, and
     * the receiver decides what to do with it.
     *
     * @return the address, or null for {@link #NOT_STATED} and for
     *         {@link #UNKNOWN}, which is never an empty string and is never
     *         an address built from the index
     */
    public String getUrl() {
        return url;
    }
}
