# Environment Variables Setup

## Overview

Sensitive credentials like keystore passwords are now stored in a `.env` file instead of `gradle.properties` for better security.

## Setup Instructions

### 1. The .env File

Your `.env` file has been created in the project root with your credentials:

```
KEYSTORE_PASSWORD=LucianNohra@20104
KEY_PASSWORD=LucianNohra@20104
```

**Location:** `D:\Projects\utility-cam\.env`

### 2. Security Setup

The following files now protect your credentials:

#### .gitignore
The `.env` file is automatically excluded from version control:
```
.env
```

This prevents your passwords from being committed to Git.

#### gradle.properties
Sensitive credentials have been **removed** from `gradle.properties`. This file is also in `.gitignore`.

### 3. How It Works

The `app/build.gradle.kts` now includes a function that:
1. Reads the `.env` file from the project root
2. Parses key-value pairs
3. Uses them in the signing configuration

```kotlin
val envVars = loadEnvFile()

signingConfigs {
    create("release") {
        storeFile = file("../utility-cam-release.keystore")
        storePassword = envVars["KEYSTORE_PASSWORD"] ?: ""
        keyAlias = "utility-cam"
        keyPassword = envVars["KEY_PASSWORD"] ?: ""
    }
}
```

### 4. Fallback Options

The build script checks for credentials in this order:
1. `.env` file (primary)
2. System environment variables (fallback)
3. Empty string (last resort)

This allows flexibility for CI/CD pipelines.

## For Team Members

### Setting Up Locally

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Add your credentials:**
   Edit `.env` and replace placeholder values:
   ```
   KEYSTORE_PASSWORD=your_actual_password
   KEY_PASSWORD=your_actual_password
   ```

3. **Never commit .env:**
   - The file is in `.gitignore`
   - Share credentials securely (not via Git)

### Files in Version Control

✅ **Committed to Git:**
- `.env.example` - Template file (no actual credentials)
- `.gitignore` - Includes `.env`
- `build.gradle.kts` - Updated to read from `.env`

❌ **NOT in Git:**
- `.env` - Contains actual credentials
- `gradle.properties` - May contain local settings

## CI/CD Setup

For automated builds (GitHub Actions, Jenkins, etc.), set environment variables:

### GitHub Actions Example
```yaml
env:
  KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
  KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
```

The build script will automatically use these environment variables as a fallback.

### GitLab CI Example
```yaml
variables:
  KEYSTORE_PASSWORD: $KEYSTORE_PASSWORD
  KEY_PASSWORD: $KEY_PASSWORD
```

## Building the App

No changes needed! Just build as usual:

```bash
# Debug build (doesn't need signing)
./gradlew assembleDebug

# Release build (uses .env credentials)
./gradlew assembleRelease

# Bundle for Play Store
./gradlew bundleRelease
```

## Troubleshooting

### Build fails with "keystore password was incorrect"

**Cause:** `.env` file not found or has wrong credentials

**Fix:**
1. Verify `.env` exists in project root
2. Check passwords are correct (no quotes, no spaces)
3. Ensure file format is correct:
   ```
   KEYSTORE_PASSWORD=your_password
   KEY_PASSWORD=your_password
   ```

### Build works locally but fails in CI/CD

**Cause:** Environment variables not set in CI

**Fix:**
Set `KEYSTORE_PASSWORD` and `KEY_PASSWORD` as secrets/variables in your CI/CD platform.

### Credentials in Git history

**Danger:** If credentials were previously committed to `gradle.properties`

**Fix:**
1. Change your keystore passwords
2. Use BFG Repo-Cleaner or git-filter-repo to remove from history
3. Force push (be careful!)

## Security Best Practices

### ✅ DO:
- Keep `.env` in `.gitignore`
- Use different passwords for different keystores
- Rotate passwords periodically
- Share credentials via secure channels (password manager, encrypted files)
- Use CI/CD secrets for automated builds

### ❌ DON'T:
- Commit `.env` to Git
- Share credentials in chat/email
- Use the same password for everything
- Store credentials in code comments
- Push credentials to public repositories

## File Structure

```
utility-cam/
├── .env                    ← Your actual credentials (not in Git)
├── .env.example            ← Template (in Git)
├── .gitignore              ← Excludes .env
├── gradle.properties       ← No credentials (in .gitignore)
└── app/
    └── build.gradle.kts    ← Reads from .env
```

## Migration Summary

### What Changed

1. **Created `.env`** with your credentials
2. **Removed credentials** from `gradle.properties`
3. **Updated `.gitignore`** to exclude `.env`
4. **Modified `build.gradle.kts`** to read from `.env`
5. **Created `.env.example`** as a template

### What Stayed the Same

- Build commands (no changes)
- Keystore file location
- Key alias
- App signing process

## Additional Notes

### Multiple Developers

Each developer should have their own `.env` file with their credentials. Never share `.env` files directly.

### Different Keystores

If team members use different keystores (debug vs release), they can have different values in their `.env`:

**Developer A (.env):**
```
KEYSTORE_PASSWORD=password_for_debug_keystore
KEY_PASSWORD=password_for_debug_keystore
```

**Developer B (.env):**
```
KEYSTORE_PASSWORD=password_for_release_keystore
KEY_PASSWORD=password_for_release_keystore
```

### Backup Your Credentials

Store your `.env` file securely:
- Password manager (1Password, LastPass, Bitwarden)
- Encrypted backup drive
- Secure note-taking app

Don't rely solely on the local file!

## Summary

✅ Credentials moved from `gradle.properties` to `.env`
✅ `.env` excluded from Git via `.gitignore`
✅ `build.gradle.kts` updated to read from `.env`
✅ `.env.example` created as template
✅ Fallback to environment variables for CI/CD
✅ Build process unchanged

**Your credentials are now secure and separated from your code!** 🔒

---

**Created:** February 1, 2026
**Files Modified:**
- `gradle.properties` (removed credentials)
- `app/build.gradle.kts` (added .env loader)
- `.gitignore` (added .env)

**Files Created:**
- `.env` (your actual credentials)
- `.env.example` (template)
- `ENV_SETUP.md` (this file)
