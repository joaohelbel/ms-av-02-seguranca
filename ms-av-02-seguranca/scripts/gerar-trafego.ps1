# Gera logins válidos, inválidos e bloqueios para popular o dashboard do Grafana (Windows).
# Executar: powershell -ExecutionPolicy Bypass -File scripts\gerar-trafego.ps1
$URL = "http://localhost:8080"

function Login($user, $senha) {
    $body = "{`"username`":`"$user`",`"senha`":`"$senha`"}"
    $body | Out-File -Encoding ascii -NoNewline "$env:TEMP\login.json"
    $code = curl.exe -s -o NUL -w "%{http_code}" -X POST "$URL/login" -H "Content-Type: application/json" --data "@$env:TEMP\login.json"
    Write-Host ("login {0,-8} {1,-10} -> {2}" -f $user, $senha, $code)
}

Write-Host "== Logins validos"
1..5 | ForEach-Object { Login "joao" "123456" }
1..3 | ForEach-Object { Login "bianca" "123456" }

Write-Host "== Logins invalidos"
Login "fantasma" "000"
Login "joao" "errada"
Login "joao" "123456"

Write-Host "== Bloqueando maria e pedro (2 falhas cada)"
Login "maria" "x1"; Login "maria" "x2"
Login "pedro" "x1"; Login "pedro" "x2"
Login "maria" "abc123"

Write-Host "== Desbloqueando pedro (admin)"
curl.exe -s -u admin:admin123 -X PUT "$URL/unlock/pedro"; Write-Host ""

Write-Host "== Listando usuarios (manutencao)"
curl.exe -s -u manutencao:manut123 "$URL/"; Write-Host ""
