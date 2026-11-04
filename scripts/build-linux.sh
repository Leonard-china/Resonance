#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
if [[ $(uname -s) != Linux ]]; then
    echo 'Build this package on Linux.' >&2
    exit 1
fi
format=${1:-deb}
case "$format" in
    portable) package_task=() ;;
    deb) package_task=(:composeApp:packageReleaseDeb) ;;
    rpm) package_task=(:composeApp:packageReleaseRpm) ;;
    *) echo 'Usage: scripts/build-linux.sh [deb|rpm|portable]' >&2; exit 2 ;;
esac
if [[ -n ${JAVA_HOME:-} ]]; then
    java_command="$JAVA_HOME/bin/java"
else
    java_command=$(command -v java || true)
fi
if [[ ! -x $java_command ]]; then
    echo 'Install JDK 17–21 (including jpackage) and set JAVA_HOME.' >&2
    exit 1
fi
command -v ffmpeg >/dev/null || { echo 'Install FFmpeg first.' >&2; exit 1; }
command -v objcopy >/dev/null || { echo 'Install binutils (objcopy) for jlink.' >&2; exit 1; }
if [[ $format == deb ]]; then
    command -v dpkg-deb >/dev/null
    command -v fakeroot >/dev/null || { echo 'Install fakeroot to build a .deb.' >&2; exit 1; }
elif [[ $format == rpm ]]; then
    command -v rpmbuild >/dev/null || { echo 'Install rpm-build to build an .rpm.' >&2; exit 1; }
fi
export RESONANCE_FFMPEG=${RESONANCE_FFMPEG:-$(command -v ffmpeg)}
# Invoke the checked-in Wrapper JAR, also when the checkout has CRLF shell scripts.
"$java_command" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain \
    :composeApp:desktopTest :composeApp:packageLinuxPortable "${package_task[@]}" \
    --no-configuration-cache --console=plain
echo 'Packages: composeApp/build/compose/binaries/main-release/{tar,deb,rpm}/'
