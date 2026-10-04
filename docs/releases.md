# APK releases and Obtainium

The `Build and release APK` workflow tests and builds pull requests. Every push to
`master` (or manual workflow run) builds a signed release APK, verifies its signature
and version, and publishes it with a SHA-256 checksum in GitHub Releases. Publishing
a release manually also builds and attaches the APK to that release.

Versions follow `<upstream version>-<fork commit hash>`, for example
`1.2.25-12345678`. Android's numeric version code is 1,000,000 plus the number of
commits on the first-parent history, so each subsequent commit can update the app.
Do not rewrite published history or change the signing key.

## Private signing key

Create a dedicated PKCS12 key once with alias `bluelineconsole`, using the same
password for the keystore and key. Configure these GitHub Actions repository secrets:

- `BLC_RELEASE_KEYSTORE_BASE64`: base64-encoded PKCS12 keystore.
- `BLC_RELEASE_KEYSTORE_PASSWORD`: its password.

Keep a private backup of the key and password. Neither belongs in git. The workflow
fails rather than publishing an unsigned APK when secrets are missing.

## Obtainium

Add `https://github.com/FZR-forks/bluelineconsole` as a GitHub app source. Releases
contain one universal APK and do not require an architecture filter. Each future
push to `master` publishes an update automatically after the checks pass.

The first installation may require uninstalling a previous build signed by upstream,
F-Droid, or a different debug key. Export your settings first. Later releases from
this fork use the same private signing key and install as normal updates.
