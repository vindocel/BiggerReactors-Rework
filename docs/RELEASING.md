# Releasing

All Minecraft targets are configured in `settings.gradle.kts`; their toolchains and optional APIs are pinned in `versions/<minecraft>/gradle.properties`. `production` holds development and prereleases. The author merges stable updates into `master`. The base mod version lives in the root `gradle.properties`; a release builds with the full version from its tag.

## Select targets and channel

| Example tag | Channel | Minecraft targets |
| --- | --- | --- |
| `v0.1.7-beta.1` | Beta | All configured targets |
| `v0.1.7-beta.1+1.21.1-26.3` | Beta | 1.21.1 and 26.3 |
| `v0.1.7+26.1.2` | Stable | 26.1.2 |
| `v0.1.7` | Stable | All configured targets |

Examples describe the syntax, not a request to republish an existing version. Use a new channel/version as appropriate. Alpha and beta numbers are positive integers. Repeated or unknown Minecraft targets, mismatched base versions and inconsistent GitHub prerelease flags are rejected. Stable releases must belong to `master`; alphas and betas must belong to `production`.

## Publish a release

The author creates the tag and publishes its GitHub Release. **A tag alone does not upload files.** Mark alpha/beta releases as Pre-release. Use the release body for public release notes. The workflow checks out that exact tag and resolves all selected targets dynamically.

Every selected artifact must pass its build, unit tests and clean GameTests before any store upload begins. Distribution artifacts are validated by Minecraft range and full release version, then transferred to the publication jobs. Store versions use `v<version>+<minecraft>` so files remain distinct. Existing versions are skipped per store; failed lookups stop uploading. A partially completed store upload is not an atomic transaction; rerunning the workflow checks existing versions before retrying missing ones.

The `management` environment keeps the existing `ID_DO_CURSEFORGE` and `ID_DO_MODRINTH` variables and `CURSEFORGE_TOKEN` and `MODRINTH_TOKEN` secrets. CI and pull requests have no publishing secrets. The current CurseForge lookup uses the public CFWidget API; unavailable or invalid responses stop publishing. Publication must be verified on both stores after an actual release.

## Local validation

Run Gradle with Java 25; the 1.21.1 target uses its Java 21 toolchain. On Windows:

```powershell
.\gradlew.bat :1.21.1:build :26.1.2:build :26.3:build
.\gradlew.bat :1.21.1:runGameTestServer :26.1.2:runGameTestServer :26.3:runGameTestServer
python -B scripts/test-release.py
python -B scripts/test-published.py
```

The release tests use mocked Git/store responses and do not publish. Bash, Python and unzip are needed for release validation; GitHub Ubuntu runners provide them. Use an explicit `:<minecraft>:` task path for clients so a generic task does not launch multiple games. Keep Stonecutter's active/VCS version at 26.1.2 when finishing edits.
