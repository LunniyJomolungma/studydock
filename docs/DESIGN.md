# Design notes

## Boundaries

`Task` validates domain data. `Planner` is a pure calculation that accepts an explicit current date. `TaskStore` owns persistence. `StudyDock` owns Swing widgets and delegates calculations and saves. No application code performs network requests.

## Scheduling

Earliest deadline first gives a predictable explanation for each placement. Priority matters only when deadlines tie. This intentionally avoids a hidden score that users cannot interpret. Each minute of active work is counted either in a scheduled block or in a shortfall.

The horizon includes today. Deadlines are inclusive. A day is available only when allowed by the weekend setting. The budget is a duration, not a free/busy calendar. Blocks may be shorter than 60 minutes when a task or day has less time left.

## Persistence

The first line is `STUDYDOCK<TAB>1`. Each following row contains UUID, Base64 title, Base64 subject, ISO date, remaining minutes and priority. Text encoding is UTF-8. A malformed row is an error, not a row to skip.

The writer holds a file lock for the lifetime of the app. A candidate state is saved before the UI adopts it, so a failed save keeps the previous in-memory task list. The lock file may remain on disk after exit; the OS lock itself is released and does not need manual deletion.

There is a 2 MB file limit and a 2000-task save limit. Preferences such as budget and weekend inclusion are session settings and are not persisted yet. CSV export includes unallocated work and its reason. CSV contains user-supplied text; treat it as data when opening in spreadsheet software.

## Tradeoffs

- Swing provides a desktop interface with the JDK alone, at the cost of some platform-specific rendering.
- File storage is simple to inspect and test. It is not encrypted or designed for multi-device collaboration.
- Planner execution and file saves run on the UI thread. This is suitable for the stated small-file limits; background persistence is a future extension.
- The app tracks remaining estimates, not actual study duration. It does not pretend that scheduled work has already happened.

## Tests

Tests use explicit dates and temporary directories. They verify inclusive deadlines, capacity conservation, priority ordering, weekend rules, failed parsing without overwriting the source, file locking and CSV quoting. They do not depend on the wall clock or external services.
