# Action Buttons Rule
- Always use `setDebouncedClickListener` (instead of `setOnClickListener`) for all action buttons and clickable elements to prevent double-clicking bugs.

# MVI Architecture Rule
- Always route user interactions (like item clicks) through the `ViewModel` via an `Action`. Let the `ViewModel` handle logic/data transformation and have it emit an `Event` for the `Activity` to consume (e.g., for navigation). Do NOT put navigation logic directly inside adapter callbacks or inline in the Activity.
- `UiState` must have 3 explicit states: `Loading`, `Success`, and `Error`.
- For `Loading` state, use the `showLoading()` function provided by the base activity.
- For `Success` and `Error` states, always call the `hideLoading()` function provided by the base activity.
- For `Error` state, display the error using the system's `Toast` after hiding the loading state.

# Adapter Initialization Rule
- Always initialize Adapters in Activities using the `by lazy` delegate (e.g., `private val adapter by lazy { ... }`). Do not use `lateinit var` or inline assignment in layout setup functions.

# Import Rule
- Always import classes at the top of the file. Do NOT use fully qualified class names inline in the code (e.g., use `Intent` instead of `android.content.Intent`).

# Core Module Rule
- Always check the `core` module (e.g., base classes, utility functions, extensions) before writing new code to see if there are existing functions to use or classes to inherit, to avoid duplication and maintain consistency.
