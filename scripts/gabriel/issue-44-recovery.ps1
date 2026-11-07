[CmdletBinding()]
param(
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectName = 'colegio-connect-issue-44-recovery'
$TrialCount = 3
$RecoveryTargetSeconds = 300
$PollMilliseconds = 1000
$AllowedServices = @('postgres', 'mailpit', 'backend')
$VolumeName = "${ProjectName}_postgres_data"
$NetworkName = "${ProjectName}_default"
$ComposeArguments = @('compose', '-p', $ProjectName)

function Invoke-Docker {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $output = @(& docker @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $text = ($output | ForEach-Object { $_.ToString() }) -join "`n"
    if (-not $AllowFailure -and $exitCode -ne 0) {
        throw "Docker command failed with exit code $exitCode.`n$text"
    }

    [pscustomobject]@{
        ExitCode = $exitCode
        Output = $text.Trim()
    }
}

function Invoke-Compose {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    Invoke-Docker -Arguments ($ComposeArguments + $Arguments) -AllowFailure:$AllowFailure
}

function Get-FreeLoopbackPort {
    param(
        [Parameter(Mandatory)]
        [AllowEmptyCollection()]
        [System.Collections.Generic.HashSet[int]]$ReservedPorts
    )

    do {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
        $listener.Start()
        $port = ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
        $listener.Stop()
    } while (-not $ReservedPorts.Add($port))

    $port
}

function New-RandomValue {
    param(
        [int]$ByteCount = 24
    )

    $bytes = [byte[]]::new($ByteCount)
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    [Convert]::ToHexString($bytes).ToLowerInvariant()
}

function Get-DockerNames {
    param(
        [Parameter(Mandatory)]
        [ValidateSet('container', 'volume')]
        [string]$ResourceType
    )

    if ($ResourceType -eq 'container') {
        $result = Invoke-Docker -Arguments @('ps', '-a', '--format', '{{.ID}}')
    } else {
        $result = Invoke-Docker -Arguments @('volume', 'ls', '--format', '{{.Name}}')
    }

    @($result.Output -split "`r?`n" | Where-Object { $_ })
}

function Assert-NoProjectCollision {
    $containers = Invoke-Docker -Arguments @('ps', '-a', '--filter', "label=com.docker.compose.project=$ProjectName", '--format', '{{.ID}}')
    if ($containers.Output) {
        throw "Refusing to reuse existing containers for Compose project $ProjectName."
    }

    $volume = Invoke-Docker -Arguments @('volume', 'inspect', $VolumeName) -AllowFailure
    if ($volume.ExitCode -eq 0) {
        throw "Refusing to reuse existing volume $VolumeName."
    }

    $network = Invoke-Docker -Arguments @('network', 'inspect', $NetworkName) -AllowFailure
    if ($network.ExitCode -eq 0) {
        throw "Refusing to reuse existing network $NetworkName."
    }
}

function Assert-ComposeConfiguration {
    $rendered = Invoke-Compose -Arguments @('config', '--format', 'json')
    $config = $rendered.Output | ConvertFrom-Json

    foreach ($service in $AllowedServices) {
        if (-not $config.services.PSObject.Properties.Name.Contains($service)) {
            throw "Resolved Compose configuration is missing $service."
        }
    }

    $datasource = [string]$config.services.backend.environment.SPRING_DATASOURCE_URL
    if ($datasource -notmatch '^jdbc:postgresql://postgres:5432/') {
        throw 'Backend datasource does not use the isolated postgres service.'
    }

    foreach ($service in $AllowedServices) {
        foreach ($publishedPort in @($config.services.$service.ports)) {
            if ($publishedPort.host_ip -ne '127.0.0.1') {
                throw "$service publishes a port outside loopback."
            }
        }
    }

    if ($config.services.backend.depends_on.postgres.condition -ne 'service_healthy' -or
        $config.services.backend.depends_on.mailpit.condition -ne 'service_healthy') {
        throw 'Backend health dependencies are not preserved.'
    }

    if (-not $config.volumes.PSObject.Properties.Name.Contains('postgres_data')) {
        throw 'Resolved Compose configuration is missing postgres_data.'
    }

    [pscustomobject]@{
        Project = $ProjectName
        Services = $AllowedServices
        BackendPort = [int]$env:BACKEND_PORT
        MailpitPort = [int]$env:MAILPIT_UI_PORT
        PostgresPort = [int]$env:POSTGRES_PORT
        Volume = $VolumeName
    }
}

function Convert-ResponseContent {
    param(
        [Parameter(Mandatory)]
        $Response
    )

    if ($Response.Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Response.Content)
    }
    [string]$Response.Content
}

function Get-CsrfSession {
    param(
        [Parameter(Mandatory)]
        [string]$BackendBaseUrl
    )

    $session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
    Invoke-WebRequest -Uri "$BackendBaseUrl/api/auth/csrf" -Method Get -WebSession $session -TimeoutSec 5 | Out-Null
    $cookie = $session.Cookies.GetCookies($BackendBaseUrl)['XSRF-TOKEN']
    if ($null -eq $cookie) {
        throw 'Backend did not issue an XSRF-TOKEN cookie.'
    }

    [pscustomobject]@{
        Session = $session
        Headers = @{ 'X-XSRF-TOKEN' = [System.Net.WebUtility]::UrlDecode($cookie.Value) }
    }
}

function Invoke-JsonPost {
    param(
        [Parameter(Mandatory)]
        [string]$Uri,
        [Parameter(Mandatory)]
        [hashtable]$Body,
        [Parameter(Mandatory)]
        $Csrf
    )

    Invoke-WebRequest -Uri $Uri -Method Post -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Compress) -Headers $Csrf.Headers -WebSession $Csrf.Session -TimeoutSec 5
}

