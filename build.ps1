param([switch]$Test)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $compiler = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
    $runtime = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
    $archiver = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/jar.exe' } else { 'jar' }
    New-Item -ItemType Directory -Force build/classes | Out-Null
    $sources = Get-ChildItem src/main/java -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
    & $compiler -encoding UTF-8 -d build/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
    & $archiver --create --file build/studydock.jar --main-class studydock.StudyDock -C build/classes .
    if ($LASTEXITCODE -ne 0) { throw 'Packaging failed.' }
    if ($Test) {
        $tests = Get-ChildItem src/test/java -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
        New-Item -ItemType Directory -Force build/test-classes | Out-Null
        & $compiler -encoding UTF-8 -cp build/classes -d build/test-classes $tests
        if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
        & $runtime -cp 'build/classes;build/test-classes' studydock.Tests
        if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
    }
    Write-Host 'Built build/studydock.jar. Run: java -jar build/studydock.jar --demo'
} finally { Pop-Location }
