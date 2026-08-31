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

/**
 * Strongly typed reader and cloud client for the 51Did (51Degrees
 * Identifier) value.
 * <p>
 * {@link fiftyone.pipeline.did.FodId} reads a 51Did from its base64 OWID
 * form, in either base64 alphabet, or from the envelope bytes. The
 * {@code tryFrom} readers answer with a
 * {@link fiftyone.pipeline.did.FodIdParseResult} instead of throwing, whose
 * {@link fiftyone.pipeline.did.FodIdParseStatus} names why an input is not
 * a 51Did, and the {@code from} readers make the same read and throw. A
 * 51Did exposes the three payload fields (Flags, License Id and the match
 * key) and the identifier {@link fiftyone.pipeline.did.IdType}, and
 * delegates OWID-level concerns to the envelope it holds. Reading never
 * checks the signature. Compare 51Dids by their match key
 * ({@code getMatchKey()}), never by their envelopes.
 * <p>
 * {@link fiftyone.pipeline.did.DidClient} is what a server uses against the
 * 51Degrees cloud: it fetches and holds the published signing keys, verifies
 * a 51Did's signature offline or through the cloud, and redeems a sealed
 * creator context result into a typed
 * {@link fiftyone.pipeline.did.RedeemResult}.
 */
package fiftyone.pipeline.did;
