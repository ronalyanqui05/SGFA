param(
    [switch]$SkipTests,
    [switch]$ConDatos,
    [string]$Logo
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME no esta definido. Debe apuntar a un JDK 21 (con jlink/jpackage)."
}
$java = Join-Path $env:JAVA_HOME "bin\java.exe"
$jlink = Join-Path $env:JAVA_HOME "bin\jlink.exe"
$jpackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
$jdeps = Join-Path $env:JAVA_HOME "bin\jdeps.exe"
foreach ($tool in @($java, $jlink, $jpackage, $jdeps)) {
    if (-not (Test-Path $tool)) {
        throw "No se encontro $tool -- JAVA_HOME debe apuntar a un JDK 21 completo (no un JRE)."
    }
}

$modules = @(
    "java.base", "java.compiler", "java.desktop", "java.instrument", "java.logging",
    "java.management", "java.naming", "java.net.http", "java.prefs", "java.rmi",
    "java.scripting", "java.security.jgss", "java.sql", "java.sql.rowset",
    "java.transaction.xa", "java.xml.crypto", "jdk.jfr", "jdk.unsupported", "jdk.unsupported.desktop"
) -join ","

$appVersion = "0.1.0"
$appName = "SGFA"
$vendor = "RYCRR"
$description = "Sistema de Gestion de Fichas Academicas"
$mainClass = "pe.edu.sgfa.Launcher"

Write-Host "== 1/4: mvnw clean package ==" -ForegroundColor Cyan
$mvnArgs = @("-q", "clean", "package")
if ($SkipTests) { $mvnArgs += "-DskipTests" }
$ErrorActionPreference = "Continue"
& "$repoRoot\mvnw.cmd" @mvnArgs 2>&1 | ForEach-Object { "$_" }
$ErrorActionPreference = "Stop"
if ($LASTEXITCODE -ne 0) { throw "mvnw clean package fallo." }

$jar = Join-Path $repoRoot "target\sgfa.jar"
if (-not (Test-Path $jar)) { throw "No se genero target/sgfa.jar -- revisar maven-shade-plugin en pom.xml." }

Write-Host "== 2/4: jlink (runtime a medida) ==" -ForegroundColor Cyan
$runtimeDir = Join-Path $repoRoot "target\runtime"
if (Test-Path $runtimeDir) { Remove-Item -Recurse -Force $runtimeDir }
& $jlink --add-modules $modules --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output $runtimeDir
if ($LASTEXITCODE -ne 0) { throw "jlink fallo." }

function New-IcoDesdeImagen([string]$origen, [string]$destino) {
    Add-Type -AssemblyName System.Drawing
    $imagen = [System.Drawing.Image]::FromFile($origen)
    try {
        $tamanos = @(16, 24, 32, 48, 64, 128, 256)
        $pngs = @()
        foreach ($t in $tamanos) {
            $bmp = New-Object System.Drawing.Bitmap $t, $t, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            $g = [System.Drawing.Graphics]::FromImage($bmp)
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $g.Clear([System.Drawing.Color]::Transparent)
            $escala = [Math]::Min($t / $imagen.Width, $t / $imagen.Height)
            $w = [int][Math]::Max(1, [Math]::Round($imagen.Width * $escala))
            $h = [int][Math]::Max(1, [Math]::Round($imagen.Height * $escala))
            $g.DrawImage($imagen, [int](($t - $w) / 2), [int](($t - $h) / 2), $w, $h)
            $g.Dispose()
            $ms = New-Object System.IO.MemoryStream
            $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
            $pngs += , ($ms.ToArray())
            $bmp.Dispose()
        }
        $fs = [System.IO.File]::Create($destino)
        $bw = New-Object System.IO.BinaryWriter $fs
        $bw.Write([UInt16]0); $bw.Write([UInt16]1); $bw.Write([UInt16]$tamanos.Count)
        $desplazamiento = 6 + 16 * $tamanos.Count
        for ($i = 0; $i -lt $tamanos.Count; $i++) {
            $lado = if ($tamanos[$i] -ge 256) { 0 } else { $tamanos[$i] }
            $bw.Write([byte]$lado); $bw.Write([byte]$lado); $bw.Write([byte]0); $bw.Write([byte]0)
            $bw.Write([UInt16]1); $bw.Write([UInt16]32)
            $bw.Write([UInt32]$pngs[$i].Length); $bw.Write([UInt32]$desplazamiento)
            $desplazamiento += $pngs[$i].Length
        }
        foreach ($png in $pngs) { $bw.Write($png) }
        $bw.Close(); $fs.Close()
    } finally {
        $imagen.Dispose()
    }
}

