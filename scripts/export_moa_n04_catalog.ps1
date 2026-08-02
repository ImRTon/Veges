param(
    [Parameter(Mandatory = $true)]
    [string]$Output,
    [ValidateSet("N04", "N05")]
    [string]$KindCode = "N04",
    [string]$StartDate = "115.04.27",
    [string]$EndDate = "115.07.26",
    [int]$PageSize = 9999,
    [int]$MaxPagesPerMarket = 20
)

$ErrorActionPreference = "Stop"
$kindSlug = $KindCode.ToLowerInvariant()
$endpoint = "https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx"
$markets = @(
    [ordered]@{ QueryName = "台北一"; Basis = "TAIPEI_FIRST" },
    [ordered]@{ QueryName = "台北二"; Basis = "TAIPEI_SECOND" }
)
$records = [System.Collections.Generic.List[object]]::new()
$pagesFetched = 0

foreach ($market in $markets) {
    for ($page = 0; $page -lt $MaxPagesPerMarket; $page++) {
        $skip = $page * $PageSize
        $uri = $endpoint +
            "?%24top=$PageSize" +
            "&%24skip=$skip" +
            "&StartDate=$StartDate" +
            "&EndDate=$EndDate" +
            "&Market=$([uri]::EscapeDataString($market.QueryName))"
        # PowerShell 7 preserves a top-level JSON array as one pipeline value in
        # some assignment contexts. Force enumeration so property access below
        # is per official record rather than an accidental array-wide projection.
        $response = Invoke-RestMethod -Uri $uri -TimeoutSec 60
        $pageRecords = @($response | ForEach-Object { $_ })
        $pagesFetched++
        foreach ($record in $pageRecords) {
            if (
                $record.'種類代碼' -eq $KindCode -and
                $record.'作物代號' -ne "rest" -and
                -not [string]::IsNullOrWhiteSpace($record.'作物代號') -and
                -not [string]::IsNullOrWhiteSpace($record.'作物名稱')
            ) {
                $records.Add([pscustomobject]@{
                    Code = [string]$record.'作物代號'
                    Name = [string]$record.'作物名稱'
                    Market = [string]$market.Basis
                    Date = [string]$record.'交易日期'
                })
            }
        }
        if ($pageRecords.Count -lt $PageSize) {
            break
        }
        if ($page -eq $MaxPagesPerMarket - 1) {
            throw "Official query exceeded MaxPagesPerMarket=$MaxPagesPerMarket for $($market.QueryName)"
        }
    }
}

$varieties = @(
    $records |
        Group-Object Code, Name |
        ForEach-Object {
            $first = $_.Group[0]
            $dates = @($_.Group.Date | Sort-Object -Unique)
            [pscustomobject][ordered]@{
                commodityCode = $first.Code
                officialName = $first.Name
                markets = @($_.Group.Market | Sort-Object -Unique)
                observedDayCount = $dates.Count
                recordCount = $_.Count
                firstObservedRocDate = $dates[0]
                lastObservedRocDate = $dates[-1]
            }
        } |
        Sort-Object commodityCode, officialName
)

$artifact = [ordered]@{
    schemaVersion = 1
    source = "MOA_FARM_TRANS_DATA"
    snapshotId = "moa-$kindSlug-taipei-$($EndDate.Replace('.', '-'))"
    kindCode = $KindCode
    startRocDate = $StartDate
    endRocDate = $EndDate
    markets = @($markets.Basis)
    pageSize = $PageSize
    pagesFetched = $pagesFetched
    varieties = $varieties
}

$target = [IO.Path]::GetFullPath($Output)
$parent = [IO.Path]::GetDirectoryName($target)
if (-not [string]::IsNullOrEmpty($parent)) {
    [IO.Directory]::CreateDirectory($parent) | Out-Null
}
$json = $artifact | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($target, $json + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))

Write-Output "Wrote $($varieties.Count) $KindCode varieties from $($records.Count) records across $pagesFetched pages to $target"
