# Roadmap

These are proposed improvements, not completed features.

## 1. [Different budgets for each weekday](https://github.com/LunniyJomolungma/studydock/issues/1)

Replace the single daily budget with a seven-day availability pattern. Preserve a simple default, persist preferences independently of task data, and test zero-capacity days and transitions between weeks.

## 2. [Log completed study sessions](https://github.com/LunniyJomolungma/studydock/issues/2)

Let a user record actual minutes against a task, reducing remaining work explicitly. Store timestamped sessions, prevent negative estimates and support undoing the latest entry. Planned blocks must not be mistaken for completed work.

## 3. [Import tasks with a preview](https://github.com/LunniyJomolungma/studydock/issues/3)

Add CSV import with a validation preview before saving. Report invalid dates, duplicate rows and unsupported columns. No partial import should occur if validation fails; include Unicode and quoted-field tests.