function Get-MailpitVerificationCode {
    param(
        [Parameter(Mandatory)]
        [string]$MailpitBaseUrl,
        [Parameter(Mandatory)]
        [string]$Email
    )

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
    $lastError = $null
    while ([DateTimeOffset]::UtcNow -lt $deadline) {
        try {
            $listResponse = Invoke-WebRequest -Uri "$MailpitBaseUrl/api/v1/messages" -Method Get -TimeoutSec 5
            $list = (Convert-ResponseContent $listResponse) | ConvertFrom-Json
            $message = @($list.messages) | Where-Object {
                @($_.To) | Where-Object { $_.Address -eq $Email }
            } | Select-Object -First 1
            if ($null -ne $message) {
                $detailResponse = Invoke-WebRequest -Uri "$MailpitBaseUrl/api/v1/message/$($message.ID)" -Method Get -TimeoutSec 5
                $detail = (Convert-ResponseContent $detailResponse) | ConvertFrom-Json
                $match = [regex]::Match("$($detail.Text)`n$($detail.HTML)", '(?<!\d)\d{6}(?!\d)')
                if ($match.Success) {
                    return $match.Value
                }
            }
        } catch {
            $lastError = $_.Exception.Message
        }
        Start-Sleep -Milliseconds $PollMilliseconds
    }

    if ($lastError) {
        throw "Verification message was not available within 60 seconds. Last error: $lastError"
    }
    throw 'Verification message was not available within 60 seconds.'
}

function Invoke-PsqlScalar {
    param(
        [Parameter(Mandatory)]
        [string]$Sql,
        [switch]$AllowFailure
    )

    $result = Invoke-Compose -Arguments @('exec', '-T', 'postgres', 'psql', '-X', '-U', $env:POSTGRES_USER, '-d', $env:POSTGRES_DB, '-Atq', '-c', $Sql) -AllowFailure:$AllowFailure
    if ($AllowFailure -and $result.ExitCode -ne 0) {
        return $null
    }
    $result.Output.Trim()
}

function Get-PostmasterStartTime {
    param([switch]$AllowFailure)

    Invoke-PsqlScalar -Sql "SELECT extract(epoch from pg_postmaster_start_time())::numeric(20,6);" -AllowFailure:$AllowFailure
}

