# Awake

Awake — небольшой Java CLI-проект с двумя запланированными режимами:

- `inhibit`: штатно просит операционную систему не переходить в сон;
- `activity`: создаёт контролируемую активность для явно заданной цели.

На текущем этапе режим `inhibit` действительно предотвращает системный сон. Первый рабочий вариант `activity` поддерживает macOS TextEdit: он печатает в отдельный временный документ, не изменяя исходный `--target`. Windows и Linux пока используют activity-заглушку.

Платформенные реализации `inhibit`:

- macOS — `/usr/bin/caffeinate -i`;
- Windows — системный вызов `SetThreadExecutionState` через JNA;
- Linux — `systemd-inhibit` (команда должна быть доступна в `PATH`).

`inhibit` не мешает дисплею гаснуть и не объявляет пользователя активным: он блокирует только автоматический сон компьютера.

## Требования и сборка

- JDK 11 или новее;
- Maven 3.8 или новее.

```bash
mvn clean package
java -jar target/awake.jar --help
```

## Примеры

```bash
java -jar target/awake.jar --mode inhibit --hours 4
java -jar target/awake.jar --mode inhibit --until 23:30
java -jar target/awake.jar --mode activity --hours 2 --target "/tmp/awake.txt"
java -jar target/awake.jar --mode activity --from 20:00 --until 23:30 --target "/tmp/awake.txt" --interval 30
java -jar target/awake.jar --mode activity --hours 4 --target "/tmp/awake.txt" --no-inhibit --erase-after 100
java -jar target/awake.jar --mode activity --hours 1 --target "/tmp/awake.txt" --dry-run
```

`--hours` принимает положительное целое число. Время задаётся в 24-часовом формате `HH:mm`. Параметр `--from` используется только вместе с `--until`. Для `activity` параметр `--target` обязателен.

Если `--until` уже прошло сегодня, окончание переносится на следующий день. Если `--from` уже прошло, запуск переносится на следующий день. Пара `--from 20:00 --until 01:00` означает пятичасовое окно через полночь.

В режиме `activity` системный `inhibit` включается по умолчанию как страховка. `--no-inhibit` отключает его, а `--erase-after 100` задаёт удаление каждой сотни подтверждённо введённых символов. `--dry-run` печатает план и сразу завершается, гарантированно не запуская планировщик и системный inhibit.

### Activity на macOS

- Поддерживается редактор TextEdit.
- `--target` может указывать файл либо `/System/Applications/TextEdit.app`; исходный файл не открывается и не изменяется.
- Awake создаёт собственный временный документ и удаляет его при завершении.
- Перед вводом проверяются TextEdit, временный документ, фокус и ожидаемая длина текста.
- Если пользователь работает в другом окне TextEdit, очередное действие пропускается.
- Для синтетического ввода macOS может запросить разрешение Accessibility/Automation для Java или терминала, из которого запущен JAR.

Подробные ограничения описаны в [ACTIVITY-SAFETY.md](ACTIVITY-SAFETY.md).

## Коды завершения

- `0` — успех;
- `2` — некорректные аргументы;
- `3` — ошибка запуска/очистки системного режима или неподдерживаемая платформа;
- `130` — прерывание пользователем (зарезервирован для планировщика).

План следующих этапов находится в [ROADMAP.md](ROADMAP.md).
