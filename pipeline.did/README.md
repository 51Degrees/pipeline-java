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
- The **value** is the stable, comparable part of the payload after the Flags
  and License Id: a 32-byte SHA-256 for Probabilistic and HashedEmail
  identifiers, or 16 GUID bytes for Random. Two 51Dids for the same inputs
  share the same value even though their envelopes differ.

**Comparing two 51Dids means comparing their values, never their envelopes.**

## Payload layout

The header is shared by every identifier type; bits 6-7 of Flags select the
type and the length of the value that follows.

| Offset | Length | Field      | Type                                            |
|-------:|-------:|------------|-------------------------------------------------|
|      0 |      1 | Flags      | uint8: bits 0-2 usage, bits 6-7 identifier type |
|      1 |      4 | LicenseId  | uint32 (little-endian)                          |
|      5 |  16/32 | Value      | SHA-256 (Probabilistic, HashedEmail) or GUID (Random) |
|  after |    any | Context    | Optional creator context section, readable only by 51Degrees |

| Bits 7-6 | `IdType`        | Value length | Minimum payload |
|---------:|-----------------|-------------:|----------------:|
|     `00` | `PROBABILISTIC` |           32 |              37 |
|     `01` | `RANDOM`        |           16 |              21 |
|     `10` | `HASHED_EMAIL`  |           32 |              37 |
|     `11` | `RESERVED`      |    remainder |               5 |

Identifiers issued before the type tag existed have bits 6-7 zeroed and decode
as `PROBABILISTIC`.

An identifier carrying a creator context is longer than the minimum, and on
such an identifier the four License Id bytes hold an encrypted value that
only 51Degrees can turn back into a licence identifier, so `getLicenseId()`
is the field's raw value and identifies nothing outside 51Degrees.

## OWID dependency

`FodId` builds on the OWID envelope library
([SWAN-community/owid-java](https://github.com/SWAN-community/owid-java),
package `com.swancommunity.owid`). Because that library's `Owid` type is
`final`, `FodId` **composes** an OWID (holds one and delegates OWID-level
concerns to it) rather than inheriting from it.

The OWID source is consumed from a git submodule of the 51Degrees fork at the
repository root (`owid-java/`, mirroring how `pipeline-dotnet` carries the
`owid-dotnet` submodule) and compiled into this module at its Java 8 level, so
there is no separate runtime dependency. The vendored OWID sources keep their
Apache-2.0 headers; the 51Did sources are EUPL-1.2.

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

## Usage

```java
import fiftyone.pipeline.did.FodId;
import fiftyone.pipeline.did.IdType;

FodId fodId = FodId.fromBase64(base64FromCloudService);

int    flags     = fodId.getFlags();
IdType type      = fodId.getType();        // PROBABILISTIC / RANDOM / HASHED_EMAIL
long   licenseId = fodId.getLicenseId();
byte[] hash      = fodId.getHash();        // SHA-256 or GUID bytes, see type

// Delegated OWID-level fields and operations.
String  domain   = fodId.getDomain();
long    minutes  = fodId.getDateMinutes(); // the envelope's own date field
boolean verified = fodId.verify(publicKeyPem);
String  base64   = fodId.asBase64();       // standard alphabet, padded
String  forUrl   = fodId.asBase64Url();    // URL-safe alphabet, no padding
```

`fromBase64` accepts the standard alphabet the cloud issues (`+`, `/`,
padded) and the URL-safe alphabet a page puts in a link (`-`, `_`, padding
optional). `asBase64Url()` gives the URL-safe form back, so an identifier
can go into a URL without any conversion by the caller.

## Comparing two 51Dids

```java
FodId a = FodId.fromBase64(idprobglobalA);
FodId b = FodId.fromBase64(idprobglobalB);

// The envelope (date, signature, base64) differs across reissues.
// The value inside the payload is stable - this is what you compare:
boolean sameValue = java.util.Arrays.equals(a.getHash(), b.getHash());
```

Use `getHash()` as the cache / dedup key.

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

1. **Parse.** The identifier arrives from a page in the URL-safe alphabet.

   ```java
   FodId fodId = FodId.fromBase64(fromThePage);
   ```

2. **Verify offline.** The client fetches the cloud's signing keys once,
   holds them, and checks the signature against the key in force when the
   identifier was created. No use is charged.

   ```java
   boolean genuine = client.verifySignature(fodId);
   // or, to learn why not:
   DidClient.SignatureCheck check = client.verifySignatureDetailed(fodId);
   ```

   `publicKeys()` returns the held list and `publicKeyFor(fodId)` the key in
   force at the identifier's date. The list is refetched, once, when it has
   no key for the date, when the date is later than the newest start held,
   or when the list is more than a day old.

3. **Verify through the cloud.** The open verify endpoint, one use against
   the resource key, needing no licence key.

   ```java
   boolean genuine = client.verify(fodId);
   ```

4. **Redeem.** A page checks the creator context from the browser with
   `verify-full` or `verify-context` and relays the sealed `result` to your
   server. Your server redeems it, with the licence key, against the
   identifier it knows independently. One use against the resource key.

   ```java
   RedeemResult redeemed = client.redeem(fodId, result, challenge);
   switch (redeemed.getContext()) {
       case VERIFIED:      // presented from where it was created
       case MISMATCH:      // redeemed.getFactors() says which factor differs
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
   ```

   A malformed identifier raises `IllegalArgumentException` with the
   cloud's message, a host without the creator context raises
   `DidNotSupportedException`, any other status raises `DidHttpException`
   carrying the status and body, and an unreachable cloud raises
   `IOException`. Every cryptographic failure comes back as the one word
   `unreadable`, by design, so the client does not try to distinguish them
   either.

`verify-context` and `verify-full` are browser calls rather than client
methods, because the creator context describes the browser's own
connection, so only the browser being judged can make that call. Creating a
51Did is likewise not part of this client: creation is the cloud `json`
endpoint through the cloud request engine and pipeline.

The `pipeline.developer-examples.fodid` module holds a web example whose
`/redeem` route is these calls in a running server.

## Non-goals

- **No signature verification on construction.** Constructing a `FodId` does
  not check the signature. Call `verify(publicKeyPem)` or
  `DidClient.verifySignature(fodId)` when needed.
- **No creation of new 51Dids.** This is a parser and a verifier; new 51Dids
  are issued by the 51Degrees cloud / on-premise hashing engines.
