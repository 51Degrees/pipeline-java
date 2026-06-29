# pipeline.did

Strongly typed Java reader for the 51Did (51Degrees Identifier) returned by
the 51Degrees Cloud service. Mirrors the .NET `FiftyOne.Did` package.

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

| Bits 7-6 | `IdType`        | Value length | Minimum payload |
|---------:|-----------------|-------------:|----------------:|
|     `00` | `PROBABILISTIC` |           32 |              37 |
|     `01` | `RANDOM`        |           16 |              21 |
|     `10` | `HASHED_EMAIL`  |           32 |              37 |
|     `11` | `RESERVED`      |    remainder |               5 |

Identifiers issued before the type tag existed have bits 6-7 zeroed and decode
as `PROBABILISTIC`.

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
boolean verified = fodId.verify(publicKeyPem);
String  base64   = fodId.asBase64();
```

## Comparing two 51Dids

```java
FodId a = FodId.fromBase64(idprobglobalA);
FodId b = FodId.fromBase64(idprobglobalB);

// The envelope (date, signature, base64) differs across reissues.
// The value inside the payload is stable - this is what you compare:
boolean sameValue = java.util.Arrays.equals(a.getHash(), b.getHash());
```

Use `getHash()` as the cache / dedup key.

## Non-goals

- **No signature verification on construction.** Constructing a `FodId` does
  not check the signature. Call `verify(publicKeyPem)` when needed.
- **No creation of new 51Dids.** This is a parser; new 51Dids are issued by the
  51Degrees cloud / on-premise hashing engines.
