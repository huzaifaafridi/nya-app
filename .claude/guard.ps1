$f = "$PSScriptRoot\usage.txt"
if (Test-Path $f) {
    $p = [int](Get-Content $f)
    if ($p -ge 70) {
        [Console]::Error.WriteLine("STOP: weekly usage is $p% (limit 70%). Do not continue working. Tell the user what is finished, what is left, and wait.")
        exit 2
    }
}
exit 0