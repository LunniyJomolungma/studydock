#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/test-classes
find src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 -d build/classes @build/sources.txt
jar --create --file build/studydock.jar --main-class studydock.StudyDock -C build/classes .
if [ "${1:-}" = "test" ]; then
  find src/test/java -name '*.java' > build/tests.txt
  javac -encoding UTF-8 -cp build/classes -d build/test-classes @build/tests.txt
  java -cp build/classes:build/test-classes studydock.Tests
fi
printf '%s\n' 'Built build/studydock.jar. Run: java -jar build/studydock.jar --demo'
