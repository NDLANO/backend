# Learningsteps to learningpaths report

## Summary
- Moved learningstep storage/reads to `learningpaths.document` and started using embedded `learningsteps` in JSON.
- Updated write paths to rewrite the full learningpath document and increment path revision on step changes.
- Adjusted tests and fixtures to the new embedded model, removing step-table expectations.
- Kept external API behavior by indexing/serving only active steps while preserving deleted steps in the stored document.
- Added test JVM args for Mockito inline on JDK 21.
- Reverted guarded test behavior so Docker/port-bound tests run normally.

## Code changes
- `common/src/main/scala/no/ndla/common/model/domain/learningpath/LearningPath.scala`
  - JSON codec now includes `learningsteps`.
  - Decoder fills missing `learningsteps` with an empty array for older documents.
- `learningpath-api/src/main/scala/no/ndla/learningpathapi/repository/LearningPathRepository.scala`
  - Read paths now load steps from `learningpaths.document` only.
  - Added `withIdRaw` for unfiltered reads and `nextLearningStepId` using `learningsteps_id_seq`.
  - Insert writes embedded steps with generated IDs.
  - External link sample query now inspects `document.learningsteps`.
  - Simplified paged queries to select directly and preserve expected aliases/order.
- `learningpath-api/src/main/scala/no/ndla/learningpathapi/service/UpdateService.scala`
  - Step updates now rewrite the full learningpath document.
  - `updateSearchAndTaxonomy` indexes only active steps for API parity.
  - SeqNo updates and status changes now operate on embedded steps.
  - Guarded `withIdRaw` against nulls from mocks to avoid MatchError in tests.
  - New steps now get an initial revision when added to the document.
- `learningpath-api/src/main/scala/no/ndla/learningpathapi/service/ConverterService.scala`
  - `insertLearningStep` keeps deleted steps in the document; filtering is done at the API layer.
  - `asApiLearningStepV2` defaults missing step revision to 1 to avoid None.get.
- `modules/ScalacOptions.mill`
  - Added `-Djdk.attach.allowAttachSelf=true` to test JVM args for Mockito inline.
- `learningpath-api/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
  - Forced Mockito to use subclass mock maker to avoid JVM attach failures.
- `scalatestsuite/src/main/scala/no/ndla/scalatestsuite/DatabaseIntegrationSuite.scala`
  - Removed environment-based skip guard to restore normal container behavior.
- `testbase/src/main/scala/no/ndla/testbase/UnitTestSuiteBase.scala`
  - Removed port-allocation skip guard to restore normal test behavior.

## Test updates
- Updated learningpath/learningstep fixtures to use `Seq` instead of `Option` for `learningsteps`.
- Removed assertions and mocks expecting `insertLearningStep`/`updateLearningStep` calls.
- Fixed repository integration tests to work with embedded steps.
- Adjusted step/status/seqNo tests to expect single `update(learningpath)` behavior.
- Fixed minor test issues (typo `Sucess`, invalid `.map` on a step).

## Compile status
- `./mill -i learningpath-api.test.compile` passes.

## Test status
- `./mill -i learningpath-api.test` passes (run with escalated permissions for Docker/ports).

## Follow-ups
- Ensure migrations populate `learningpaths.document.learningsteps` before dropping the table.
- Update any remaining DB seeds (`local_testdata.sql`) if used by runtime tests.
