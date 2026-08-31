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
 * What a read of a 51Did produced, and why.
 * <p>
 * Every read reports the same three facts, so a caller never has to infer
 * one of them from another. Whether the read worked is {@link #isSuccess()},
 * the 51Did is {@link #getValue()} and is present only on success, and the
 * reason is {@link #getStatus()} either way. When {@link #isSuccess()} is
 * true the value is not null and the status is
 * {@link FodIdParseStatus#PARSED}, and when it is false the value is null
 * and the status names the problem.
 * <p>
 * A successful read says the bytes are a structurally valid 51Did. It says
 * nothing about whether the signature is genuine, which is a separate
 * question answered by {@link FodId#verify(String)},
 * {@link FodId#verifyDetailed(String)} or
 * {@link DidClient#verifySignature(FodId)}.
 * <p>
 * A result carries no text taken from the input, because the input came
 * from outside and putting it in a message would mean logging whatever an
 * untrusted sender chose to send.
 */
public final class FodIdParseResult {

    private final FodId value;

    private final FodIdParseStatus status;

    private FodIdParseResult(FodId value, FodIdParseStatus status) {
        this.value = value;
        this.status = status;
    }

    static FodIdParseResult parsed(FodId value) {
        return new FodIdParseResult(value, FodIdParseStatus.PARSED);
    }

    static FodIdParseResult failed(FodIdParseStatus status) {
        return new FodIdParseResult(null, status);
    }

    /**
     * @return true when the input was a structurally valid 51Did and
     *         {@link #getValue()} holds it
     */
    public boolean isSuccess() {
        return value != null;
    }

    /**
     * @return the 51Did when {@link #isSuccess()} is true, otherwise null.
     *         A parsed 51Did has not had its signature checked.
     */
    public FodId getValue() {
        return value;
    }

    /**
     * @return {@link FodIdParseStatus#PARSED} on success, otherwise the
     *         reason the input is not a 51Did
     */
    public FodIdParseStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "FodIdParseResult{" + status + "}";
    }
}
