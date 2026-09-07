# pipeline.did

Strongly typed Java reader and cloud client for the 51Did (51Degrees
Identifier) returned by the 51Degrees Cloud service. Mirrors the .NET
`FiftyOne.Did` package.

## Terminology

A 51Did is described at three levels, and the wording is deliberate.

- The **51Did** (51Degrees Identifier) is the identifier as a whole.
- The **envelope** is the data model that carries it: a signed OWID holding
  the version, domain, date, payload and signature. It changes byte-for-byte
  every time the cloud issues one, even for the same inputs, because the date
  and signature change on each call.
- The **match key** is the stable, comparable part of the payload after the
  Flags and License Id, a 32-byte SHA-256 for Probabilistic and HashedEmail
  identifiers, or 16 GUID bytes for Random. Two 51Dids for the same inputs
  share the same match key even though their envelopes differ.

**Comparing two 51Dids means comparing their match keys, never their
envelopes.**

## Payload layout

The byte layout of a 51Did is specified in
[identifier-layout.md](https://github.com/51Degrees/specifications/blob/main/did-specification/identifier-layout.md),
and the surface every 51Did package offers is specified in
[package-surface.md](https://github.com/51Degrees/specifications/blob/main/did-specification/package-surface.md).
Those two pages are the authority, so read them rather than working the
layout out from this package. What follows is a summary of the part that
changes how this package behaves.

The identifier carries a five byte header of Flags and License Id, then the
match key, then an optional creator context section. Bits 6-7 of Flags
select the type, which decides how long the match key is and so what the
least a payload can hold is.

| Bits 7-6 | `IdType`        | Match key length | Minimum payload |
|---------:|-----------------|-------------:|----------------:|
|     `00` | `PROBABILISTIC` |           32 |              37 |
|     `01` | `RANDOM`        |           16 |              21 |
|     `10` | `HASHED_EMAIL`  |           32 |              37 |
|     `11` | `RESERVED`      |    remainder |               5 |

Identifiers issued before the type tag existed have bits 6-7 zeroed and decode
as `PROBABILISTIC`.

The minimums in that table are the only lengths this package enforces. There
is no upper bound. An identifier carrying a creator context is longer than
the minimum, its extra bytes have a shape only the cloud knows, and a reader
built before that shape existed still reads the identifier, so the package
never refuses a payload for being long. On such an identifier the four
License Id bytes hold an encrypted value that only 51Degrees can turn back
into a licence identifier, so `getLicenseId()` is the field's raw value and
identifies nothing outside 51Degrees.

The package gives no way to read the payload by hand. There is no raw flags
accessor and the offsets and lengths are package-private, because every
field and every bit already has a typed accessor and reading the bytes by
hand is how the usage gets misread. See the usage section below for the
mistake this closes off.

## OWID dependency

`FodId` builds on the OWID envelope library
([SWAN-community/owid-java](https://github.com/SWAN-community/owid-java),
package `com.swancommunity.owid`). Because that library's `Owid` type is
`final`, `FodId` **composes** an OWID (holds one and delegates OWID-level
concerns to it) rather than inheriting from it.

The OWID source is consumed from a git submodule at the repository root
(`owid-java/`, mirroring how `pipeline-dotnet` carries the `owid-dotnet`
submodule) and compiled into this module at its Java 8 level, so there is no
separate runtime dependency. The vendored OWID sources keep their Apache-2.0
headers; the 51Did sources are EUPL-1.2.

The OWID library only hands out an `Owid` that came from a complete,
structurally valid read or from a `Creator` that signed it. There is no
public constructor, no setter and no throwing parse. `FodId` keeps the same
rule, so a `FodId` you hold is always a whole envelope over a payload that
met the 51Did minimums, and the only thing left to ask about it is whether
the signature is genuine.

The cloud client reads JSON with `org.json:json`, at the version the
pipeline's cloud request engine already uses.

### Bundled third-party licence

Because the OWID (`com.swancommunity.owid.*`) code is compiled into
`pipeline.did.jar`, the jar ships Apache-2.0 code alongside the EUPL-1.2 51Did
code. As required by Apache-2.0, the jar carries the full Apache licence text
and an attribution: see `META-INF/LICENSE-owid.txt` and `META-INF/NOTICE.txt`
(OWID is © 51 Degrees Mobile Experts Limited, from
[SWAN-community/owid-java](https://github.com/SWAN-community/owid-java),
Apache-2.0).

## Reading a 51Did

A 51Did arrives from outside, so failing to be one is an ordinary outcome
rather than an error. The readers that answer without throwing are the ones
to use on external input.

```java
import fiftyone.pipeline.did.FodId;
import fiftyone.pipeline.did.FodIdParseResult;
import fiftyone.pipeline.did.FodIdParseStatus;

FodIdParseResult read = FodId.tryFromBase64(valueFromThePage);
if (read.isSuccess()) {
    FodId fodId = read.getValue();   // structurally valid, not yet verified
} else {
    FodIdParseStatus why = read.getStatus();   // for example INVALID_BASE64
}
```

`tryFromByteArray(byte[])` does the same for the raw envelope bytes. Every
result reports the same three facts, and they always agree:

1. `isSuccess()` says whether the input was a 51Did.
2. `getValue()` is the `FodId` on success and `null` otherwise. There is never
   a half-read `FodId`.
3. `getStatus()` is `PARSED` on success and names the reason otherwise.

The throwing readers `fromBase64(String)`, `fromByteArray(byte[])` and
`fromOwid(Owid)` make exactly the same read and throw instead, for code that
already handles exceptions. An envelope fault throws `OwidException` with the
status in the message, a payload shorter than its type's minimum throws
`IllegalArgumentException`, and a `null` argument throws
`NullPointerException`.

Both string readers accept the standard alphabet the cloud issues (`+`, `/`,
padded) and the URL-safe alphabet a page puts in a link (`-`, `_`, padding
optional), with or without whitespace around the value. `asBase64Url()` gives
the URL-safe form back, so an identifier can go into a URL without any
conversion by the caller.

### What the status means

`FodIdParseStatus` carries the OWID library's own statuses across under the
same names, so an envelope fault is reported exactly as the envelope reader
found it, and adds two of its own for the payload rules.

| Status | Meaning | Layer |
|---|---|---|
| `PARSED` | A structurally valid 51Did. Says nothing about the signature. | |
| `MISSING_INPUT` | Null or empty input. | OWID |
| `INVALID_BASE64` | The string is not base64. | OWID |
| `UNSUPPORTED_VERSION` | The first byte names an envelope version the reader does not know. | OWID |
| `UNEXPECTED_END` | The data stopped in the middle of an envelope field. | OWID |
| `INVALID_DOMAIN_ENCODING` | The creator domain is unterminated or longer than a domain name can be. | OWID |
| `BYTE_COUNT_MISMATCH` | The declared payload length disagrees with the bytes present. | OWID |
| `IMPLEMENTATION_CAPACITY_EXCEEDED` | Consistent, but larger than this runtime can hold. Not reachable in Java. | OWID |
| `MALFORMED_ENVELOPE` | A fault none of the others describes. Nothing produces one today. | OWID |
| `ABSENT_NODE` | The single zero byte that marks an absent optional OWID. Not a fault, not a 51Did. | OWID |
| `INVALID_INPUT_TYPE` | Kept for the cross language vocabulary. Not reachable in Java. | OWID |
| `PAYLOAD_TOO_SHORT` | The envelope read, but the payload cannot hold the five byte header, so the type cannot be read. | 51Did |
| `INVALID_TYPE_PAYLOAD_LENGTH` | The header names a type and the payload is shorter than that type's minimum. | 51Did |

Every one of those is an expected data result and comes back as a status.
What remains exceptional is a `null` passed to a throwing reader, and on the
client, a cloud that cannot be reached or a key list that cannot be fetched,
which the client reports by failing the future rather than by throwing.

### Reading is not verifying

A successful read means the bytes are the right shape. It does not mean the
cloud issued them. Verify the signature as a separate step, with
`verify(publicKeyPem)` for a yes or no, `verifyDetailed(publicKeyPem)` to
learn why not, or `DidClient` to check against the cloud's published keys.
`verifyDetailed` keeps "the signature does not match" (`SIGNATURE_INVALID`)
apart from "the signature could not be checked" (`KEY_UNAVAILABLE`,
`INVALID_KEY`), because an outage is not a forgery.

## Usage

```java
import fiftyone.pipeline.did.FodId;
import fiftyone.pipeline.did.IdType;
import fiftyone.pipeline.did.Usage;
import java.time.Instant;

FodId fodId = FodId.fromBase64(base64FromCloudService);

IdType  type       = fodId.getType();      // PROBABILISTIC / RANDOM / HASHED_EMAIL
Usage   usage      = fodId.getUsage();     // what the identifier may be used for
boolean fromConsent = fodId.isUsageFromConsent();
long    licenseId  = fodId.getLicenseId();
byte[]  matchKey   = fodId.getMatchKey();  // SHA-256 or GUID bytes, see type

// Delegated OWID-level fields and operations.
String  domain   = fodId.getDomain();
Instant date     = fodId.getDate();        // when the cloud issued it
boolean verified = fodId.verify(publicKeyPem);
String  base64   = fodId.asBase64();       // standard alphabet, padded
String  forUrl   = fodId.asBase64Url();    // URL-safe alphabet, no padding
```

## Comparing two 51Dids

```java
FodId a = FodId.fromBase64(idprobglobalA);
FodId b = FodId.fromBase64(idprobglobalB);

// The envelope (date, signature, base64) differs across reissues.
// The match key inside the payload is stable - this is what you compare:
boolean sameMatchKey = java.util.Arrays.equals(a.getMatchKey(), b.getMatchKey());
```

Use `getMatchKey()` as the cache / dedup key.

## What a 51Did may be used for

`getUsage()` answers what the identifier was created for, and it is the
accessor a data protection decision turns on.

| `Usage` | Cloud `id.usage` | What it means |
|---|---|---|
| `NON_MARKETING` | `non-marketing` | Created for use that is not marketing. Must never be passed to a demand source. |
| `STANDARD` | `standard` | Created for standard marketing, being targeting unrelated to the person's browsing history or interactions. |
| `PERSONALIZED` | `personalized` | Created for personalized marketing, being targeting related to the person's browsing history or interactions. |
| `NONE` | none | No usage bit is set. The cloud never issues such an identifier, so treat it as one that may not be passed on. |

`STANDARD` and `PERSONALIZED` may be passed only to a recipient that has
accepted the applicable terms.

The three usages are cumulative in the byte rather than exclusive, because
non-marketing sets one bit, standard sets two and personalized sets three,
so every marketing identifier also carries the non-marketing bit. Code that
masked the byte for that bit alone would read every marketing identifier as
non-marketing, which is the wrong way round for a rule that says a
non-marketing identifier must never reach a demand source. `getUsage()`
answers with the highest usage granted, so that mistake cannot be made, and
the package offers no raw flags accessor with which to make it.

`isUsageFromConsent()` says whether the usage came from an IAB consent
string the caller sent rather than being stated by the caller directly.
Both are legitimate ways to arrive at a usage and this says nothing about
which usage it is.

```java
if (fodId.getUsage() == Usage.NON_MARKETING) {
    // Do not pass this identifier to a demand source.
}
```

## Verifying on your server

`DidClient` handles every manipulation of a 51Did a server needs against the
51Degrees cloud, so server code never hand-writes HTTP or key handling.
Build one at start-up and share it, because it holds the cloud's published
signing keys in memory.

```java
import fiftyone.pipeline.did.DidClient;
import fiftyone.pipeline.did.FodId;
import fiftyone.pipeline.did.RedeemResult;

// The resource key is the page's and public by nature. The licence key is
// server side only and is needed to redeem where the account holds licence
// keys. The endpoint defaults to https://cloud.51degrees.com/api/v4/, or
// the FOD_CLOUD_API_URL environment variable where that is set.
DidClient client = new DidClient(resourceKey, licenceKey);
```

In the order a server uses them:

1. **Read.** The identifier arrives from a page in the URL-safe alphabet.

   ```java
   FodIdParseResult read = FodId.tryFromBase64(fromThePage);
   if (read.isSuccess() == false) {
       // answer 400, naming read.getStatus() if you wish
   }
   FodId fodId = read.getValue();
   ```

2. **Verify offline.** The client fetches the cloud's signing keys once,
   holds them, and checks the signature against the key in force when the
   identifier was created. No use is charged. Every client method that may
   reach the cloud returns at once with a `CompletableFuture`, so a request
   thread is never held while the cloud is asked.

   ```java
   CompletableFuture<Boolean> genuine = client.verifySignature(fodId);
   // or, to learn why not:
   CompletableFuture<DidClient.SignatureCheck> check =
       client.verifySignatureDetailed(fodId);
   ```

   `publicKeys()` answers with the held list and `publicKeyFor(fodId)` with
   the key in force at the identifier's date. The list is refetched, once,
   when it has no key for the date, when the date is later than the newest
   start held, or when the list is more than a day old, and callers that
   arrive while a fetch is under way wait on that one fetch rather than
   starting their own. A key list that cannot be fetched fails the future
   with `IOException`, never with a false, because not being able to check
   is not the same as the signature being wrong.

3. **Verify through the cloud.** The open verify endpoint, one use against
   the resource key, needing no licence key.

   ```java
   CompletableFuture<Boolean> genuine = client.verify(fodId);
   ```

4. **Redeem.** A page checks the creator context from the browser with
   `verify-full` or `verify-context` and relays the sealed `result` to your
   server. Your server redeems it, with the licence key, against the
   identifier it knows independently. One use against the resource key.

   ```java
   client.redeem(fodId, result, challenge).thenAccept(redeemed -> {
       switch (redeemed.getContext()) {
           case VERIFIED:      // presented from where it was created
           case MISMATCH:      // redeemed.getFactors() says which differs
           case NO_CONTEXT:    // the identifier carries no creator context
           case NOT_CHECKABLE: // the cloud could not check it
           case EXPIRED:       // redeemed outside the freshness window
           case REPLAYED:      // already redeemed
           case UNREADABLE:    // tampered, wrong identifier, challenge or key
           case UNCONFIRMED:   // answered 503, retry
       }
       redeemed.getSignature();            // VERIFIED, INVALID or UNKNOWN
       redeemed.getVerifiedAt();           // when the cloud sealed the result
       redeemed.getSecondsSinceVerified(); // how long before this redemption
   });
   ```

   A failure completes the future exceptionally. A malformed identifier
   fails it with `IllegalArgumentException`, a host without the creator
   context with `DidNotSupportedException`, any other status with
   `DidHttpException` carrying the status and body, and an unreachable
   cloud with `IOException`. `get()` reports the failure as the cause of an
   `ExecutionException`, `join()` as the cause of a `CompletionException`,
   and a stage added with `exceptionally`, `handle` or `whenComplete`
   receives a `CompletionException` whose cause it is. Every cryptographic
   failure comes back as the one word `unreadable`, by design, so the
   client does not try to distinguish them either.

The client methods that take the identifier as a string, `verify(String)`
and `redeem(String, String, String)`, read the value before doing anything
else, and for a value that does not read as a 51Did the future fails with
`IllegalArgumentException` naming the status, before any key is fetched or
the cloud is called, in the same shape as a failure the cloud answers with,
so a caller has one place to look. Two things are worth knowing about that
boundary. The client also turns away any string longer than a generous
fixed limit before reading it, which is client policy against obviously
wrong input and says nothing about how long a 51Did can be. And the client
checks the shape, not the signature, because the signature is the question
the cloud is about to be asked.

The default transport is `java.net.HttpURLConnection`, which only blocks,
so by default each request is blocking I/O on a background thread. Those
threads come from a small shared pool of daemon threads, or from an
executor given to the builder:

```java
DidClient client = DidClient.builder(resourceKey)
    .licenceKey(licenceKey)
    .executor(myPool)
    .build();
```

On Java 11 and later, supply a transport over
`java.net.http.HttpClient.sendAsync` through `transport(HttpTransport)`
instead, which needs no thread per request, and give no executor, because
the executor belongs to the default transport and a transport of your own
schedules its own work. An
`HttpTransport` is one method, `send(Request)`, answering a
`CompletableFuture<Response>`.

`verify-context` and `verify-full` are browser calls rather than client
methods, because the creator context describes the browser's own
connection, so only the browser being judged can make that call. Creating a
51Did is likewise not part of this client: creation is the cloud `json`
endpoint through the cloud request engine and pipeline.

The `pipeline.developer-examples.fodid` module holds a web example whose
`/redeem` route is these calls in a running server.

## Migrating from the blocking client

Earlier versions of `DidClient` blocked the calling thread on every call
that reached the cloud. Those methods are gone, each replaced by one of the
same name and arguments that answers through a `CompletableFuture`:

| Removed | Replaced by |
| --- | --- |
| `List<SigningKey> publicKeys()` | `CompletableFuture<List<SigningKey>> publicKeys()` |
| `SigningKey publicKeyFor(FodId)` | `CompletableFuture<SigningKey> publicKeyFor(FodId)` |
| `boolean verifySignature(FodId)` | `CompletableFuture<Boolean> verifySignature(FodId)` |
| `SignatureCheck verifySignatureDetailed(FodId)` | `CompletableFuture<SignatureCheck> verifySignatureDetailed(FodId)` |
| `boolean verify(FodId)` | `CompletableFuture<Boolean> verify(FodId)` |
| `boolean verify(String)` | `CompletableFuture<Boolean> verify(String)` |
| `RedeemResult redeem(FodId, String, String)` | `CompletableFuture<RedeemResult> redeem(FodId, String, String)` |
| `RedeemResult redeem(String, String, String)` | `CompletableFuture<RedeemResult> redeem(String, String, String)` |

None of them declares `IOException` any more, because the failure arrives
through the future. A caller that wants the old blocking behaviour calls
`join()` on the future, or `get()`, and takes the failure from the cause of
the `CompletionException` or `ExecutionException` that reports it.
`HttpTransport.send(Request)` likewise answers a
`CompletableFuture<Response>` rather than a `Response`, and does not throw
for a failure of the exchange. A transport of your own that blocks must
run its blocking call on a thread of its own and complete the future from
there.

## Migrating from the OWID library's removed API

Earlier OWID library versions let code build an `Owid` directly, parse one
with a throwing factory, and change its fields afterwards. The hardened
library removed all three, so code that reached those through this package
changes as follows.

Before:

```java
Owid owid = Owid.fromBase64(text);          // threw on bad input
FodId fodId = FodId.fromOwid(owid);

Owid built = new Owid(domain, Instant.now(), payload);
creator.sign(built);
```

After:

```java
FodIdParseResult read = FodId.tryFromBase64(text);   // never throws
if (read.isSuccess()) {
    FodId fodId = read.getValue();
}
// or keep the exception style, which makes the same read:
FodId fodId = FodId.fromBase64(text);

Owid signed = creator.createBytes(payload);          // stamped and signed
FodId fromSigned = FodId.fromOwid(signed);
```

`FodId.fromOwid(Owid)` still exists and still declares `OwidException`, so
callers compile unchanged, and since the library only hands out complete
signed envelopes there is no longer a copy made inside it.

## Non-goals

- **No signature verification on reading.** Reading a `FodId` does not check
  the signature. Call `verify(publicKeyPem)`, `verifyDetailed(publicKeyPem)`
  or `DidClient.verifySignature(fodId)` when needed.
- **No creation of new 51Dids.** This is a reader and a verifier; new 51Dids
  are issued by the 51Degrees cloud / on-premise hashing engines.
- **No upper bound on a payload.** The cloud owns the shape of anything past
  the match key, and this package does not second-guess it.
