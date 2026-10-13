# MCJUnitLib

A copy of [MCJUnitLib](https://github.com/alcatrazEscapee/mcjunitlib) by AlcatrazEscapee, used to run the 1.16.5 tests in `src/test`. It's MIT licensed, see `LICENSE.md`.

Copied from the `1.4.4-1.16.5` tag (`f7c43d1`). The upstream repository is archived, so changes are made here directly.

## Changes from upstream

- Unit test classes are found through FML's scan data, like integration tests, instead of selecting whole classpath roots. JUnit used to load every class in them, which logged an error for each client only class on the dedicated test server.
- A blank `MOD_MODULES`, which Forge Gradle sets when there are no modules, is skipped instead of being passed to JUnit as a module name.
- Anything thrown from an integration test, a scheduled action or an assertion fails that test with its own message. It used to be reported as `Reflection Error: null`, or crash the server when thrown from a scheduled action.
