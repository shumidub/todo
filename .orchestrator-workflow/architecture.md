# Architecture — todo100android

## Стек
- **Platform**: Android, minSdk 24, targetSdk/compileSdk 35
- **Language**: Java 17
- **DB**: Realm (`realm-android` Gradle plugin)
- **UI**: AppCompat + Material 1.11, AndroidX, RecyclerView, CardView
- **Navigation**: single `MainActivity` + `CustomViewPager` (androidx.viewpager.widget) + `FragmentPagerAdapter`
- **Build**: Gradle (`app/build.gradle`)
- **Tests**: AndroidJUnitRunner настроен, но реально проект полагается на manual QA (в спринтах TDD не используется)

## Слои
- `realmmodel/` — Realm-сущности (`task/`, `notes/`, `report/`)
- `realmcontrollers/` — обёртки над Realm для CRUD (`taskcontroller/`, `notescontroller/`, `reportcontroller/`, `ContainersControllers/`)
- `ui/activity/` — `MainActivity` + `BaseActivity`
- `ui/fragment/` — экраны по секциям: `task_section/`, `note_fragment/`, `report_section/`
- `ui/dialog/` — кастомные диалоги (edit task/folder/note/report)
- `ui/actionmode/` — `ActionMode.Callback` для multi-select (task/note/report)
- `ui/theme/` — палитры (`CornflowerPalette` для Tasks2)
- `sync/` — синхронизация

## Архитектурный паттерн
Lightweight MVP — частичный:
- В части фрагментов есть `presenter/` пакеты (напр. `FolderSlidingPanelFragment` → `PresenterFolderSlidingPanelFragment`)
- В других — Fragment напрямую дёргает `realmcontrollers/`
- Нет общего DI-фреймворка, нет ViewModel/LiveData/RxJava

## Tab layout
`MainPagerAdapter.getCount() == 3`:
- pos 0: `FolderNoteFragment` (Notes)
- pos 1: `FolderSlidingPanelFragment.newInstance(0)` (Tasks)
- pos 2: `FolderSlidingPanelFragment.newInstance(1)` (Tasks2 — Cornflower palette)

Tasks2 палитра инжектится через `CornflowerPalette(Context)` resolver и применяется в адаптерах/фрагментах по index.

## Ключевые экраны task-секции
- `FolderSlidingPanelFragment` — список папок/категорий + слайд-панель с задачами
- `SmallTasksFragment` — экран категории с её задачами
- `TasksRecyclerViewAdapter` — рендеринг карточек задач
- `FolderOfTaskRecyclerViewAdapter` — рендеринг карточек категорий
- `TaskActionModeCallback` — контекстные действия над выбранными задачами

## Конвенции
- Java-классы CamelCase, пакеты snake_case (исторически)
- Layouts: `*_card_view.xml`, `dialog_*.xml`
- Feature flags пока нет — добавляются через этот спринт
