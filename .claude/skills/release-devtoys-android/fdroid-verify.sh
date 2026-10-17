#!/bin/bash
# Rebuilds a DevToys release the way F-Droid's build server does and checks it against the signed GitHub APK.
# Usage: fdroid-verify.sh <versionName> <versionCode> <full commit hash>
# Needs Docker. Exit 0 = F-Droid's build is byte-identical to the GitHub release.
set -euo pipefail

version=$1 code=$2 commit=$3
[[ $commit =~ ^[0-9a-f]{40}$ ]] || { echo "commit must be a full 40-character hash"; exit 2; }
image=registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

cat > "$work/com.cir0cuit.devtoys.yml" <<EOF
License: Apache-2.0
SourceCode: https://github.com/Cir0cuit/devtoys-android

RepoType: git
Repo: https://github.com/Cir0cuit/devtoys-android.git
Binaries: https://github.com/Cir0cuit/devtoys-android/releases/download/v%v/DevToys-%v.apk

Builds:
  - versionName: $version
    versionCode: $code
    commit: $commit
    subdir: app
    gradle:
      - yes

AllowedAPKSigningKeys: c9680cf469fc025144b51139198f5b1109e1cbbcc497b0d9da56ad9f757e000b

AutoUpdateMode: Version
UpdateCheckMode: Tags ^v[\d.]+$
CurrentVersion: $version
CurrentVersionCode: $code
EOF

# Runs inside the container; mirrors the "fdroid build" job in fdroiddata's .gitlab-ci.yml.
cat > "$work/run.sh" <<'EOF'
set -e
APPID=com.cir0cuit.devtoys
BUILD=$APPID:$CODE
export ANDROID_HOME=/opt/android-sdk
D=/builds/fdroiddata
git clone -q --depth 1 https://gitlab.com/fdroid/fdroiddata.git $D
cp /host/$APPID.yml $D/metadata/$APPID.yml
cd $D
source /etc/profile.d/bsenv.sh
rm -rf $fdroidserver
git clone -q --depth 1 https://gitlab.com/fdroid/fdroidserver.git $fdroidserver
export PATH="$fdroidserver:$PATH" PYTHONPATH="$fdroidserver:$fdroidserver/examples" PYTHONUNBUFFERED=true serverwebroot=/tmp
apt-get -q update >/dev/null
sdkmanager "platform-tools" "build-tools;31.0.0" >/dev/null
git -C $home_vagrant/gradlew-fdroid pull -q || true
mkdir -p build logs tmp unsigned srclibs
for d in logs tmp unsigned $home_vagrant/.android $home_vagrant/.gradle $home_vagrant/metadata; do mkdir -p $d; chown -R vagrant $d; done
ln -s $D/tmp $home_vagrant/tmp
ln -s $D/srclibs $home_vagrant/srclibs
export GRADLE_USER_HOME=$home_vagrant/.gradle
fdroid="sudo --preserve-env --user vagrant env PATH=$fdroidserver:$PATH env PYTHONPATH=$fdroidserver:$fdroidserver/examples env PYTHONUNBUFFERED=true env HOME=$home_vagrant fdroid"
apt-get install -qy sudo openjdk-21-jdk-headless >/dev/null
update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java
cp -R $D/build $home_vagrant/build
cp metadata/$APPID.yml $home_vagrant/metadata/
chown -R vagrant $home_vagrant $D
cd $home_vagrant
ln -s $D $home_vagrant/fdroiddata
$fdroid fetchsrclibs $BUILD
rm $home_vagrant/fdroiddata
set +e
(unset CI; $fdroid build --verbose --test --refresh-scanner --on-server --no-tarball $BUILD)
rc=$?
exit $rc
EOF

host=$work
command -v cygpath >/dev/null && host=$(cygpath -w "$work")
docker info >/dev/null 2>&1 || { echo "Docker is not running"; exit 2; }
set +e
MSYS_NO_PATHCONV=1 docker run --rm -e CODE="$code" -v "$host:/host" "$image" bash /host/run.sh > "$work/log.txt" 2>&1
rc=$?
set -e
# Long lines are cut: on a mismatch fdroid prints whole differing files.
grep -aE "Successfully built|successfully verified|NOT verified|Could not build|CRITICAL" "$work/log.txt" | cut -c1-200 || true

if [ $rc -ne 0 ]; then
  echo "Files that differ between F-Droid's build and the GitHub APK:"
  grep -aE "^(diff -r|Only in|Binary files) " "$work/log.txt" | sed -E 's#/tmp/[^ ]*/content/##g' | cut -c1-200 || true
  log="${TMPDIR:-/tmp}/fdroid-verify-$version.log"
  cp "$work/log.txt" "$log"
  echo "full log: $log"
fi
echo "fdroid build exit code: $rc"
exit $rc
