# FridgeChef

Type what's in your fridge and get recipes from the web, ranked by how much of your fridge each one uses. Each recipe comes with its ingredient quantities and step-by-step instructions. There's no recipe database to maintain: everything is fetched live.

## How it finds recipes

| Source | Key needed | How it works |
|---|---|---|
| **TheMealDB** (default) | No | Queries each of your ingredients, keeps the meals that match the most of them, then fetches each one to work out what you have and what you'd need to buy. About 600 curated recipes, with quantities as published ("3/4 cup", "200g"). |
| **Spoonacular** (optional) | Free key | One query over all your ingredients, across hundreds of thousands of recipes. Gives numeric quantities, so you can **rescale servings** and switch between metric and US units. The free tier allows about 150 points a day. |

Paste a Spoonacular key under **Settings** (gear icon) to switch to it. If the key is rejected or the daily quota runs out, the app falls back to TheMealDB on its own and tells you.

Ranking favours recipes that use more of your ingredients and need fewer extra purchases. Pantry staples (salt, pepper, water, oil) never count as missing.

## Features
- Ingredient chips: type one item, or paste a comma-separated list. There's also a quick-add row. The list is saved between launches.
- Results with a photo, how many of your ingredients each recipe uses, and what's missing.
- Recipe screen: ingredient list with quantities, ✓ for items you have and a 🛒 for items to buy, a servings stepper (Spoonacular only), numbered steps, and links to the original recipe and video.
- Material 3, dynamic colour on Android 12 and later, dark mode.

## Build

**Android Studio:** open this folder, let Gradle sync, then press Run. Requires JDK 17; Android Studio's bundled JDK works.

**Command line:** `./gradlew assembleDebug`. The APK goes to `app/build/outputs/apk/debug/app-debug.apk`.

**No local Android setup:** push this folder to a GitHub repo. The included workflow (`.github/workflows/build-apk.yml`) builds the APK, runs the unit tests and attaches the APK as a downloadable artifact on the Actions run.

To install on a phone, allow "Install unknown apps" for your file manager or browser, then open the APK.

## Project layout
```
app/src/main/java/dev/fridgechef/
  data/
    MealDbProvider.kt       TheMealDB search + parsing
    SpoonacularProvider.kt  Spoonacular search + parsing
    IngredientMatcher.kt    fuzzy "do I have this?" matching, pantry staples
    Quantities.kt           amount formatting (1½, ¾) and servings scaling
    Settings.kt             saved fridge, API key, units
  ui/
    MainViewModel.kt        state, search, fallback, navigation
    FridgeScreen.kt / ResultsScreen.kt / RecipeScreen.kt / SettingsDialog.kt
app/src/test/.../ParsersTest.kt  JSON parsing and matching tests
```

To add another recipe source (Edamam, or an LLM that writes recipes), implement `RecipeProvider` and select it in `MainViewModel`.
