# warm-letters

Демонстрация работы приложения:
https://disk.yandex.ru/i/AnflaKD57WOdgg

Для запуска:

1) Пропишите ip сервера и доступный порт в файл warm-letters/app/src/main/assets/config.template.yaml, удалите template из названия

2) Соберите приложение в apk файл, установите его на смартфон

3) Запустите server.py

Сборка кодировщика на unix системех - из папки bebr_encoder запустить команду go build -o main.so -buildmode=c-shared .
