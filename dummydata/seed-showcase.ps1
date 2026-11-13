param(
    [string]$StorageRoot = $(if ($env:STORAGE_LOCAL_ROOT) { $env:STORAGE_LOCAL_ROOT } else { 'data/object-storage' })
)

$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $PSScriptRoot
$fixtures = Join-Path $PSScriptRoot 'showcase'

Push-Location $backendRoot
try {
    $storage = New-Item -ItemType Directory -Path (Join-Path $StorageRoot 'showcase') -Force
    $assets = [ordered]@{
        checkup_size = 'checkup.png'
        medication_size = 'medication.png'
        lab_result_size = 'lab-result.png'
        outpatient_size = 'outpatient.png'
        sibling_checkup_size = 'sibling-checkup.png'
    }
    $psqlArgs = @('compose', 'exec', '-T', '-e', 'PGCLIENTENCODING=UTF8', 'postgres',
        'psql', '-X', '-U', 'dodam', '-d', 'dodam', '-v', 'ON_ERROR_STOP=1')
    foreach ($asset in $assets.GetEnumerator()) {
        $file = Get-Item -LiteralPath (Join-Path $fixtures $asset.Value)
        Copy-Item -LiteralPath $file.FullName -Destination $storage.FullName
        $psqlArgs += @('-v', "$($asset.Key)=$($file.Length)")
    }

    $previousEncoding = $OutputEncoding
    try {
        $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        Get-Content -LiteralPath (Join-Path $fixtures 'seed.sql') -Raw -Encoding UTF8 | docker @psqlArgs
        if ($LASTEXITCODE -ne 0) { throw '시연 데이터를 초기화하지 못했습니다. 로컬 PostgreSQL과 Flyway 마이그레이션 상태를 확인해 주세요.' }
    } finally {
        $OutputEncoding = $previousEncoding
    }
    Write-Output '김민지 시연 데이터와 문서 원본을 준비했습니다.'
} finally {
    Pop-Location
}
