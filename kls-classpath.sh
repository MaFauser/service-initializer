#!/usr/bin/env sh
# Feeds Gradle's compile classpath to the Kotlin Language Server (fwcd.kotlin) so Cursor/VS Code
# can resolve Spring and other dependencies.
# Linux/macOS: use this script.
# Windows: use the sibling `kls-classpath.cmd`.
# After adding or changing these files, run:
#   Ctrl+Shift+P -> "Kotlin: Restart Language Server"
cd "$(dirname "$0")"
./gradlew -q printClasspath --no-configuration-cache 2>/dev/null || true
