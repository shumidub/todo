# task-003 · Tasks3 таб с палитрой J2 Canary

**Sprint**: sprint-001-k8m3n2p-task-ux
**Status**: Phase 2 — clarification
**Owner**: bg-agent (TBD)

## User-facing описание
Добавить 4-й таб **Tasks3** в `MainActivity` ViewPager, который функционально повторяет Tasks/Tasks2, но использует палитру J2 Canary (тёплый канареечный фон с бумажными карточками и коралловым акцентом).

## Палитра J2 Canary
- `bg` `#FBE34A` — основной фон экрана (канареечный жёлтый)
- `surface` `#FFFEEC` — почти-белая карточка Inbox / поле ввода (тёплый-белый)
- `surfaceMuted` `#F0D63D` — нижняя плашка таба
- `accent` `#E94E3B` — коралловый акцент (кнопка "C")
- `text` `#2E2A08` — тёплый чёрный
- `textSoft` `rgba(46,42,8,0.64)` — плейсхолдеры / second-level
- `inputText` `#2E2A08` — текст на белой карточке
- `counter` `#94882F` — счётчик "0 / 2"
- `divider` `rgba(46,42,8,0.14)` — тонкая линия над полем ввода

## Затронутые файлы (предварительно)
- Новый: `ui/theme/CanaryPalette.java` (зеркало `CornflowerPalette`)
- `res/values/colors.xml` — цвета `canary*`
- `MainPagerAdapter.java` — `getCount() = 4`, добавить case для position 2 → Tasks3
- `FolderSlidingPanelFragment` + дочерние адаптеры — учитывать новый palette index
- Возможно: `MainActivity` — индикатор табов

## Открытые вопросы заранее
- Tasks3 — это просто Tasks с новой палитрой? Или с другим набором функций?
- Сохранять разные списки задач в Tasks/Tasks2/Tasks3 или один общий?

## Requirements
_TBD bg-агент в фазе 2_

## Design
_TBD фаза 3_

## Tests
_skip (manual QA)_

## Implementation
_TBD фаза 5_

## Review
_TBD фаза 6_

## Open Questions
_TBD bg-агент в фазе 2_
