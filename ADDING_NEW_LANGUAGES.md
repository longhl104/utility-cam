# How to Add New Languages to Utility Cam

## Quick Start Guide

### Step 1: Create Language Folder

Create a new folder in `app/src/main/res/` with the format `values-{language-code}`:

**Common language codes:**

- Spanish: `values-es`
- French: `values-fr`
- German: `values-de`
- Italian: `values-it`
- Japanese: `values-ja`
- Korean: `values-ko`
- Chinese (Simplified): `values-zh-rCN`
- Chinese (Traditional): `values-zh-rTW`
- Russian: `values-ru`
- Arabic: `values-ar`
- Hindi: `values-hi`

### Step 2: Copy strings.xml

Copy the `strings.xml` file from either:

- `app/src/main/res/values/strings.xml` (English - default)
- `app/src/main/res/values-pt/strings.xml` (Portuguese - example)

### Step 3: Translate Strings

Open the copied `strings.xml` and translate all text between the `<string>` tags.

**Example for Spanish (`values-es/strings.xml`):**

```xml
<!-- English -->
<string name="app_name">Utility Cam</string>
<string name="gallery_no_photos">No utility photos yet</string>

<!-- Spanish -->
<string name="app_name">Utility Cam</string>
<string name="gallery_no_photos">Aún no hay fotos utilitarias</string>
```

### Step 4: Test

1. Change your device language to the newly added language
2. Run the app
3. Verify all text displays correctly in the new language

## Translation Tips

### DO

- ✅ Keep the same formatting placeholders (`%s`, `%d`, `%1$s`, etc.)
- ✅ Maintain the same number of placeholders
- ✅ Keep special characters like `\n` (newline) and `\"` (escaped quotes)
- ✅ Preserve HTML tags if present
- ✅ Keep `formatted="false"` attribute for strings that have it

### DON'T

- ❌ Translate placeholder variables (keep `%s`, `%d` as-is)
- ❌ Change the `name` attribute of the string resource
- ❌ Remove or add extra placeholders
- ❌ Translate technical terms like "Utility Cam" (brand name)

## Example Translations

### String with Placeholder

```xml
<!-- English -->
<string name="photo_detail_expires_in">Expires in %s</string>

<!-- Portuguese -->
<string name="photo_detail_expires_in">Expira em %s</string>

<!-- Spanish -->
<string name="photo_detail_expires_in">Expira en %s</string>
```

### String with Multiple Placeholders

```xml
<!-- English -->
<string name="settings_version_format">Version %1$s (%2$d)</string>

<!-- Portuguese -->
<string name="settings_version_format">Versão %1$s (%2$d)</string>

<!-- Spanish -->
<string name="settings_version_format">Versión %1$s (%2$d)</string>
```

### Plurals

```xml
<!-- English -->
<plurals name="notification_cleanup_message">
    <item quantity="one">%d expired photo was automatically deleted</item>
    <item quantity="other">%d expired photos were automatically deleted</item>
</plurals>

<!-- Portuguese -->
<plurals name="notification_cleanup_message">
    <item quantity="one">%d foto expirada foi automaticamente excluída</item>
    <item quantity="other">%d fotos expiradas foram automaticamente excluídas</item>
</plurals>
```

## No Code Changes Required

Once you add the translated `strings.xml` file to the appropriate `values-{lang}` folder, the app will automatically use it when the device language matches. No Kotlin code changes needed!

## Testing Checklist

After adding a new language:

- [ ] All screen titles are translated
- [ ] All button labels are translated
- [ ] All error messages are translated
- [ ] All dialog messages are translated
- [ ] Notifications appear in the new language
- [ ] Widget text is translated
- [ ] Settings screen is fully translated
- [ ] No English text appears when using the new language

## Need Help?

Refer to existing translations:

- English: `app/src/main/res/values/strings.xml`
- Portuguese: `app/src/main/res/values-pt/strings.xml`

## Language Priority

Android selects languages in this order:

1. Exact match (e.g., `values-pt-rBR` for Brazilian Portuguese)
2. Language match (e.g., `values-pt` for Portuguese)
3. Default (e.g., `values` for English)
