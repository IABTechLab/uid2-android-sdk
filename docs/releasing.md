Releasing
=========

Releasing is easiest performed via the `release` [GitHub workflow](https://github.com/IABTechLab/uid2-android-sdk/actions/workflows/release.yml).
Click the 'Run workflow' button (on the right), and then enter the new version number. If the snapshot version number also
needs to be updated, you can provide that too. Click 'Run workflow', and a release will be performed.

Once complete, a new GitHub [release](https://github.com/IABTechLab/uid2-android-sdk/releases) will be created.
You can go ahead and edit the release notes if required.

### Manual release

The workflow mentioned above calls the `scripts/release.sh` script. You can call the script manually, but you need to
ensure that the correct environmental variables are set:

```shell
export ORG_GRADLE_PROJECT_mavenCentralUsername=FOO
export ORG_GRADLE_PROJECT_mavenCentralPassword=BAR
export ORG_GRADLE_PROJECT_signingInMemoryKey=...
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...

# Release v1.0.0, and use 1.1.0-SNAPSHOT for the next development version
./scripts/release.sh 1.0.0 1.1.0-SNAPSHOT
```
