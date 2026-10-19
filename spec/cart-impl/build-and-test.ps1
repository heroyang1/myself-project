$ErrorActionPreference = "Stop"
$root = "c:\Users\Admin1\IdeaProjects\osh-backend\spec\cart-impl"
$m2 = Join-Path $env:USERPROFILE ".m2\repository\org"
$jars = @(
  "$m2\junit\jupiter\junit-jupiter-api\5.8.2\junit-jupiter-api-5.8.2.jar",
  "$m2\junit\jupiter\junit-jupiter-engine\5.8.2\junit-jupiter-engine-5.8.2.jar",
  "$m2\junit\platform\junit-platform-commons\1.9.3\junit-platform-commons-1.9.3.jar",
  "$m2\junit\platform\junit-platform-engine\1.9.3\junit-platform-engine-1.9.3.jar",
  "$m2\junit\platform\junit-platform-launcher\1.9.3\junit-platform-launcher-1.9.3.jar",
  "$m2\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar",
  "$m2\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar"
)
foreach ($j in $jars) { if (-not (Test-Path $j)) { throw "MISSING JAR: $j" } }
$cp = $jars -join ";"
$out = Join-Path $root "out"
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Force -Path $out | Out-Null

$mainSrc = Join-Path $root "src\main\java"
$testSrc = Join-Path $root "src\test\java"
$testFile = Join-Path $testSrc "com\backstage\system\cart\service\impl\CartServiceImplTest.java"
$runner = Join-Path $root "testsupport\TestRunner.java"

Write-Output "=== COMPILE ==="
& javac -encoding UTF-8 -cp $cp -d $out -sourcepath "$mainSrc;$testSrc" $testFile $runner
if ($LASTEXITCODE -ne 0) { throw "COMPILE FAILED exit=$LASTEXITCODE" }
Write-Output "COMPILE OK"

Write-Output "=== RUN ==="
& java -cp "$cp;$out" TestRunner
Write-Output "RUN exit=$LASTEXITCODE"
