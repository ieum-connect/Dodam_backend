param(
    [string]$AiBackendPath = '../ai-backend',
    [int]$BackendPort = 8081,
    [int]$VaccinationPort = 8007,
    [string]$FrontendOrigin = 'http://localhost:4173',
    [string]$PythonVersion = '3.12'
)

$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
$ai = $null
$backend = $null

function Wait-ForHttp($url, $process) {
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        if ($process.HasExited) { throw '시연 프로세스가 종료되었습니다. data/showcase-logs를 확인해 주세요.' }
        try {
            return Invoke-RestMethod -Uri $url -TimeoutSec 2
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    throw '시연 서버 응답을 확인하지 못했습니다. data/showcase-logs를 확인해 주세요.'
}

Push-Location $backendRoot
try {
    $aiRoot = (Resolve-Path -LiteralPath $AiBackendPath).Path
    if (!(Test-Path (Join-Path $aiRoot 'app/domain/vaccination/engine.py'))) {
        throw '예방접종 코드가 있는 AI 백엔드 체크아웃 경로를 -AiBackendPath로 지정해 주세요.'
    }
    $python = Get-Command py.exe -ErrorAction Stop
    & $python.Source "-$PythonVersion" -c 'import sys; sys.exit(sys.version_info < (3, 11))'
    if ($LASTEXITCODE -ne 0) { throw 'Python 3.11 이상을 설치하고 -PythonVersion으로 버전을 지정해 주세요.' }
    foreach ($port in @($BackendPort, $VaccinationPort)) {
        $probe = [System.Net.Sockets.TcpClient]::new()
        try {
            try { $probe.Connect('127.0.0.1', $port) } catch [System.Net.Sockets.SocketException] {}
            if ($probe.Connected) { throw "포트 $port 사용 중입니다. 기존 로컬 서버를 종료하거나 다른 포트를 지정해 주세요." }
        } finally { $probe.Dispose() }
    }

    docker compose up -d --wait postgres
    if ($LASTEXITCODE -ne 0) { throw '로컬 PostgreSQL을 시작하지 못했습니다.' }
    $logs = New-Item -ItemType Directory -Force -Path "data/showcase-logs/$BackendPort"
    $env:PYTHONPATH = $aiRoot
    $env:AI_INTERNAL_TOKEN = [guid]::NewGuid().ToString('N')
    $env:DODAM_SHOWCASE_VACCINATION_PORT = [string]$VaccinationPort
    $env:JWT_SECRET = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    $env:STORAGE_LOCAL_ROOT = 'data/object-storage'

    $ai = Start-Process -FilePath $python.Source -ArgumentList "-$PythonVersion showcase/vaccination_server.py" -WorkingDirectory $PSScriptRoot -RedirectStandardOutput (Join-Path $logs.FullName 'vaccination.out.log') -RedirectStandardError (Join-Path $logs.FullName 'vaccination.err.log') -WindowStyle Hidden -PassThru
    Wait-ForHttp "http://127.0.0.1:$VaccinationPort/health" $ai | Out-Null
    $spring = "--server.port=$BackendPort --server.address=127.0.0.1 --spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/dodam --spring.datasource.username=dodam --spring.datasource.password=dodam_local --dodam.demo-login=true --jwt.cookie-secure=false --dodam.frontend-origin=$FrontendOrigin --dodam.ai.base-url=http://127.0.0.1:$VaccinationPort --dodam.push.enabled=false --dodam.schedule.reminders-enabled=false --dodam.welfare.gov24.sync-enabled=false"
    $backend = Start-Process -FilePath (Join-Path $backendRoot 'mvnw.cmd') -ArgumentList @('spring-boot:run', ('-Dspring-boot.run.arguments="' + $spring + '"')) -WorkingDirectory $backendRoot -RedirectStandardOutput (Join-Path $logs.FullName 'backend.out.log') -RedirectStandardError (Join-Path $logs.FullName 'backend.err.log') -WindowStyle Hidden -PassThru
    $api = "http://127.0.0.1:$BackendPort/api"
    Wait-ForHttp "$api/auth/config" $backend | Out-Null
    & (Join-Path $PSScriptRoot 'seed-showcase.ps1') -StorageRoot $env:STORAGE_LOCAL_ROOT
    Invoke-RestMethod -Uri "$api/demo/login" -Method Post -ContentType 'application/json' -Body '{"account":"MINJI"}' -SessionVariable demoSession | Out-Null
    $children = Invoke-RestMethod -Uri "$api/children" -WebSession $demoSession
    $primary = $children | Where-Object { $_.name -eq '박도윤 (시연)' } | Select-Object -First 1
    if (!$primary) { throw '실행 중인 백엔드에서 김민지 시연 아이를 찾지 못했습니다.' }
    $query = '?patientId=' + $primary.id
    foreach ($endpoint in @('health-records', 'visits', 'documents')) {
        $page = Invoke-RestMethod -Uri "$api/$endpoint$query" -WebSession $demoSession
        if (!$page.items.Count) { throw "김민지 시연 데이터가 비어 있습니다 ($endpoint)." }
    }
    $doseChecks = Invoke-RestMethod -Uri "$api/vaccinations$query" -WebSession $demoSession
    $profile = Invoke-RestMethod -Uri "$api/welfare-profiles$query" -WebSession $demoSession
    if ($doseChecks.counts.done -lt 1 -or !$profile.regionCode) {
        throw '예방접종 또는 복지 시연 데이터가 빠져 있습니다.'
    }
    Write-Output "로컬 백엔드 http://127.0.0.1:$BackendPort 에 김민지 시연 데이터가 준비되었습니다."
} catch {
    foreach ($process in @($backend, $ai)) {
        if ($process -and !$process.HasExited) {
            taskkill.exe /PID $process.Id /T /F | Out-Null
        }
    }
    throw
} finally {
    Pop-Location
}