function Get-DataSignature {
    param(
        [Parameter(Mandatory)]
        [string]$UserId
    )

    $sql = @"
WITH stable_records AS (
    SELECT 'user' AS record_type, id::text AS record_key,
           concat_ws(chr(31), id::text, email, password_hash, account_type, email_verified::text, status) AS payload
    FROM users
    WHERE id = '$UserId'::uuid
    UNION ALL
    SELECT 'email_code', id::text,
           concat_ws(chr(31), id::text, user_id::text, code_hash,
                     extract(epoch from created_at)::text,
                     extract(epoch from expires_at)::text,
                     coalesce(extract(epoch from used_at)::text, ''), attempts::text)
    FROM email_codes
    WHERE user_id = '$UserId'::uuid
)
SELECT count(*)::text || '|' || md5(string_agg(payload, chr(30) ORDER BY record_type, record_key))
FROM stable_records;
"@
    $parts = (Invoke-PsqlScalar -Sql $sql).Split('|', 2)
    if ($parts.Count -ne 2) {
        throw 'Database signature query returned an unexpected result.'
    }

    [pscustomobject]@{
        Count = [int]$parts[0]
        Checksum = $parts[1]
    }
}

function Invoke-FreshLogin {
    param(
        [Parameter(Mandatory)]
        [string]$BackendBaseUrl,
        [Parameter(Mandatory)]
        [string]$Email,
        [Parameter(Mandatory)]
        [string]$Password,
        [Parameter(Mandatory)]
        [string]$ExpectedUserId
    )

    $csrf = Get-CsrfSession -BackendBaseUrl $BackendBaseUrl
    $loginResponse = Invoke-JsonPost -Uri "$BackendBaseUrl/api/auth/login" -Body @{ email = $Email; password = $Password } -Csrf $csrf
    $login = (Convert-ResponseContent $loginResponse) | ConvertFrom-Json
    if ($login.id -ne $ExpectedUserId -or $login.email -ne $Email -or $login.accountType -ne 'UPRM_MEMBER') {
        throw 'Login response did not identify the expected student.'
    }

    $sessionCookie = @($csrf.Session.Cookies.GetAllCookies() | Where-Object { $_.Name -eq 'JSESSIONID' }) | Select-Object -Last 1
    if ($null -eq $sessionCookie) {
        throw 'Login did not issue a JSESSIONID cookie.'
    }
    $meResponse = Invoke-WebRequest -Uri "$BackendBaseUrl/api/auth/me" -Method Get -Headers @{ Cookie = "JSESSIONID=$($sessionCookie.Value)" } -TimeoutSec 5
    $me = (Convert-ResponseContent $meResponse) | ConvertFrom-Json
    if ($me.id -ne $ExpectedUserId -or $me.email -ne $Email -or $me.accountType -ne 'UPRM_MEMBER') {
        throw 'Authenticated session did not identify the expected student.'
    }

    $me
}

function Get-BackendHealth {
    param(
        [Parameter(Mandatory)]
        [string]$BackendBaseUrl
    )

    $response = Invoke-WebRequest -Uri "$BackendBaseUrl/actuator/health" -Method Get -TimeoutSec 5
    $body = (Convert-ResponseContent $response) | ConvertFrom-Json
    $response.StatusCode -eq 200 -and $body.status -eq 'UP'
}

