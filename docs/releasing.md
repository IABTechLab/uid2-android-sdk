Releasing
=========

Cutting a Release
-----------------

1. Update `CHANGELOG.md`.

2. Set versions:

    ```
    export RELEASE_VERSION=X.Y.Z
    export NEXT_VERSION=X.Y.Z-SNAPSHOT
    ```

3. Update versions:

    ```
    sed -i "" \
      "s/ version = \'.*\'/ version = \'$RELEASE_VERSION\'/g" \
      build.gradle
    ```

4. Tag the release and push to GitHub.

    ```
    git commit -am "Prepare for release $RELEASE_VERSION."
    git tag -a r$RELEASE_VERSION -m "Version $RELEASE_VERSION"
    git push && git push --tags
    ```

5. Wait for [GitHub Actions][github_actions] to start the publish job.

6. Prepare for ongoing development and push to GitHub.

    ```
    sed -i "" \
      "s/ version = \'.*\'/ version = \'$NEXT_VERSION\'/g" \
      build.gradle
    git commit -am "Prepare next development version."
    git push
    ```

7. Confirm the [GitHub Actions][github_actions] publish job succeeded.

[github_actions]: https://github.com/IABTechLab/uid2-android-sdk/actions
[sonatype_issues]: https://issues.sonatype.org/