$origenLogo = $Logo
if (-not $origenLogo) {
    foreach ($nombre in @("logo.png", "logo.jpg", "logo.jpeg")) {
        $candidato = Join-Path $env:USERPROFILE ".sgfa\config\$nombre"
        if (Test-Path $candidato) { $origenLogo = $candidato; break }
    }
}
$argsIcono = @()
if ($origenLogo -and (Test-Path $origenLogo)) {
    $carpetaIcono = Join-Path $repoRoot "target\icono"
    New-Item -ItemType Directory -Force -Path $carpetaIcono | Out-Null
    $icono = Join-Path $carpetaIcono "sgfa.ico"
    New-IcoDesdeImagen $origenLogo $icono
    $argsIcono = @("--icon", $icono)
    Write-Host "Icono del .exe generado desde el logo de la institucion: $origenLogo" -ForegroundColor Green
} else {
    Write-Host "No hay logo configurado: SGFA.exe usara el icono por defecto." -ForegroundColor Yellow
}

Write-Host "== 3/4: jpackage --type app-image ==" -ForegroundColor Cyan
$distDir = Join-Path $repoRoot "target\dist"
$inputDir = Join-Path $distDir "input"
if (Test-Path $distDir) { Remove-Item -Recurse -Force $distDir }
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
Copy-Item $jar $inputDir

if ($ConDatos) {
    $datosOrigen = Join-Path $env:USERPROFILE ".sgfa"
    if (-not (Test-Path (Join-Path $datosOrigen "data\sgfa.db"))) {
        throw "No hay base de datos en $datosOrigen\data\sgfa.db para incluir. Quite -ConDatos para un instalador vacio."
    }
    if (Get-Process SGFA -ErrorAction SilentlyContinue) { throw "Cierre SGFA antes de empaquetar los datos (la base esta en uso)." }
    $semilla = Join-Path $inputDir "datos-iniciales"
    foreach ($sub in @("data", "config", "documents", "tessdata")) {
        $desde = Join-Path $datosOrigen $sub
        if (Test-Path $desde) { Copy-Item $desde (Join-Path $semilla $sub) -Recurse }
    }
    Get-ChildItem (Join-Path $semilla "data") | Where-Object { $_.Name -ne "sgfa.db" } | Remove-Item -Force
    Write-Host "Datos incluidos desde $datosOrigen (base, documentos, configuracion)." -ForegroundColor Green
}

$appImageDir = Join-Path $distDir "app-image"
& $jpackage --type app-image `
    --input $inputDir --dest $appImageDir `
    --name $appName --main-jar sgfa.jar --main-class $mainClass `
    --runtime-image $runtimeDir --app-version $appVersion `
    --vendor $vendor --description $description @argsIcono
if ($LASTEXITCODE -ne 0) { throw "jpackage --type app-image fallo." }

$exePath = Join-Path $appImageDir "$appName\$appName.exe"
if (-not (Test-Path $exePath)) { throw "No se genero $exePath." }
Write-Host "App-image lista en: $appImageDir\$appName ($exePath)" -ForegroundColor Green

Write-Host "== 4/4: jpackage --type exe (instalador) ==" -ForegroundColor Cyan
$candle = Get-Command candle.exe -ErrorAction SilentlyContinue
if (-not $candle) {
    $wixBin = Get-ChildItem "${env:ProgramFiles(x86)}\WiX Toolset v3*\bin", "$env:ProgramFiles\WiX Toolset v3*\bin" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($wixBin) { $env:PATH = "$($wixBin.FullName);$env:PATH"; $candle = Get-Command candle.exe -ErrorAction SilentlyContinue }
}
if (-not $candle) {
    Write-Host "WiX Toolset (candle.exe) no esta en el PATH -- se omite el instalador." -ForegroundColor Yellow
    Write-Host "El app-image de arriba ya es un SGFA.exe autocontenido y funcional." -ForegroundColor Yellow
    Write-Host "Para generar el instalador, instale WiX Toolset 3.x (winget install WiXToolset.WiXToolset)" -ForegroundColor Yellow
    Write-Host "y agregue su carpeta bin al PATH antes de reintentar." -ForegroundColor Yellow
    exit 0
}

$installerDir = Join-Path $distDir "installer"
& $jpackage --type exe `
    --input $inputDir --dest $installerDir `
    --name $appName --main-jar sgfa.jar --main-class $mainClass `
    --runtime-image $runtimeDir --app-version $appVersion `
    --vendor $vendor --description $description `
    --win-shortcut --win-menu --win-dir-chooser --win-shortcut-prompt @argsIcono
if ($LASTEXITCODE -ne 0) { throw "jpackage --type exe fallo." }

Write-Host "Instalador listo en: $installerDir\$appName-$appVersion.exe" -ForegroundColor Green
