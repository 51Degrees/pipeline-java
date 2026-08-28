# 51Did examples

This module holds the developer examples for the 51Did package
(`pipeline.did`). There are two programs, both in the package
`pipeline.developerexamples.fodid`.

| Program | What it shows |
| --- | --- |
| `Main` | Reads a 51Did offline. Builds a sample identifier in process, parses it back with `FodId` and shows that the value is stable while the envelope changes on every issue. Needs no cloud access. |
| `CreatorContextDemoServer` | Serves a small web page that creates a 51Did in the browser, verifies it from the browser, and redeems the encrypted creator context result on this server, which is the only place the licence key lives. |

## Creator context

Every 51Did the 51Degrees cloud issues carries a creator context, which
binds the identifier to the browser and connection it was created on.
The creator context only makes sense from a browser, because a program
verifying its own connection checks itself against itself, so the demo
is a web page and a server. The flow against the cloud has three steps:

1. **Create** a 51Did by calling the `json` endpoint, which issues an
   identifier for the calling connection.
2. **Verify** it with `verify-full`, which returns both the signature
   outcome and the creator context verdict only as an encrypted
   `result` that the caller cannot read or forge. (A deployment
   holding no context secret answers in the open instead.)
3. **Redeem** the encrypted result with `redeem`, presenting the 51Did,
   the encrypted result and the account's licence key, and receive the
   true creator context verdict, when the verification happened
   (`verifiedAt`) and how long ago that was (`secondsSinceVerified`).

Steps 1 and 2 run in the visitor's browser, so the cloud observes the
browser's live connection, and the page relays the encrypted result to
your server. Step 3 runs on your server, which is the party holding
the licence key. A single-use `challenge` issued by the server per page
load is bound through both steps by the cloud.

The demo server uses nothing outside the standard library. Production
code should use a JSON library rather than the small regex helper the
page and server keep for the demo.

### What you copy into your own server

The only server-side part of the flow is the redeem call, which adds
the licence key the browser never sees. The `redeem` handler in
`CreatorContextDemoServer.java` is that call, and these are its
essential lines, where `API` is the cloud API base, `RESOURCE` the
resource key, `LICENCE` the licence key, and the three query values
are what the page passed on from `verify-full`:

```java
String upstream = API + "id/redeem/" + RESOURCE
    + "?51did=" + valueOr(query, "51did")
    + "&result=" + valueOr(query, "result")
    + "&challenge=" + valueOr(query, "challenge")
    + "&license=" + encode(LICENCE == null ? "" : LICENCE);
```

The handler fetches that URL and relays the status, content type and
body of the answer to the page exactly as received. The answer carries
`signature`, `context`, `verifiedAt` and `secondsSinceVerified`. A
production server would also remember the challenge it issued and
reject a redemption carrying any other, which the demo keeps out of
scope.

### Environment variables

| Variable | Meaning |
| --- | --- |
| `_51DEGREES_RESOURCE_KEY` | Required. The resource key of the page, public by nature. The older name `RESOURCE_KEY` is read when this one is not set. |
| `_51DEGREES_LICENSE_KEY` | Optional. A licence key of the same account, server side only. The older name `LICENSE_KEY` is read when this one is not set. Only an account that holds licence keys needs one to redeem, because the licence key is what keeps redemption to the acting party's own servers, so an account holding none redeems without one. |
| `FOD_CLOUD_API_URL` | Optional. The cloud API base including the `/api/v4/` segment, defaulting to `https://cloud.51degrees.com/api/v4/`. This is the same variable the cloud request engine in this repository honours, so setting it once points every 51Degrees example at the same place. A host other than cloud.51degrees.com would be used to (a) use an on premise web server, or (b) use a privately hosted version of the 51Degrees cloud for performance reasons, which is the private hosting option of the cloud service. Both run the same service, so the examples work unchanged. |
| `PORT` | The port to listen on, defaulting to `5100`. |

### How to run

With Maven, from the root of the repository, build the module and run
the demo server by its class name:

```sh
mvn -pl pipeline.developer-examples/pipeline.developer-examples.fodid -am -DskipTests compile
mvn -pl pipeline.developer-examples/pipeline.developer-examples.fodid exec:java -Dexec.mainClass=pipeline.developerexamples.fodid.CreatorContextDemoServer
```

Because the demo server uses nothing outside the standard library, it
can also be compiled and run with the JDK alone from this module's
folder. It needs `src/main/resources` on the classpath, where the page
and stylesheet live under `fodid/creator-context/`:

```sh
javac -d target/classes src/main/java/pipeline/developerexamples/fodid/CreatorContextDemoServer.java
java -cp "target/classes;src/main/resources" pipeline.developerexamples.fodid.CreatorContextDemoServer
```

On Linux and macOS the classpath separator is `:` rather than `;`.

The demo server prints the address to open, `http://localhost:5100/`
by default. A creator context verdict of `nocontext` is a normal
outcome rather than an error, because a self-hosted container may be
configured not to emit the creator context, so an identifier it issued
has none to check, and the page shows it as the verdict. A 404 from
`verify-full` or `redeem` means the host answering does not support
the creator context at all, and the page reports the check as not
supported by this host. Any other status outside 2xx, or a body that
is not JSON, is shown on the page as a failure naming the status and
what the service said.

### What a run costs

Every call the demo makes to the cloud is one use against the
subscription behind the resource key. Checking a 51Did from the
browser makes two, verify-full from the page and redeem from the
server, so a browser-based context check is two uses every time.
Checking only the signature with `verify` is one use.

### The web demo, and the copy-and-paste proof

The demo server serves `page.html`, injecting a fresh challenge per
page load, and holds the licence key. The browser creates the 51Did
and calls `verify-full`, the first verification step, so the cloud
observes the browser's live connection, then the page hands the
encrypted result to its own server, which redeems it with the licence
key as the second step.

The creation call requests every 51Did identifier in one request, and
the page shows all six in a table: the probabilistic pair
(`IdProbGlobal` and `IdProbLic`) derived from the connection, the
deterministic hashed-email pair (`IdHemGlobal` and `IdHemLic`) derived
from email evidence supplied as `id.email` (the demo sends
`demo@51did.example`, so the pair is the same on every device that
email appears on), and the random pair (`IdRandGlobal` and
`IdRandLic`). Global identifiers are shared across customers, licensed
ones are scoped to the licence key. The verification and creator
context flow then carries the licensed probabilistic identifier through
both steps, or the global one where the account holds no licence keys.

Once the 51Did has fully validated, the page shows a **copy-and-paste
section** with a link carrying the same 51Did, and an explanation of
what will happen next. Open that link in a **different browser** and
the same page loads with the same identifier: the signature still
verifies and the identifier unpacks, because it is genuine, but the
creator context does **not** validate, because the context binds the
identifier to the browser and connection it was created on. That
visible failure is the demonstration that matters, a copied or stolen
identifier caught at presentation with nothing stored server side.
Opening the link in the same browser is not the demonstration, since
the same browser presents the same context and may still verify.

To demonstrate across two devices, serve on an address both can reach
and open the copied link on the second device.

### The stylesheet

The vendored `examples-main.min.css` beside `page.html` under
`src/main/resources/fodid/creator-context/` is the design system build
and is refreshed by the `update-example-assets` step of common-ci.
