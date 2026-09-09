# Customer Book

Tenant-scoped address book for billable people and companies.

## Scope

Owns:

- `customer`: the only future order target; composite PK `(organization_id, customer_id)`.
- `customer_business_contact`: people inside a business; never customers/order targets.
- `INDIVIDUAL`/`BUSINESS` discrimination and one-way archival.

Excludes orders and tenant membership/roles ([Organization Management](organization-management.md)). Here “organization” means the Mesazon tenant; every endpoint uses `X-Organization-ID`.

## Model and invariants

`customer` holds `customer_type`, shared `name`, `emails`/`phone_numbers` JSONB lists, address, optional business-only `tax_id`, `status`, and audit timestamps. `customer_business_contact` holds `(organization_id, customer_id, customer_business_contact_id)`, name, role, optional email/phone, and a tenant-scoped FK to `customer`.

- One row + discriminator makes a customer exactly one type. Typed reads/updates filter `customer_type`; wrong-type lookup is not found.
- Repository views: `CustomerIndividualDetailsRow` (`fullName`) and `CustomerBusinessDetailsRow` (`businessName`, `taxID`); no `CustomerRow`. `CustomerSummaryRow(customerID, name, customerType)` is a projection.
- Contacts exist only for businesses by service convention; the FK enforces tenant, not subtype.
- Customers store email/phone lists. Each entry contains value + `isDefault`; empty is valid, non-empty requires exactly one default. Business contacts retain one optional email/phone.
- Validation accumulates all list errors. `InvalidFieldError.index` identifies the item. Batch customer errors use the outer customer index while preserving nested contact indexes.
- `customer_status` is the native PG enum `Active|Archived`; queries require the casts in [Repository flow](flow/04-repository.md#queries).
- Customers archive; contacts hard-delete. Archive retains contacts. No unarchive.

Named uniqueness:

| Constraint/index | Rule | 409 message |
|---|---|---|
| `uq_customer_name` | active `(organization_id, customer_type, name)`; partial on `status = 'Active'` | `A customer with the given name already exists in this organization` |
| `uq_customer_business_contact_email` | `(organization_id, customer_id, email)` | `A business contact with the given email already exists for this customer` |
| `uq_customer_business_contact_phone_number` | `(organization_id, customer_id, phone_number_e164)` | `A business contact with the given phone number already exists for this customer` |

Nullable contact fields allow multiple `NULL`s. Repository maps only SQL state `23505` + these names to `ConflictError.UniqueConstraintViolation`.

## Endpoints

Service: `CustomerBookService`; bearer + completed onboarding. Reads allow `OWNER|ADMIN|USER`; writes allow `OWNER|ADMIN`.
Smithy JSON requests are limited to 5 MiB by `HttpApp.SmithyMaxEntitySize`.

| Method | Path | Operation | Result/effect |
|---|---|---|---|
| GET | `/get/customer-individual/{customerID}` | `GetCustomerIndividualGet` | individual details |
| GET | `/get/customer-business/{customerID}` | `GetCustomerBusinessGet` | business details |
| GET | `/get/customers` | `GetCustomersGet` | active summaries |
| POST | `/insert/customer-individual` | `InsertCustomerIndividualPost` | one individual, returns its full row |
| POST | `/insert/customer-individuals` | `InsertCustomerIndividualsPost` | atomic batch, returns a summary per row in request order |
| PUT | `/update/customer-individual` | `UpdateCustomerIndividualPut` | update active individual |
| POST | `/insert/customer-business` | `InsertCustomerBusinessPost` | one business + inline contacts, returns the full row incl. generated contact IDs |
| POST | `/insert/customer-businesses` | `InsertCustomerBusinessesPost` | atomic batch, returns a summary per row in request order |
| PUT | `/update/customer-business` | `UpdateCustomerBusinessPut` | update active business |
| POST | `/insert/customers` | `InsertCustomersPost` | atomic mixed batch, returns individual summaries then business summaries, each in request order |
| PUT | `/add/customer-business-contacts` | `AddCustomerBusinessContactsPut` | append contacts |
| PUT | `/remove/customer-business-contacts` | `RemoveCustomerBusinessContactsPut` | hard-delete contacts |
| PUT | `/archive/customer` | `ArchiveCustomerPut` | archive either type |

Smithy: `smithy/CustomerBookService.smithy`, `smithy/domain/CustomerBook.smithy`. Each operation owns its shapes.

Error sets:

- Reads, remove contacts, archive: `BadRequest, Unauthorized, Forbidden, InternalServerError`.
- Other writes: above plus `ValidationError` and `Conflict`.
- Remove/archive contain only pure UUIDs, cannot validation-fail, and cannot create uniqueness conflicts.
- By-ID reads filter type, not status: archived rows still return; missing/wrong type → 500. Mutations use the lenient policy below.

## Flow and decisions

- Insert individual: `customer(type=INDIVIDUAL,status=Active,tax_id=NULL)`.
- Insert business: `customer(type=BUSINESS,status=Active)` plus inline contacts in the same transaction.
- Batch/mixed insert: multi-row statements, one `transactionOrWiden`, all-or-nothing. IDs/timestamps are generated in the repository.
- All 5 insert operations return `200` with the row(s) just persisted instead of `204`: repository insert methods return `CustomerIndividualDetailsRow`/`CustomerBusinessDetailsRow` (+ contact rows, paired via a named tuple `CustomerBusinessInsertRow`) rather than bare IDs, and the service maps them into per-operation response shapes. Singular individual/business inserts return the full row; batch/mixed inserts return a `GetCustomer`-shaped summary (`customerID`, `name`, `customerType`) per item in request order — for the mixed endpoint, individuals then businesses.
- Update: one type- and `Active`-filtered update. Required email/phone lists always overwrite (`Some(list)`); optional scalars use `...OptUpdate` (absent = unchanged).
- Add/remove contacts: transaction first checks `customerActiveExists`; archived/absent parent → silent `204` no-op.
- Archive: type-independent `Active → Archived`; missing/already archived → silent `204`; retains contacts. Partial uniqueness frees the name.
- After archive, update/contact/archive mutations silently no-op. This intentionally differs from by-ID reads: racing archive already satisfies the mutation’s desired outcome.
- `GetCustomersGet`: active rows, SQL order `LOWER(name), customer_id`. No expression index yet; tenant PK narrows rows. Add `(organization_id, lower(name))` only if pagination/scale warrants it.
- Org isolation comes from composite keys/FKs. Future order history must retain customer rows and snapshot buyer fields; use `on delete restrict`.

Repository inputs never use API request types. `CustomerBookRepository` owns batch element inputs in its companion; singular operations reuse them, while single-only updates/removal use flat parameters. Service maps validated request → input with Chimney. JSONB Row fields and named codecs use `List[CustomerEmailEntryInput]` / `List[CustomerPhoneNumberEntryInput]`; `CustomerBookQueries` imports `CustomerBookRepository.*` and `io.github.iltotore.iron.jsoniter.given`.

Open decisions:

- Archive keeps contacts; revisit only if clients must hide them.
- No unarchive; reactivation must resolve active-name conflict.
- Singular, batch, and mixed inserts overlap; retain until client needs justify convergence.

## Photo extraction

`POST /extract/customer-book-photo` is a Tapir streaming endpoint, not Smithy — same reason and same transport as [Organization Management](organization-management.md#logo-upload)'s logo upload and [Catalogue](catalogue.md#image-upload)'s item-image upload: Smithy JSON routes cap at 5 MB, Tapir streams binary and allows 20 MB. See [Alternate HTTP](../project/alternate-http.md) for the shared transport mechanics this endpoint follows.

Binary body; organization in the `X-Organization-ID` header. Security (`AuthorizationService.auth`): valid access JWT, `OnboardStage.completedStages`, and the caller must be `OWNER` or `ADMIN` in the organization — identical gate to [adding a customer](../../pages/epics/05-customer-book.md#1-user-adds-a-customer). No `X-File-Name` header: unlike the logo/catalogue-item uploads, nothing here is ever kept, so there is no original file name to preserve.

`FileService.extractCustomersFromPhoto` runs inside one `ZIO.scoped` block, reusing the existing upload pipeline pieces but stopping short of storage:

1. `FileScanner.scan` spools the incoming `ZStream[Byte]` to a temp file exactly as the two existing uploads do — same `SupportedMediaType.images` (`PNG`, `JPEG`, `WEBP`) content-sniffed check, same `fileServiceConfig.maxUploadBytes` cap. It returns `FileScannerScanOutput` (`utils/utils.scala`: `fileByteStreamScanned`, the matched `SupportedMediaType` member, `fileBytesSize: FileBytesSize`) — the media type and size it already computed for its own checks, not re-detected by any caller. The logo/catalogue-item uploads destructure this and use only `fileByteStreamScanned`, unchanged from before; this endpoint is the first caller that also needs `supportedMediaType`.
2. Unlike the logo/catalogue-item uploads, there is no `ImageProcessing.normalize` step and no `S3Client` call: `FileService` passes `fileByteStreamScanned` straight through to `AIClient`, never writing it to object storage or resizing it.
3. `AIClient.extractFromImage[A](imageByteStream: FileByteStreamScanned, supportedMediaType: SupportedMediaType, instructions: String)(using Schema[A], JsonValueCodec[A])` consumes that stream **exactly once** — unwrapping and collecting it, then base64-encoding it — mirroring how `S3ClientOrganizationMedia`'s upload methods take a `ZStream`-wrapping type and do their own internal consumption. Typing the parameter as `FileByteStreamScanned` (not a bare `ZStream`) makes it a compile-time guarantee that only an already-scanned stream can reach `AIClient`. The media type comes from `FileScanner.scan`'s output via `FileService`, not detected inside `AIClient`. It sends the base64 image (built from `supportedMediaType.mime`) plus a system prompt (the `instructions` argument, owned by `FileService`, not `AIClient`) describing the extraction task (classify each recognized entry as an individual or a business; a candidate needs at least a name; note anything unclear on that candidate; flag same-kind same-name duplicates found within this one photo, never against the stored book; report how many entries were identified versus turned into candidates; summarize, in one line, what could not be processed) as an OpenAI structured-output request (`ResponseFormat.JsonSchema`, same mechanism `OpenAIClient` already uses) targeting `ExtractCustomersFromPhotoResponse` directly.
4. The AI's structured response is returned to the caller as-is: `Entries Identified`, `Entries Processed`, `Is Duplicate`, and the unidentified-entries summary are the model's own best-effort output, not recomputed or cross-checked by the service. Nothing is written to `customer`/`customer_business_contact` or anywhere else; the step is fully stateless and safe to repeat.

`CustomerIndividualCandidate`/`CustomerBusinessCandidate` each wrap the real `InsertCustomerIndividualPostRequest`/`InsertCustomerBusinessPostRequest` domain type (the same Iron-refined fields step 1 validates against) alongside `isDuplicate`/`extractionNotes`, plus `entriesIdentified`/`entriesProcessed`/`unidentifiedEntriesSummary` at the response's top level. A clean candidate can be forwarded into [step 1](../../pages/epics/05-customer-book.md#1-user-adds-a-customer) without edits; the AI is prompted to produce realistic values but nothing here re-validates them.

**Accepted trade-off:** because the whole response is one structured-output JSON document decoded in a single pass, a single field that fails its Iron constraint anywhere in that document (e.g. one malformed email on one of several candidates) fails the entire decode, surfacing as one `500 INTERNAL_SERVER_ERROR` for the whole request rather than dropping just that field or candidate. OpenAI's structured-output "strict" mode reliably enforces JSON structure/types but not Iron's string `pattern` constraints during generation, so this is a real (if expected to be uncommon) failure mode, chosen deliberately over the added complexity of a lenient per-field fallback.

### Key files (photo extraction)

- Orchestration: `service/FileService.scala` (shared with logo/catalogue-item image uploads)
- AI client: `clients/AIClient.scala` (new, parallel to `clients/OpenAIClient.scala` — that client and its config are untouched), `config/AIClientConfig.scala`
- Pipeline utils (shared): `utils/FileScanner.scala`
- Transport (shared): `tapir/FileServiceEndpoints.scala`, `tapir/tapir.scala`
- Domain: `domain/gateway/CustomerBook.scala` (`CustomerIndividualCandidate`, `CustomerBusinessCandidate`, `ExtractCustomersFromPhotoResponse`)
- JSON: `json/json.scala` (`Schema`/`JsonValueCodec` givens for `ExtractCustomersFromPhotoResponse`, plus the bottom-up nested-type givens it needs)
- Config: new `ai-client` section, both core/gateway-it `application.conf` copies (separate from `open-ai-client`)

### Tests (photo extraction)

`it/AIClientSpec.scala` is the real integration spec for `AIClient`, against a wiremock-stubbed OpenAI chat-completions endpoint (mirrors `TwilioClientSpec`, per [External client](../project/external-client.md)). Wiremock has no dynamic stub API here — its stubs are the static mapping files in `backend/wiremock/mappings/` baked into the `local/wiremock:latest` image — so the three `AIClientSpec` cases are distinguished by matching on request-body markers: a distinct `instructions` string per case (`AI_CLIENT_SPEC_SUCCESS`/`AI_CLIENT_SPEC_ERROR`/`AI_CLIENT_SPEC_MALFORMED`) plus, for the success mapping, requiring the `image_url`/`json_schema` substrings the real request body must contain — proving the outbound request carries the image content part and requests structured output, without needing to add body capture to the shared `WiremockClient` harness. An earlier `unit/clients/AIClientSpec.scala` (a plain, no-dependency test of the then-stub `AIClient.live`) existed only as a stand-in until this real spec landed and has been deleted now that it has.

Target coverage, ground each case in the epic's own [Business Scenarios table](../../pages/epics/05-customer-book.md#7-user-extracts-customers-from-a-photo) rather than a generic success/failure pair — every numbered scenario there (clear entries, an entry with something unclear, no legible name, same-kind duplicate names, different-kind same names, nothing recognizable, unsupported/oversized file, AI unreachable, disallowed role, repeat calls) is a candidate test case once the real orchestration lands:

- Acceptance: `FileApiSpec`'s `/extract/customer-book-photo` block — happy path against a wiremock-stubbed AI response, missing token (401), invalid token (401), disallowed stage (403), missing `X-Organization-ID` header (400), non-member (500), disallowed role (403), unsupported file type (500), AI-service failure (500)
- Functional: `FileServiceSpec`'s `extractCustomersFromPhoto` block covers the success path, `FileScanner` failure propagating with `AIClient` never called, and `AIClient` failure propagating. `fileScannerMock` is a normal ScalaMock mock; `AIClient` is `Mocks.AIClientMock` (`mock/Mocks.scala`, a hand-written test double, not `mock[AIClient]`) because ScalaMock cannot mock its generic `extractFromImage[A](...)(using Schema[A], JsonValueCodec[A])` shape — see [Functional testing](../project/functional-testing.md)'s "Known limitation" section for the full writeup.
- Integration: `it/AIClientSpec.scala` (mirrors `TwilioClientSpec`) against `src/test/resources/compose/wiremock.yaml` and the new `backend/wiremock/mappings/ai-client-chat-completions*.json` stubs — success decode, HTTP/network failure, and the "accepted trade-off" malformed-JSON case above
- Unit: `FileScannerSpec` (shared, unchanged — this endpoint adds no new size/type-check behavior to cover)

**Status (photo extraction):** `FileService.extractCustomersFromPhoto` is fully implemented (scan → `AIClient.extractFromImage` → return the AI's response as-is) and covered by three green `FileServiceSpec` cases. `AIClient` (`clients/AIClient.scala`) is fully implemented and covered by `it/AIClientSpec.scala` (success, AI-service-error, and malformed-JSON cases against wiremock), with its own `config/AIClientConfig.scala` and a new `ai-client` section in core's `application.conf` (`open-ai-client`/`OpenAIClient` remain untouched). `json/json.scala` carries `Schema`/`JsonValueCodec` givens for `ExtractCustomersFromPhotoResponse`, derived bottom-up per nested type (a direct top-level `Schema.derived` failed to resolve `List[CustomerIndividualCandidate]`; explicit givens for every nested type from `PhoneNumber` up resolved it). `Main.scala` now provides `AIClientConfig.live`/`AIClient.live` (mirroring `OpenAIClientConfig.live`/`OpenAIClient.live`) since `FileService.live`'s layer graph requires them once `FileServiceImpl` takes `AIClient` as a constructor dependency — this was a compile-time-forced addition, not new design. `POST /extract/customer-book-photo` is now defined and routed in `tapir/FileServiceEndpoints.scala` (binary body in, `jsonBody[ExtractCustomersFromPhotoResponse]` out, same security/error pattern and OpenAPI registration as the other two uploads) and is reachable over HTTP calling the real implementation. Remaining: gateway-it's `application.conf`/`compose.yaml` don't point `AIClient` at wiremock yet, and there is no acceptance coverage (`FileApiSpec`) for this endpoint yet — those land in their own remaining slice.

## Key files and config

- Contract: `smithy/CustomerBookService.smithy`, `smithy/domain/CustomerBook.smithy`
- Domain/validation: `domain/gateway/CustomerBook.scala`, shared `Newtypes.scala`, `validation/service/CustomerBookRequestValidator.scala`
- Service: `service/CustomerBookService.scala`
- Persistence: `repository/CustomerBookRepository.scala`, `repository/domain/Customer*Row.scala`, `repository/domain/CustomerSummaryRow.scala`, `repository/queries/CustomerBookQueries.scala`
- Schema: `backend/schemas/migrations/V2025.05.27__init.sql`
- Config: `RepositoryConfig` and both core/gateway-it `application.conf` copies

## Status

Implementation, wiring, schema, validation, repository, functional tests, and repository tests are complete. Acceptance: 8/13 endpoints complete.

| Acceptance done | Remaining |
|---|---|
| four singular/batch inserts; three reads; archive | mixed insert; two updates; add/remove contacts |

`Main` provides service, validator, repository, and queries; `HttpApp.externalSmithyRoutes` serves the contract.

## Tests

- Unit: `CustomerBookRequestValidatorSpec` — every validator, accumulation, nested indexes/default rules.
- Integration: `CustomerBookRepositorySpec` — every repository operation; three conflicts; atomic rollback; enum/partial-index semantics; archived mutation guards; whole-row IDs/timestamps.
- Functional: `CustomerBookServiceSpec` — exact org-scoped calls/mappings/responses; validation blocks repository; repository errors propagate.
- Acceptance: `CustomerBookApiSpec` — per completed endpoint, the [acceptance-testing matrix](../project/acceptance-testing.md), full DB state, and no forbidden effects. Also proves duplicate/contact conflicts, by-ID missing → 500, archive state flip, and missing archive → 204.

Structural type exclusivity needs no dedicated “not both” test. Repository tests instead prove cross-type same names are valid, archived names are reusable, and typed reads do not miss rows.
