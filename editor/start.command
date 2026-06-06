#!/bin/bash
# Запуск локального сервера редактора Todo100 + открытие в браузере.
# Двойной клик по этому файлу в Finder (macOS).

cd "$(dirname "$0")" || exit 1

PORT=8777
URL="http://localhost:8777"

echo "Todo100 редактор → $URL"
echo "Останов: закрой это окно или нажми Ctrl+C"
echo

# открыть браузер чуть позже, когда сервер поднимется
( sleep 1; open "$URL" ) &

python3 -m http.server "$PORT"
