#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLI="$BASE_DIR/quest-home-switcher"
APK="$BASE_DIR/Quest-Home-Switcher-v1.8.apk"

"$CLI" doctor
"$CLI" install-switcher "$APK"
"$CLI" open
