# Ничего не падает — расписание

Консольное приложение на Kotlin: читает файл с расписанием, выводит занятия за период или по типу и показывает ошибки в строках файла.

## Сборка и запуск

Запускать из корня проекта. Нужен JDK 25, для проверки — Python.

Windows:

```powershell
.\gradlew.bat build :app:installDist
.\app\build\install\schedule\bin\schedule.bat period --file samples/clean.txt --from 2026-09-28 --to 2027-05-13
.\app\build\install\schedule\bin\schedule.bat type --file samples/dirty.txt --type EXAM
.\app\build\install\schedule\bin\schedule.bat errors --file samples/dirty.txt
```

Linux / macOS:

```bash
bash ./gradlew build :app:installDist
./app/build/install/schedule/bin/schedule period --file samples/clean.txt --from 2026-09-28 --to 2027-05-13
./app/build/install/schedule/bin/schedule type --file samples/dirty.txt --type EXAM
./app/build/install/schedule/bin/schedule errors --file samples/dirty.txt
```

Тесты: `.\gradlew.bat test`, проверка вывода: `python tools/verify.py`.

## Где что лежит

- `model/` — `Session` и четыре класса занятий, плюс общие функции проверки в `Validation.kt`
- `repo/` — реестр, обёртка с журналом и `AuditEvent`
- `parse/` — разбор строки, результат разбора и причины отказа
- `format/` — текст ошибок и вывод занятий
- `Commands.kt` — команды `period`, `type`, `errors`

## Почему class, а не data class

Для занятий я выбрал обычный `class`. У `data class` есть `copy()`, через который можно создать объект с другими полями, и тогда его тоже пришлось бы защищать. Ещё в `data class` все параметры конструктора должны быть свойствами, а мне нужно принять список преподавателей и сохранить его копию.

У обычного класса `copy()` нет, поэтому объект создаётся только через конструктор, а там все проверки в `init`. Все поля `val`, так что после создания их нельзя поменять. Наследование я не использовал (сдача — не лекция), общие проверки вынесены в функции.

`Session` сделан `sealed`, чтобы `when` по типу занятия был без `else`. `AuditEvent` — `data class`, потому что у него нет ограничений, а сравнивать события в тестах так удобнее. `ParseResult` и `RejectReason` тоже `sealed`: плохая строка возвращается как результат с причиной, а не через исключение.

## Защита коллекций

`List<String>` — это только список для чтения, сам объект при этом может быть изменяемым. Поэтому преподаватели сохраняются через `java.util.List.copyOf(teachers)`:

- это копия, поэтому если потом изменить исходный список, занятие не поменяется;
- это неизменяемый список, поэтому если привести его к `MutableList` и вызвать `add`, будет `UnsupportedOperationException`. С `toList()` так бы не работало.

Проверки в `init` делаются по сохранённой копии (`this.teachers`).

Реестр возвращает в `all()` копию своего списка, поэтому старый снимок не меняется после новых добавлений.

## Делегирование

`AuditedSessionRepository` сделан через `SessionRepository by delegate`. Хранит занятия делегат, а обёртка переопределяет `add` и `addAll`: сначала вызывает делегата, потом пишет событие в журнал и увеличивает счётчик. `add` даёт событие `("add", 1)`, `addAll` — `("addAll", размер списка)`, в том числе `("addAll", 0)` для пустого списка. `all()` не переопределён и в журнал не пишет.

**Почему мало переопределить только `add`:** тогда `addAll` у обёртки сразу уйдёт в `delegate.addAll`, и журнал его не увидит. Даже если делегат внутри `addAll` вызывает `add`, это будет его собственный `add`, а не обёртки — делегат про обёртку ничего не знает. Поэтому `addAll` тоже переопределён.

## Попытки сломать модель

| # | Что пробуем | Что защищает | Тест |
|---|---|---|---|
| 1 | Пустая тема | проверка при создании | `ModelTest.rejectsInvalidTopic` |
| 2 | Год 0 или 10000 | проверка при создании | `ModelTest.rejectsYearOutOfRange` |
| 3 | Занятие без преподавателей | проверка при создании | `ModelTest.rejectsActiveSessionWithoutTeachers` |
| 4 | Сдача с одним преподавателем | проверка при создании | `ModelTest.rejectsExamWithOneTeacher` |
| 5 | Сдача с двумя одинаковыми | проверка при создании | `ModelTest.rejectsExamWithDuplicateTeachers` |
| 6 | Изменить исходный список | защита коллекции (копия) | `RepositoryTest.originalListChangeDoesNotAffectSession` |
| 7 | Изменить выданный список | защита коллекции (неизменяемый список) | `RepositoryTest.teachersListFromSessionCannotBeModified` |
| 8 | Обойти проверки через `copy` | устройство типа (у `class` нет `copy`) | — |

Создание всех четырёх типов, снимки и журнал проверяются в `ModelTest.createsAllFourTypes` и остальных тестах `RepositoryTest`.

## Свои сообщения об ошибках

Обязательные сообщения сделаны как в задании. Для пункта 7 я придумал такие:

| Проверка | Сообщение |
|---|---|
| управляющие символы | `line N: field "topic" contains control characters: "a\u0009b"` |
| пробелы по краям | `line N: field "teachers" has leading or trailing whitespace: " Petrov"` |
| поля отмены не `-` | `line N: type "CANCELLED" requires "-" in field "location", got "Room 1"` |
| место `-` у занятия | `line N: type "LECTURE" requires a location, got "-"` |
| плохое имя преподавателя | `line N: invalid teacher name ""` |
| повтор имени | `line N: duplicate teacher "Ivanov"` |

Значения в кавычках экранируются: `\` → `\\`, `"` → `\"`, остальное не-ASCII и управляющие символы → `\uXXXX`.

Для преподавателей в ошибке пишется конкретное имя, а не всё поле. Имя из одних пробелов считается плохим именем.