function Invoke-RecoveryTrial {
    param(
        [Parameter(Mandatory)]
        [int]$Trial,
        [Parameter(Mandatory)]
        [string]$BackendBaseUrl,
        [Parameter(Mandatory)]
        [string]$Email,
        [Parameter(Mandatory)]
        [string]$Password,
        [Parameter(Mandatory)]
        [string]$UserId,
        [Parameter(Mandatory)]
        $Baseline
    )

    $beforeStart = Get-PostmasterStartTime
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $restart = Invoke-Compose -Arguments @('restart', 'postgres') -AllowFailure
    $errors = [System.Collections.Generic.List[string]]::new()
    if ($restart.ExitCode -ne 0) {
        $errors.Add("PostgreSQL restart command failed with exit code $($restart.ExitCode).")
    }

    $databaseSeconds = $null
    $loginSeconds = $null
    $afterStart = $null
    $student = $null
    $lastLoginError = 'No login response was received.'

    while ($stopwatch.Elapsed.TotalSeconds -lt $RecoveryTargetSeconds -and ($null -eq $databaseSeconds -or $null -eq $loginSeconds)) {
        if ($null -eq $databaseSeconds) {
            try {
                $candidateStart = Get-PostmasterStartTime -AllowFailure
                if ($candidateStart -and [decimal]$candidateStart -ne [decimal]$beforeStart) {
                    $afterStart = $candidateStart
                    $databaseSeconds = [math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
                }
            } catch {
                $errors.Add("Database probe: $($_.Exception.Message)")
            }
        }

        if ($null -eq $loginSeconds) {
            try {
                $student = Invoke-FreshLogin -BackendBaseUrl $BackendBaseUrl -Email $Email -Password $Password -ExpectedUserId $UserId
                $loginSeconds = [math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
            } catch {
                $lastLoginError = $_.Exception.Message
            }
        }

        if ($null -eq $databaseSeconds -or $null -eq $loginSeconds) {
            Start-Sleep -Milliseconds $PollMilliseconds
        }
    }
    $stopwatch.Stop()

    if ($null -eq $databaseSeconds) {
        $errors.Add('PostgreSQL did not answer from a changed server process within 300 seconds.')
    }
    if ($null -eq $loginSeconds) {
        $errors.Add("Fresh login did not succeed within 300 seconds. Last error: $lastLoginError")
    }

    $health = $false
    try {
        $health = Get-BackendHealth -BackendBaseUrl $BackendBaseUrl
    } catch {
        $errors.Add("Backend health probe: $($_.Exception.Message)")
    }

    $signature = $null
    try {
        $signature = Get-DataSignature -UserId $UserId
    } catch {
        $errors.Add("Data integrity probe: $($_.Exception.Message)")
    }

    $startChanged = $null -ne $afterStart -and [decimal]$afterStart -ne [decimal]$beforeStart
    $integrity = $null -ne $signature -and $signature.Count -eq $Baseline.Count -and $signature.Checksum -eq $Baseline.Checksum
    $passed = $restart.ExitCode -eq 0 -and $null -ne $databaseSeconds -and $databaseSeconds -lt $RecoveryTargetSeconds -and
        $null -ne $loginSeconds -and $loginSeconds -lt $RecoveryTargetSeconds -and $startChanged -and $health -and $integrity

    [pscustomobject]@{
        Trial = $Trial
        DatabaseRecoverySeconds = $databaseSeconds
        FreshLoginSeconds = $loginSeconds
        PostgresStartChanged = $startChanged
        BackendHealth = if ($health) { 'UP' } else { 'NOT_UP' }
        RecordCount = if ($signature) { $signature.Count } else { $null }
        ChecksumMatches = $integrity
        Verdict = if ($passed) { 'PASS' } else { 'FAIL' }
        Errors = @($errors)
    }
}

function Assert-IsolatedResourceLabels {
    $containers = Invoke-Docker -Arguments @('ps', '-a', '--filter', "label=com.docker.compose.project=$ProjectName", '--format', '{{.ID}}')
    foreach ($containerId in @($containers.Output -split "`r?`n" | Where-Object { $_ })) {
        $inspection = (Invoke-Docker -Arguments @('inspect', $containerId, '--format', '{{json .Config.Labels}}')).Output | ConvertFrom-Json
        if ($inspection.'com.docker.compose.project' -ne $ProjectName -or $inspection.'com.docker.compose.service' -notin $AllowedServices) {
            throw "Container $containerId does not have the expected isolated Compose labels."
        }
    }

    foreach ($resource in @(@{ Type = 'volume'; Name = $VolumeName }, @{ Type = 'network'; Name = $NetworkName })) {
        $inspectionResult = Invoke-Docker -Arguments @($resource.Type, 'inspect', $resource.Name) -AllowFailure
        if ($inspectionResult.ExitCode -eq 0) {
            $inspection = ($inspectionResult.Output | ConvertFrom-Json)[0]
            if ($inspection.Labels.'com.docker.compose.project' -ne $ProjectName) {
                throw "$($resource.Type) $($resource.Name) does not have the expected isolated Compose project label."
            }
        }
    }
}

function Remove-IsolatedResources {
    Assert-IsolatedResourceLabels
    $down = Invoke-Compose -Arguments @('down', '--volumes', '--remove-orphans') -AllowFailure
    if ($down.ExitCode -ne 0) {
        throw "Isolated Compose cleanup failed with exit code $($down.ExitCode)."
    }

    $containers = Invoke-Docker -Arguments @('ps', '-a', '--filter', "label=com.docker.compose.project=$ProjectName", '--format', '{{.ID}}')
    if ($containers.Output) {
        throw 'Isolated containers remain after cleanup.'
    }
    if ((Invoke-Docker -Arguments @('volume', 'inspect', $VolumeName) -AllowFailure).ExitCode -eq 0) {
        throw 'Isolated PostgreSQL volume remains after cleanup.'
    }
    if ((Invoke-Docker -Arguments @('network', 'inspect', $NetworkName) -AllowFailure).ExitCode -eq 0) {
        throw 'Isolated network remains after cleanup.'
    }
}

function Assert-PreexistingResourcesPreserved {
    param(
        [Parameter(Mandatory)]
        [string[]]$ContainerIds,
        [Parameter(Mandatory)]
        [string[]]$VolumeNames
    )

    $remainingContainers = @(Get-DockerNames -ResourceType container)
    $remainingVolumes = @(Get-DockerNames -ResourceType volume)
    foreach ($containerId in $ContainerIds) {
        if ($containerId -notin $remainingContainers) {
            throw "Pre-existing container $containerId is missing after the rehearsal."
        }
    }
    foreach ($volumeName in $VolumeNames) {
        if ($volumeName -notin $remainingVolumes) {
            throw "Pre-existing volume $volumeName is missing after the rehearsal."
        }
    }

    $development = Invoke-Docker -Arguments @('ps', '--filter', 'label=com.docker.compose.project=colegio-connect-dev', '--format', '{{.ID}}')
    if (-not $development.Output) {
        throw 'The colegio-connect-dev project is not still running.'
    }
    if ('colegio-connect-dev_postgres_data' -notin $remainingVolumes) {
        throw 'The colegio-connect-dev PostgreSQL volume is missing.'
    }
}

$environmentNames = @('POSTGRES_DB', 'POSTGRES_USER', 'POSTGRES_PASSWORD', 'POSTGRES_PORT', 'BACKEND_PORT', 'MAILPIT_SMTP_PORT', 'MAILPIT_UI_PORT')
$originalEnvironment = @{}
foreach ($name in $environmentNames) {
    $originalEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$reservedPorts = [System.Collections.Generic.HashSet[int]]::new()
$env:POSTGRES_DB = 'colegio_recovery'
$env:POSTGRES_USER = 'colegio_recovery'
$env:POSTGRES_PASSWORD = New-RandomValue
$env:POSTGRES_PORT = [string](Get-FreeLoopbackPort -ReservedPorts $reservedPorts)
$env:BACKEND_PORT = [string](Get-FreeLoopbackPort -ReservedPorts $reservedPorts)
$env:MAILPIT_SMTP_PORT = [string](Get-FreeLoopbackPort -ReservedPorts $reservedPorts)
$env:MAILPIT_UI_PORT = [string](Get-FreeLoopbackPort -ReservedPorts $reservedPorts)

$preexistingContainers = @()
$preexistingVolumes = @()
$resourcesCreated = $false
$cleanupVerified = $false
$results = [System.Collections.Generic.List[object]]::new()
$fatalError = $null

try {
    Invoke-Docker -Arguments @('info', '--format', '{{.ServerVersion}}') | Out-Null
    Assert-NoProjectCollision
    $configuration = Assert-ComposeConfiguration
    $preexistingContainers = @(Get-DockerNames -ResourceType container)
    $preexistingVolumes = @(Get-DockerNames -ResourceType volume)

    if ($ValidateOnly) {
        [pscustomobject]@{
            Mode = 'ValidateOnly'
            Configuration = $configuration
            ExistingContainersPreserved = $true
            ExistingVolumesPreserved = $true
        } | ConvertTo-Json -Depth 5
        return
    }

    $up = Invoke-Compose -Arguments @('up', '-d', '--build', '--wait', '--wait-timeout', '300', 'postgres', 'mailpit', 'backend')
    $resourcesCreated = $true

    $backendBaseUrl = "http://127.0.0.1:$($env:BACKEND_PORT)"
    $mailpitBaseUrl = "http://127.0.0.1:$($env:MAILPIT_UI_PORT)"
    $studentEmail = "recovery-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())-$(New-RandomValue -ByteCount 4)@upr.edu"
    $studentPassword = "R!$(New-RandomValue -ByteCount 16)"

    $registrationCsrf = Get-CsrfSession -BackendBaseUrl $backendBaseUrl
    Invoke-JsonPost -Uri "$backendBaseUrl/api/auth/register" -Body @{ email = $studentEmail; password = $studentPassword } -Csrf $registrationCsrf | Out-Null
    $verificationCode = Get-MailpitVerificationCode -MailpitBaseUrl $mailpitBaseUrl -Email $studentEmail
    $verificationCsrf = Get-CsrfSession -BackendBaseUrl $backendBaseUrl
    Invoke-JsonPost -Uri "$backendBaseUrl/api/auth/verify" -Body @{ email = $studentEmail; code = $verificationCode } -Csrf $verificationCsrf | Out-Null

    $loginCsrf = Get-CsrfSession -BackendBaseUrl $backendBaseUrl
    $loginResponse = Invoke-JsonPost -Uri "$backendBaseUrl/api/auth/login" -Body @{ email = $studentEmail; password = $studentPassword } -Csrf $loginCsrf
    $login = (Convert-ResponseContent $loginResponse) | ConvertFrom-Json
    $userId = [string]$login.id
    Invoke-FreshLogin -BackendBaseUrl $backendBaseUrl -Email $studentEmail -Password $studentPassword -ExpectedUserId $userId | Out-Null
    $baseline = Get-DataSignature -UserId $userId

    foreach ($trial in 1..$TrialCount) {
        $results.Add((Invoke-RecoveryTrial -Trial $trial -BackendBaseUrl $backendBaseUrl -Email $studentEmail -Password $studentPassword -UserId $userId -Baseline $baseline))
    }

    [pscustomobject]@{
        Project = $ProjectName
        Baseline = [pscustomobject]@{
            UserId = $userId
            RecordCount = $baseline.Count
            Checksum = $baseline.Checksum
            RegistrationVerified = $true
            FreshLoginVerified = $true
        }
        PollIntervalMilliseconds = $PollMilliseconds
        TargetSeconds = $RecoveryTargetSeconds
        Trials = @($results)
    } | ConvertTo-Json -Depth 7
} catch {
    $fatalError = $_
    throw
} finally {
    try {
        if ($resourcesCreated) {
            Remove-IsolatedResources
        }
        if ($preexistingContainers.Count -gt 0 -or $preexistingVolumes.Count -gt 0) {
            Assert-PreexistingResourcesPreserved -ContainerIds $preexistingContainers -VolumeNames $preexistingVolumes
        }
        $cleanupVerified = $true
    } catch {
        if ($null -eq $fatalError) {
            throw
        }
        Write-Error "Cleanup verification failed after the original error: $($_.Exception.Message)"
    } finally {
        foreach ($name in $environmentNames) {
            [Environment]::SetEnvironmentVariable($name, $originalEnvironment[$name], 'Process')
        }
    }
}

if ($results.Count -ne $TrialCount -or @($results | Where-Object { $_.Verdict -ne 'PASS' }).Count -gt 0 -or -not $cleanupVerified) {
    exit 1
}
