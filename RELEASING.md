Releasing
========

 1. Ensure everything builds and all tests pass, including integration tests.
    (Note that Integration tests can be flaky.)
    `./gradlew clean build check connectedCheck`
 2. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 3. Update the `CHANGELOG.md` for the impending release.
 4. Update the `README.md` with the new version.
 5. `git commit -am "Prepare for release X.Y."` (where X.Y is the new version)
 6. `git tag -a X.Y -m "Version X.Y"` (where X.Y is the new version)
 7. `./gradlew clean uploadArchives`
 8. Update the `gradle.properties` to the next SNAPSHOT version.
 9. `git commit -am "Prepare next development version."`
 10. `git push && git push --tags`
 11. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
