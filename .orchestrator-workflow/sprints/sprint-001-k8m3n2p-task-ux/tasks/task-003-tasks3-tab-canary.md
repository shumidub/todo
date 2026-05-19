# task-003 · Tasks3 таб с палитрой J2 Canary

**Sprint**: sprint-001-k8m3n2p-task-ux
**Status**: Phase 2 complete → Phase 3 (architecture)
**Owner**: bg-agent (TBD)

## User-facing описание
Добавить 4-й таб **Tasks3** в `MainActivity` ViewPager. Tasks3 = **независимый** список папок/задач (как Tasks vs Tasks2), использующий палитру J2 Canary и включающий все остальные новые фичи спринта (BottomSheet редактор из task-001, секции из task-002).

## Палитра J2 Canary
| token | value (#AARRGGBB) | usage |
|---|---|---|
| `canaryBg` | `#FBE34A` | основной фон экрана |
| `canarySurface` | `#FFFEEC` | бумажная карточка / поле ввода |
| `canarySurfaceMuted` | `#F0D63D` | нижняя плашка таба |
| `canaryAccent` | `#E94E3B` | коралловый акцент (применяется ко всем upstream-callers `accent`: линия под полем ввода, cursor, cycling indicator, ActionMode-бар) |
| `canaryText` | `#2E2A08` | основной текст |
| `canaryTextSoft` | `#A32E2A08` (alpha 0xA3 = 0.64) | плейсхолдеры, second-level |
| `canaryInputText` | `#2E2A08` | текст на белой карточке |
| `canaryCounter` | `#94882F` | счётчик "0 / 2" |
| `canaryDivider` | `#242E2A08` (alpha 0x24 = 0.14) | тонкая линия над полем ввода |

## Requirements (финал, Phase 2 confirmed)

### Tab integration
- **R1.** `MainPagerAdapter.getCount()` → 4. Position 3 → `FolderSlidingPanelFragment.newInstance(2)` (`taskGroup = 2`).
- **R2.** Дефолтный таб при старте — остаётся Tasks (position 1, `setCurrentItem(1)`).
- **R3.** Имя "Tasks3" пользователю **не показывается** напрямую: ActionBar title = имя текущей категории Tasks3 (как в Tasks/Tasks2). TabLayout не вводится.
- **R4.** Все места в `MainActivity`/`FolderSlidingPanelFragment`/`SmallTasksFragment`/адаптерах, где есть `if (taskGroup == 1)` для Cornflower, расширяются на `taskGroup == 2` для Canary. **Без** общего интерфейса `TabPalette` — параллельный класс (рефактор отдельной задачей).

### Палитра — артефакты
- **R5.** Новый `ui/theme/CanaryPalette.java` — зеркало `CornflowerPalette` (поля: `bg, surface, surfaceMuted, text, textSoft, inputText, counter, accent, divider`).
- **R6.** В `res/values/colors.xml` — все `canary*` ресурсы из таблицы выше.
- **R7.** В `res/values/styles.xml` — `ThemeOverlay.App.MaterialAlertDialog.Canary` по аналогии с Cornflower.
- **R8.** В `applyTabChrome` (MainActivity) для Canary-таба: `WindowInsetsControllerCompat.setAppearanceLightStatusBars(true)` и `setAppearanceLightNavigationBars(true)` — тёмные иконки на жёлтом фоне.

### Точки применения (зеркало Cornflower)
- **R9.** Каждый upstream-caller `CornflowerPalette` получает ветку для Canary:
  - `MainActivity.applyTabChrome` — статусбар/навбар/actionbar/фон
  - `MainActivity.dialogBuilder` / `dialogContext` — выбор `ThemeOverlay`
  - `MainActivity.tintActionModeBarIfCornflower` → расширить (имя оставить или переименовать в `tintActionModeBarForPalette`, на усмотрение Phase 3)
  - `FolderSlidingPanelFragment.applyCornflowerPalette(view)` → отдельный `applyCanaryPalette` (параллельный)
  - `FolderOfTaskRecyclerViewAdapter` — switch по `taskGroup` → выбор палитры
  - `TasksRecyclerViewAdapter.useCornflowerPalette(boolean)` → отдельный `useCanaryPalette(boolean)` (или один setter с enum, на усмотрение Phase 3 — минимизировать дифф)
  - `SmallTasksFragment.isInCornflowerTab()` → отдельный `isInCanaryTab()` + getter для палитры
  - `TaskActionModeCallback` — выбор `accent` цвета по `taskGroup`
- **R10.** В `onBackPressed` распознавать `currentFragmentItem == 3` и матчить `getTaskGroup() == 2` идентично существующему.

### Данные (независимый список)
- **R11.** Tasks3 — **независимый** список папок. В `RealmFoldersContainer` добавить `RealmList<FolderTaskObject> folderOfTasksList3`.
- **R12.** Realm migration: поднять `SCHEMA_VERSION` (координация с task-002 — обе задачи трогают migration; финальная версия = +1 от текущего 3, но единым шагом).
  - Через `schema.get("RealmFoldersContainer").addRealmListField("folderOfTasksList3", ...)`.
- **R13.** `FolderTaskRealmController` расширить:
  - `getFoldersList(int group)` — case `2` → `folderOfTasksList3`
  - `getFolderGroup(folderId)` — возвращает 2 для Tasks3-папок
  - `getAllFolders()` — включает Tasks3
  - `moveFolderToGroup` — поддержать `targetGroup == 2`

### Интеграция с task-001 (BottomSheet) и task-002 (секции)
- **R14.** Tasks3 использует тот же `SmallTasksFragment` + `TasksRecyclerViewAdapter`, что и Tasks/Tasks2 — значит BottomSheet редактор и секции там работают **автоматически**, при условии что:
  - BottomSheet принимает текущую палитру (`CanaryPalette` для Tasks3) — см. task-001 R13
  - Секции (task-002 R15) применяются ко всем 3 task-табам, включая Tasks3
- **R15.** Координация по порядку фаз 4-5: модельные изменения task-002 (миграция, новые controller) и task-003 (новый список папок) собираются в **единую миграцию** `SCHEMA_VERSION → 4` (планировщик Phase 3 это учитывает).

### Non-goals
- **R16.** Не вводится общий `TabPalette` интерфейс / `PaletteProvider` — параллельная структура классов. Рефактор отдельной задачей будущего спринта.
- **R17.** Не меняются иконки, dialog layouts (только цвета через ThemeOverlay).

## Затронутые файлы
- Новый: `ui/theme/CanaryPalette.java`
- `res/values/colors.xml`, `res/values/styles.xml` — `canary*` ресурсы, ThemeOverlay
- `MainPagerAdapter.java` — `getCount=4`, position 3
- `MainActivity.java` — `applyTabChrome`, `dialogBuilder`, `tintActionModeBar...`, `onBackPressed`
- `FolderSlidingPanelFragment.java` + дочерние адаптеры (`FolderOfTaskRecyclerViewAdapter`, `TasksRecyclerViewAdapter`) — ветка `taskGroup==2`
- `SmallTasksFragment.java` — `isInCanaryTab` + getter палитры
- `TaskActionModeCallback.java` — accent по taskGroup
- `realmmodel/RealmFoldersContainer` — `folderOfTasksList3`
- `RealmMigrations.java` + `RealmConfiguration` — координированно с task-002
- `FolderTaskRealmController.java` — поддержка `group=2`

## Design

### 1. Resources — `res/values/colors.xml`

Добавить блок сразу после Cornflower (после строки `<color name="cornflowerDivider">#2EF2F4FA</color>`):

```xml
<!-- Canary palette (Tasks3 tab) -->
<color name="canaryBg">#FBE34A</color>
<color name="canarySurface">#FFFEEC</color>
<color name="canarySurfaceMuted">#F0D63D</color>
<color name="canaryText">#2E2A08</color>
<color name="canaryTextSoft">#A32E2A08</color>
<color name="canaryInputText">#2E2A08</color>
<color name="canaryCounter">#94882F</color>
<color name="canaryAccent">#E94E3B</color>
<color name="canaryDivider">#242E2A08</color>
```

Замечание по alpha: Android `#AARRGGBB`. `#A32E2A08` = alpha 0xA3 (163/255 ≈ 0.64) поверх `#2E2A08`. `#242E2A08` = alpha 0x24 (36/255 ≈ 0.14). Соответствует таблице в требованиях.

### 2. Resources — `res/values/styles.xml`

Добавить ниже `ThemeOverlay.App.MaterialAlertDialog.Cornflower`:

```xml
<!-- Canary MaterialAlertDialog overlay — used when a dialog is spawned from the Tasks3 tab. -->
<style name="ThemeOverlay.App.MaterialAlertDialog.Canary" parent="ThemeOverlay.App.MaterialAlertDialog">
    <item name="colorSurface">@color/canarySurfaceMuted</item>
    <item name="colorOnSurface">@color/canaryText</item>
    <item name="android:textColorPrimary">@color/canaryText</item>
    <item name="android:textColorSecondary">@color/canaryTextSoft</item>
    <item name="android:colorBackground">@color/canarySurfaceMuted</item>
    <item name="colorAccent">@color/canaryAccent</item>
    <item name="colorPrimary">@color/canaryAccent</item>
    <item name="colorControlActivated">@color/canaryAccent</item>
</style>
```

### 3. New file — `ui/theme/CanaryPalette.java`

Зеркало `CornflowerPalette` с теми же полями. Параллельный класс (R16 — no shared `TabPalette`):

```java
package com.shumidub.todoapprealm.ui.theme;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.shumidub.todoapprealm.R;

/**
 * Canary palette used by the Tasks3 tab. Mirror of CornflowerPalette.
 * Parallel class by design — see task-003 R16, ADR пока нет, общий
 * TabPalette интерфейс будет отдельным рефактором.
 */
public final class CanaryPalette {
    public final int bg;
    public final int surface;
    public final int surfaceMuted;
    public final int text;
    public final int textSoft;
    public final int inputText;
    public final int counter;
    public final int accent;
    public final int divider;

    public CanaryPalette(Context ctx) {
        bg = ContextCompat.getColor(ctx, R.color.canaryBg);
        surface = ContextCompat.getColor(ctx, R.color.canarySurface);
        surfaceMuted = ContextCompat.getColor(ctx, R.color.canarySurfaceMuted);
        text = ContextCompat.getColor(ctx, R.color.canaryText);
        textSoft = ContextCompat.getColor(ctx, R.color.canaryTextSoft);
        inputText = ContextCompat.getColor(ctx, R.color.canaryInputText);
        counter = ContextCompat.getColor(ctx, R.color.canaryCounter);
        accent = ContextCompat.getColor(ctx, R.color.canaryAccent);
        divider = ContextCompat.getColor(ctx, R.color.canaryDivider);
    }
}
```

### 4. `MainPagerAdapter.java` — diff

```java
@Override public int getCount() { return 4; }

@Override
public Fragment getItem(int position) {
    if (position == 0) return new FolderNoteFragment();
    if (position == 1) return FolderSlidingPanelFragment.newInstance(0);
    if (position == 2) return FolderSlidingPanelFragment.newInstance(1);
    if (position == 3) return FolderSlidingPanelFragment.newInstance(2);
    return null;
}
```

### 5. `MainActivity.java` — изменения

**5.1 `isCornflowerTab()` оставляем как есть** (для совместимости с TaskActionModeCallback в текущей форме), **добавляем**:

```java
public boolean isCanaryTab() {
    return viewPager != null && viewPager.getCurrentItem() == 3;
}
```

**5.2 `dialogBuilder()` — расширить:**

```java
public com.google.android.material.dialog.MaterialAlertDialogBuilder dialogBuilder() {
    if (viewPager == null) return new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
    int pos = viewPager.getCurrentItem();
    if (pos == 2) return new com.google.android.material.dialog.MaterialAlertDialogBuilder(this,
            R.style.ThemeOverlay_App_MaterialAlertDialog_Cornflower);
    if (pos == 3) return new com.google.android.material.dialog.MaterialAlertDialogBuilder(this,
            R.style.ThemeOverlay_App_MaterialAlertDialog_Canary);
    return new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
}
```

**5.3 `dialogContext()` — расширить:**

```java
public android.content.Context dialogContext() {
    if (viewPager == null) return this;
    int pos = viewPager.getCurrentItem();
    if (pos == 2) return new androidx.appcompat.view.ContextThemeWrapper(this,
            R.style.ThemeOverlay_App_MaterialAlertDialog_Cornflower);
    if (pos == 3) return new androidx.appcompat.view.ContextThemeWrapper(this,
            R.style.ThemeOverlay_App_MaterialAlertDialog_Canary);
    return this;
}
```

**5.4 `tintActionModeBarIfCornflower()` — переименовать в `tintActionModeBarForCurrentTab()`** (минимальный diff: вызовы только из `onSupportActionModeStarted` / `onActionModeStarted` в этом же файле). Тело:

```java
public void tintActionModeBarForCurrentTab() {
    if (viewPager == null) return;
    int pos = viewPager.getCurrentItem();
    final int color;
    if (pos == 2) {
        color = new com.shumidub.todoapprealm.ui.theme.CornflowerPalette(this).bg;
    } else if (pos == 3) {
        color = new com.shumidub.todoapprealm.ui.theme.CanaryPalette(this).bg;
    } else {
        return;
    }
    Runnable tint = () -> applyActionModeBarColor(color);
    View decor = getWindow().getDecorView();
    decor.post(tint);
    decor.postDelayed(tint, 100);
}
```

Обновить два callsite (`onSupportActionModeStarted`, `onActionModeStarted`) → `tintActionModeBarForCurrentTab()`. Старое имя оставлять не нужно — все вызовы внутри `MainActivity.java`.

**5.5 `applyTabChrome(int position)` — расширить case position==3:**

```java
private void applyTabChrome(int position) {
    if (rootLayout == null || actionBar == null) return;
    androidx.core.view.WindowInsetsControllerCompat insets =
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
    if (position == 2) {
        com.shumidub.todoapprealm.ui.theme.CornflowerPalette p =
                new com.shumidub.todoapprealm.ui.theme.CornflowerPalette(this);
        rootLayout.setBackgroundColor(p.bg);
        actionBar.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(p.bg));
        getWindow().setStatusBarColor(p.bg);
        getWindow().setNavigationBarColor(p.bg);
        if (insets != null) {
            insets.setAppearanceLightStatusBars(false);
            insets.setAppearanceLightNavigationBars(false);
        }
    } else if (position == 3) {
        com.shumidub.todoapprealm.ui.theme.CanaryPalette p =
                new com.shumidub.todoapprealm.ui.theme.CanaryPalette(this);
        rootLayout.setBackgroundColor(p.bg);
        actionBar.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(p.bg));
        getWindow().setStatusBarColor(p.bg);
        getWindow().setNavigationBarColor(p.bg);
        if (insets != null) {
            insets.setAppearanceLightStatusBars(true);   // R8 — dark icons on yellow
            insets.setAppearanceLightNavigationBars(true);
        }
    } else {
        int green = androidx.core.content.ContextCompat.getColor(this, R.color.colorBackgroundActivity);
        rootLayout.setBackgroundColor(green);
        int primary = androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary);
        actionBar.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(primary));
        getWindow().setStatusBarColor(primary);
        getWindow().setNavigationBarColor(primary);
        if (insets != null) {
            insets.setAppearanceLightStatusBars(false);
            insets.setAppearanceLightNavigationBars(false);
        }
    }
}
```

Импорт: `androidx.core.view.WindowInsetsControllerCompat`, `androidx.core.view.WindowCompat` (последний уже импортирован).

**5.6 `onPageSelected` — расширить блок titles на `position == 3`:**

```java
else if (position == 1 || position == 2 || position == 3){
    for (Fragment fragment: getSupportFragmentManager().getFragments()){
        if (fragment instanceof FolderSlidingPanelFragment
                && ((FolderSlidingPanelFragment) fragment).getTaskGroup() == position - 1){
            actionBar.setTitle(((FolderSlidingPanelFragment) fragment).getValidTitle());
        }
    }
}
```

И блок `if (position==1)` (где `setDisplayHomeAsUpEnabled(false)`) — без изменений, остальные позиции уже падают в `else { actionBar.setDisplayHomeAsUpEnabled(false); }`.

**5.7 `onBackPressed` — добавить ветку для position 3:**

```java
if (currentFragmentItem == 1 || currentFragmentItem == 2 || currentFragmentItem == 3){
    for (Fragment fragment: getSupportFragmentManager().getFragments()){
        if (fragment instanceof FolderSlidingPanelFragment
                && ((FolderSlidingPanelFragment) fragment).getTaskGroup() == currentFragmentItem - 1){
            SlidingUpPanelLayout slidingUpPanelLayout = ((FolderSlidingPanelFragment) fragment).slidingUpPanelLayout;
            if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED){
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                return;
            } else {
                onBackPressedWithTimer();
                return;
            }
        }
    }
}
```

### 6. `FolderSlidingPanelFragment.java` — изменения

**6.1 Поля:** убрать `private CornflowerPalette palette` как тип — сделать `Object` или ввести второй field. Для минимального diff и параллельности:

```java
private com.shumidub.todoapprealm.ui.theme.CornflowerPalette cornflowerPalette;
private com.shumidub.todoapprealm.ui.theme.CanaryPalette canaryPalette;
```

(старое поле `palette` удалить; все его использования заменить).

**6.2 onViewCreated:**

```java
if (taskGroup == 1) applyCornflowerPalette(view);
else if (taskGroup == 2) applyCanaryPalette(view);
```

**6.3 Новый метод `applyCanaryPalette(View root)`** — точное зеркало `applyCornflowerPalette`, только `new CanaryPalette(getContext())` и присваивание `canaryPalette = ...`. Идентичный набор `setBackgroundColor` / `setTextColor` / cursor drawable.

**6.4 `activeAccent()`:**

```java
private int activeAccent() {
    if (cornflowerPalette != null) return cornflowerPalette.accent;
    if (canaryPalette != null) return canaryPalette.accent;
    return getResources().getColor(R.color.colorAccent);
}
```

**6.5 Getter палитры для downstream** (для task-001 BottomSheet, task-002 секции):

```java
/** Active palette tint accent (Cornflower/Canary/default colorAccent). */
public int getActiveAccent() { return activeAccent(); }

/** true если этот fragment рендерится в Cornflower-табе. */
public boolean isCornflowerGroup() { return taskGroup == 1; }

/** true если этот fragment рендерится в Canary-табе. */
public boolean isCanaryGroup() { return taskGroup == 2; }
```

(BottomSheet редактор из task-001 берёт палитру через эти getter'ы; общий интерфейс — non-goal R16.)

### 7. `FolderOfTaskRecyclerViewAdapter.java` — изменения

Поле `palette` остаётся `CornflowerPalette` для совместимости — добавляем параллельное:

```java
private CornflowerPalette cornflowerPalette;
private com.shumidub.todoapprealm.ui.theme.CanaryPalette canaryPalette;
```

Конструктор:

```java
public FolderOfTaskRecyclerViewAdapter(RealmList<FolderTaskObject> realmListFolder, Activity activity, int taskGroup){
    this.realmListFolder = realmListFolder;
    this.activity = activity;
    this.taskGroup = taskGroup;
    if (taskGroup == 1) cornflowerPalette = new CornflowerPalette(activity);
    else if (taskGroup == 2) canaryPalette = new com.shumidub.todoapprealm.ui.theme.CanaryPalette(activity);
}
```

В `onBindViewHolder` заменить `if (palette != null) applyPaletteToFolderCard((ItemViewHolder) holder);` на:

```java
if (cornflowerPalette != null) applyCornflowerPaletteToCard((ItemViewHolder) holder);
else if (canaryPalette != null) applyCanaryPaletteToCard((ItemViewHolder) holder);
```

Существующий метод `applyPaletteToFolderCard` переименовать в `applyCornflowerPaletteToCard`. Создать параллельный `applyCanaryPaletteToCard` с теми же `set*` вызовами, но из `canaryPalette`. (Реализатор фазы 5 — проверить точный набор полей, прочитав текущий метод.)

### 8. `TasksRecyclerViewAdapter.java` — изменения

Параллельная пара методов и поле:

```java
private CornflowerPalette cornflowerPalette;
private com.shumidub.todoapprealm.ui.theme.CanaryPalette canaryPalette;

public void useCornflowerPalette(boolean enabled) {
    cornflowerPalette = enabled ? new CornflowerPalette(activity) : null;
    canaryPalette = null;
    notifyDataSetChanged();
}

public void useCanaryPalette(boolean enabled) {
    canaryPalette = enabled ? new com.shumidub.todoapprealm.ui.theme.CanaryPalette(activity) : null;
    cornflowerPalette = null;
    notifyDataSetChanged();
}
```

Все site'ы внутри адаптера, читающие `palette.*`, нужно расширить: переименовать `palette` field в одно из `cornflowerPalette` (или ввести локальный helper `activePalette()` возвращающий **либо текст/bg значения через массив `int[]`, либо использовать два if'а на каждом site**). Рекомендую второй путь — минимизирует diff. Реализатор фазы 5 сам выберет, что чище после чтения целого файла. Non-goal R16 разрешает дублирование.

### 9. `SmallTasksFragment.java` — изменения

Добавить getter палитры (используется секциями task-002 и BottomSheet task-001 если они инстанцируются изнутри fragment'а):

```java
private boolean isInCornflowerTab() {
    Fragment parent = getParentFragment();
    return parent instanceof FolderSlidingPanelFragment
            && ((FolderSlidingPanelFragment) parent).getTaskGroup() == 1;
}

private boolean isInCanaryTab() {
    Fragment parent = getParentFragment();
    return parent instanceof FolderSlidingPanelFragment
            && ((FolderSlidingPanelFragment) parent).getTaskGroup() == 2;
}

/** taskGroup of parent fragment, -1 если нет parent'а. */
public int getTabTaskGroup() {
    Fragment parent = getParentFragment();
    return parent instanceof FolderSlidingPanelFragment
            ? ((FolderSlidingPanelFragment) parent).getTaskGroup() : -1;
}
```

Точки применения (строки 205-208, 250, 260):

```java
if (isInCornflowerTab()) {
    tasksRecyclerViewAdapter.useCornflowerPalette(true);
    applyCornflowerToFragmentView();
} else if (isInCanaryTab()) {
    tasksRecyclerViewAdapter.useCanaryPalette(true);
    applyCanaryToFragmentView();
}
```

Аналогично на строках 250 и 260 (внутри `showAllTasks()`):

```java
if (isInCornflowerTab()) tasksRecyclerViewAdapter.useCornflowerPalette(true);
else if (isInCanaryTab()) tasksRecyclerViewAdapter.useCanaryPalette(true);
```

Новый метод:

```java
private void applyCanaryToFragmentView() {
    if (getView() == null) return;
    com.shumidub.todoapprealm.ui.theme.CanaryPalette p =
            new com.shumidub.todoapprealm.ui.theme.CanaryPalette(getContext());
    getView().setBackgroundColor(p.bg);
}
```

### 10. `TaskActionModeCallback.java` — изменения

Расширить blob на строке 82-84 (зависит от `taskGroup`, не `isCornflowerTab` — потому что ActionMode инстанцируется внутри SmallTasksFragment, и через MainActivity тут проще остаться по флагам):

```java
final int accentColor;
if (activity.isCornflowerTab()) {
    accentColor = new com.shumidub.todoapprealm.ui.theme.CornflowerPalette(activity).accent;
} else if (activity.isCanaryTab()) {
    accentColor = new com.shumidub.todoapprealm.ui.theme.CanaryPalette(activity).accent;
} else {
    accentColor = activity.getResources().getColor(R.color.colorAccent);
}
this.accentColor = accentColor;  // адаптировать по точному месту присваивания
```

(Реализатор фазы 5: сохранить локальную форму присваивания `accentColor`. `MainActivity.isCanaryTab()` уже добавлен в 5.1.)

### 11. Realm migration — координация с task-002

Поднимаем `SCHEMA_VERSION` с 3 до **4** одним шагом. Финальное состояние `RealmMigrations.migrate`:

```java
public static final long SCHEMA_VERSION = 4;

@Override
public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
    RealmSchema schema = realm.getSchema();

    if (oldVersion < 2) {
        schema.get("TaskObject").addRealmListField("extraFolderIds", Long.class);
    }

    if (oldVersion < 3) {
        schema.get("RealmFoldersContainer")
                .addRealmListField("folderOfTasksList2", schema.get("FolderTaskObject"));
    }

    if (oldVersion < 4) {
        // task-002: SectionObject + TaskObject.sectionId/position
        RealmObjectSchema sectionSchema = schema.create("SectionObject")
                .addField("id", long.class, FieldAttribute.PRIMARY_KEY)
                .addField("name", String.class)
                .addField("collapsedByDefault", boolean.class)
                .addField("currentlyCollapsed", boolean.class)
                .addField("parentFolderId", long.class)
                .addField("position", int.class);

        schema.get("TaskObject")
                .addField("sectionId", long.class)   // default 0 = "без секции"
                .addField("position", int.class)
                .transform(obj -> {
                    // backfill task.position = index в RealmList родительской папки.
                    // Реализация: пройти по всем FolderTaskObject, проставить position
                    // задачам в порядке их появления в folder.tasks.
                    // Делается отдельным проходом ниже, потому что transform работает
                    // на одной записи и не знает её позиции в RealmList.
                });

        // Backfill position для TaskObject (R5 task-002):
        // Для каждой папки во всех контейнерах — пройти RealmList<TaskObject>
        // и проставить position = index. Делать это на DynamicRealm:
        io.realm.RealmResults<DynamicRealmObject> folders = realm.where("FolderTaskObject").findAll();
        for (DynamicRealmObject folder : folders) {
            io.realm.RealmList<DynamicRealmObject> tasks = folder.getList("tasks");
            if (tasks != null) {
                for (int i = 0; i < tasks.size(); i++) {
                    tasks.get(i).setInt("position", i);
                }
            }
        }

        // task-003: folderOfTasksList3
        schema.get("RealmFoldersContainer")
                .addRealmListField("folderOfTasksList3", schema.get("FolderTaskObject"));
    }
}
```

**Порядок внутри блока `oldVersion < 4`:**
1. `schema.create("SectionObject")` — модель появляется первой.
2. `TaskObject.addField("sectionId")` + `addField("position")`.
3. Backfill `position` через `DynamicRealmObject` (точное имя RealmList в `FolderTaskObject` — проверить геттер, скорее всего "tasks"; реализатор фазы 5 уточнит, прочитав модель).
4. `RealmFoldersContainer.addRealmListField("folderOfTasksList3", ...)` — последним, не конфликтует с (1)-(3).

**Координация:** обе задачи (002 и 003) пишут в один `if (oldVersion < 4)` блок. Кто первым приземляется — кладёт свой раздел и поднимает SCHEMA_VERSION. Второй задачей добавляется только её раздел в тот же блок. Если они пойдут параллельно — нужен merge-конфликт в `RealmMigrations.java`, явно разрешаемый ревьюером в фазе 6.

### 12. `RealmFoldersContainer.java` — diff

```java
public class RealmFoldersContainer extends RealmObject implements Serializable{
    public RealmList<FolderTaskObject> folderOfTasksList;
    public RealmList<FolderTaskObject> folderOfTasksList2;
    /** Folders shown on the third Tasks tab (Canary palette). */
    public RealmList<FolderTaskObject> folderOfTasksList3;
    public RealmList<FolderNotesObject> folderOfNotesList;
    public RealmList<ReportObject> reportObjectList;
}
```

### 13. `App.java` — diff

Рядом с `folderOfTasksList2FromContainer`:

```java
public static RealmList<FolderTaskObject> folderOfTasksList3FromContainer;
```

В блоке `initRealm()` (после строки 138):

```java
folderOfTasksList3FromContainer = realmFoldersContainer.folderOfTasksList3;
```

### 14. `FolderTaskRealmController.java` — API extension

Сигнатуры (расширение существующих методов):

```java
/** Get folders for a given tab. group=0 → Tasks1, group=1 → Tasks2, group=2 → Tasks3. */
public static RealmList<FolderTaskObject> getFoldersList(int group){
    App.initRealm();
    switch (group) {
        case 1: return App.folderOfTasksList2FromContainer;
        case 2: return App.folderOfTasksList3FromContainer;
        default: return App.folderOfTasksListFromContainer;
    }
}

/** Tab index (0, 1, 2) the folder lives on. -1 если не в одном из container list. */
public static int getFolderGroup(FolderTaskObject folder){
    if (folder == null) return -1;
    if (App.folderOfTasksListFromContainer != null
            && App.folderOfTasksListFromContainer.contains(folder)) return 0;
    if (App.folderOfTasksList2FromContainer != null
            && App.folderOfTasksList2FromContainer.contains(folder)) return 1;
    if (App.folderOfTasksList3FromContainer != null
            && App.folderOfTasksList3FromContainer.contains(folder)) return 2;
    return -1;
}

public static java.util.List<FolderTaskObject> getAllFolders(){
    App.initRealm();
    java.util.List<FolderTaskObject> all = new ArrayList<>();
    if (App.folderOfTasksListFromContainer != null) all.addAll(App.folderOfTasksListFromContainer);
    if (App.folderOfTasksList2FromContainer != null) all.addAll(App.folderOfTasksList2FromContainer);
    if (App.folderOfTasksList3FromContainer != null) all.addAll(App.folderOfTasksList3FromContainer);
    return all;
}

public static void moveFolderToGroup(FolderTaskObject folder, int targetGroup){
    if (folder == null) return;
    int current = getFolderGroup(folder);
    if (current == targetGroup) return;
    App.initRealm();
    App.realm.executeTransaction((r) -> {
        if (App.folderOfTasksListFromContainer != null) App.folderOfTasksListFromContainer.remove(folder);
        if (App.folderOfTasksList2FromContainer != null) App.folderOfTasksList2FromContainer.remove(folder);
        if (App.folderOfTasksList3FromContainer != null) App.folderOfTasksList3FromContainer.remove(folder);
        getFoldersList(targetGroup).add(folder);
    });
}
```

В `deleteFolder(FolderTaskObject)` — добавить removal из третьего списка (строки 131-136 текущей версии):

```java
if (App.folderOfTasksList3FromContainer != null) {
    App.folderOfTasksList3FromContainer.remove(folderObject);
}
```

### 15. Status/NavigationBar — точный API

Используется `androidx.core.view.WindowInsetsControllerCompat`:

```java
WindowInsetsControllerCompat insets =
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
insets.setAppearanceLightStatusBars(true);      // R8 — Canary tab only
insets.setAppearanceLightNavigationBars(true);  // R8 — Canary tab only
```

Для Cornflower и default — `false` (тёмный фон → светлые иконки). Уже вписано в `applyTabChrome` секции 5.5.

### 16. BottomSheet / Sections integration (task-001, task-002)

- Task-001 BottomSheet берёт палитру через `FolderSlidingPanelFragment.isCanaryGroup() / isCornflowerGroup() / getActiveAccent()` (см. секцию 6.5). Изменений на стороне task-003 не требуется — task-001 в своём дизайне обязан учесть эти getter'ы и кейс `taskGroup == 2`.
- Task-002 секции рендерятся в `TasksRecyclerViewAdapter` (multi-view-type из task-002 R8). Когда таб == Canary, секционный header использует `canaryPalette` (через `useCanaryPalette` setter). Реализатор task-002 фазы 5 должен это учесть в R8: ветка `if (cornflowerPalette != null) ... else if (canaryPalette != null) ...` для tint header'а.

### 17. Risks

- **Backward compat (R6 кросс-задачи):** старая БД на v3 → миграция в v4 создаёт пустой `folderOfTasksList3`. Открытие Tasks3 в первый раз — `getFoldersList(2)` вернёт пустой `RealmList`, `FolderOfTaskRecyclerViewAdapter` отрендерит пустой recycler, `setEmptyStateIfNeed()` покажет empty state. Падений быть не должно — `FolderSlidingPanelFragment` уже корректно работает с пустым список на Tasks2 (та же ветка `taskGroup`).
  Проверить вручную: пользователь обновляет → открывает Tasks3 свайпом → видит пустой экран с empty state в палитре Canary → жмёт "+" → создаёт первую папку. Дополнительно — `SmallTaskFragmentPagerAdapter` с пустым `folderObjects.size()==0` не должен крашиться (та же проверка, что и для Tasks2 в текущем коде).
- **`viewPager.setOffscreenPageLimit(1)`:** при свайпе на Tasks3 (pos 3) — Tasks (pos 1) выгружается из памяти. При возврате — пересоздаётся. Behaviour идентичен текущему Tasks2 ↔ Tasks. Не риск.
- **`onPause`/`closeRealm`:** при swipe между табами `App.closeRealm()` не вызывается (срабатывает только в `onPause` Activity). `folderOfTasksList*FromContainer` живут от `initRealm()` до `closeRealm()`. Tasks3 список ре-инициализируется на каждом `initRealm` (App.java строки 137-139 + новая 139). OK.
- **Coordinating migrations:** если task-002 уже мерджится первой и пушит `SCHEMA_VERSION=4`, task-003 обязана **расширить тот же `if (oldVersion < 4)` блок**, а не добавлять `if (oldVersion < 5)`. Иначе старая прод-БД проскочит мимо `addRealmListField("folderOfTasksList3", ...)`. Обратное — если task-003 первой, тот же режим.
- **JsonSyncUtil backup/restore:** `JsonSyncUtil` сериализует `RealmFoldersContainer` целиком — новое поле `folderOfTasksList3` попадёт в JSON и при restore старых JSON-бэкапов (без `folderOfTasksList3`) поле останется пустым, не падает. Реализатор фазы 5 должен проверить (быстрый grep на `folderOfTasksList`), что JsonSyncUtil не имеет hardcoded списка из 2 контейнеров.
- **`ViewPager.setCurrentItem(1)` defaults (R2):** не меняется. Tasks остаётся стартовым табом.
- **`tintActionModeBarIfCornflower` переименование:** все callsite'ы внутри `MainActivity.java` (две штуки на строках 235, 241). Внешних вызовов из других файлов нет (grep'ом проверено — встречается только в `MainActivity.java`). Переименование безопасно.

### 18. Файлы, которые нужно создать / тронуть (итог)

Новые:
- `app/src/main/java/com/shumidub/todoapprealm/ui/theme/CanaryPalette.java`

Изменить:
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/styles.xml`
- `app/src/main/java/com/shumidub/todoapprealm/ui/activity/main/MainPagerAdapter.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/activity/main/MainActivity.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/fragment/FolderSlidingPanelFragment.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/adapter/FolderOfTaskRecyclerViewAdapter.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/TasksRecyclerViewAdapter.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/task/TaskActionModeCallback.java`
- `app/src/main/java/com/shumidub/todoapprealm/realmmodel/RealmFoldersContainer.java`
- `app/src/main/java/com/shumidub/todoapprealm/App.java`
- `app/src/main/java/com/shumidub/todoapprealm/RealmMigrations.java` (координация с task-002 — общий блок `oldVersion < 4`)
- `app/src/main/java/com/shumidub/todoapprealm/realmcontrollers/taskcontroller/FolderTaskRealmController.java`

## Tests
_skip (manual QA)_

## Implementation
_TBD фаза 5_

## Review
_TBD фаза 6_
