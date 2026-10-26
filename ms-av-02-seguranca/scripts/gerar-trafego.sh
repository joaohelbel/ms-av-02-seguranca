#!/usr/bin/env bash
# Gera logins válidos, inválidos e bloqueios para popular o dashboard do Grafana.
URL=${URL:-http://localhost:8080}

login() {
  printf "login %-8s %-10s -> " "$1" "$2"
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$URL/login" \
    -H "Content-Type: application/json" -d "{\"username\":\"$1\",\"senha\":\"$2\"}"
}

echo "== Logins válidos"
for i in 1 2 3 4 5; do login joao 123456; done
for i in 1 2 3; do login bianca 123456; done

echo "== Logins inválidos"
login fantasma 000
login joao errada            # 1 falha (ainda não bloqueia)
login joao 123456            # zera as falhas

echo "== Bloqueando maria e pedro (2 falhas cada)"
login maria x1; login maria x2
login pedro x1; login pedro x2
login maria abc123           # 401: bloqueada

echo "== Desbloqueando pedro (admin)"
curl -s -u admin:admin123 -X PUT "$URL/unlock/pedro"; echo

echo "== Listando usuários (manutencao)"
curl -s -u manutencao:manut123 "$URL/"; echo
