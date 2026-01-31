# App-ads.txt Setup Instructions

## What is app-ads.txt?

App-ads.txt is a text file that helps prevent unauthorized inventory sales and protects your app's ad revenue. It's similar to ads.txt for websites but designed specifically for mobile apps.

## Your app-ads.txt File

I've created the file `app-ads.txt` in your project root with the following content:

```
google.com, pub-8461786124508841, DIRECT, f08c47fec0942fa0
```

### File Breakdown

- **google.com** - Ad system domain (Google AdMob)
- **pub-8461786124508841** - Your AdMob Publisher ID
- **DIRECT** - Relationship type (you directly control this inventory)
- **f08c47fec0942fa0** - Google's certification authority ID

## Setup Steps

### Step 1: Get Your Developer Website Domain

You need a publicly accessible website where you can host the app-ads.txt file. This must be the **same domain** listed in your Google Play Store listing.

#### Where to Find Your Developer Website

1. Go to Google Play Console
2. Navigate to: **Store presence** → **Main store listing**
3. Look for the "Website" field
4. This is the domain you'll use (e.g., `example.com`)

**Important:** The domain must be **exactly** as listed on Google Play, including:
- Include `www.` if your Play Store listing includes it
- Don't include `www.` if your Play Store listing doesn't have it

### Step 2: Upload app-ads.txt to Your Website

Upload the `app-ads.txt` file to the **root directory** of your website.

#### Examples of Correct Placement

✅ **Correct:**
- `https://yourdomain.com/app-ads.txt`
- `https://www.yourdomain.com/app-ads.txt` (if your Play Store has www)

❌ **Incorrect:**
- `https://yourdomain.com/files/app-ads.txt` (not in root)
- `https://yourdomain.com/ads/app-ads.txt` (subdirectory)
- `https://subdomain.yourdomain.com/app-ads.txt` (wrong subdomain)

#### How to Upload (Common Methods)

##### Option 1: cPanel File Manager
1. Log in to your hosting cPanel
2. Go to File Manager
3. Navigate to `public_html` folder (this is your website root)
4. Click "Upload"
5. Upload the `app-ads.txt` file
6. Make sure it's directly in `public_html`, not in any subfolder

##### Option 2: FTP
1. Connect to your server via FTP (FileZilla, WinSCP, etc.)
2. Navigate to your website root (usually `public_html` or `www`)
3. Upload `app-ads.txt` to this directory

##### Option 3: WordPress
1. Use an FTP client or cPanel File Manager
2. Navigate to your WordPress root directory (where `wp-config.php` is)
3. Upload `app-ads.txt` there
4. Alternatively, use a plugin like "ads.txt Manager" and add the content

##### Option 4: GitHub Pages / Static Hosting
1. Add `app-ads.txt` to your repository root
2. Commit and push
3. The file will be available at `https://yourusername.github.io/app-ads.txt`

### Step 3: Verify the File is Accessible

Before proceeding, test that your file is publicly accessible:

1. Open a web browser
2. Go to: `https://yourdomain.com/app-ads.txt`
3. You should see the text content:
   ```
   google.com, pub-8461786124508841, DIRECT, f08c47fec0942fa0
   ```

**Troubleshooting:**
- ✅ File loads? Great, proceed to Step 4
- ❌ 404 Error? File is not in the correct location
- ❌ Download prompt? Server might not be serving .txt files correctly (see fixes below)

#### Fix for File Not Loading

If the file isn't loading, add this to your `.htaccess` file (in the same directory):

```apache
<Files "app-ads.txt">
    ForceType text/plain
    Header set Content-Type "text/plain; charset=utf-8"
</Files>
```

### Step 4: Add Developer Website to Google Play Console

If you haven't already added your website to Google Play:

1. Go to Google Play Console
2. Navigate to: **Store presence** → **Main store listing**
3. Scroll to the "Contact details" section
4. Enter your website in the "Website" field
5. Click "Save"

**Important:** This must match where you uploaded app-ads.txt!

Examples:
- If app-ads.txt is at `https://example.com/app-ads.txt`
  → Website field should be: `https://example.com`
- If app-ads.txt is at `https://www.example.com/app-ads.txt`
  → Website field should be: `https://www.example.com`

### Step 5: Verify in AdMob Console

1. Go to AdMob Console: https://apps.admob.com/
2. Navigate to: **Apps** → Select your app
3. Click on **App settings**
4. Scroll to the "app-ads.txt" section
5. Click **"Verify app-ads.txt"** or **"Crawl now"**

AdMob will:
- Check if the file exists at your domain
- Verify it contains the correct publisher ID
- Validate the file format

#### Verification Status

- **✅ Verified** - Great! You're all set
- **⚠️ Pending** - AdMob is checking (can take a few minutes to 24 hours)
- **❌ Not found** - Double-check your file location and domain

### Step 6: Wait for Crawling (if needed)

AdMob automatically crawls app-ads.txt files periodically, but initial verification can take:
- **Best case:** A few minutes
- **Typical:** 1-24 hours
- **Worst case:** Up to 48 hours

**Don't worry if it's not immediate!** As long as the file is publicly accessible, AdMob will find it.

## Common Issues and Fixes

### Issue 1: "app-ads.txt not found"

**Causes:**
- File not in root directory
- Domain mismatch between Play Store and hosting
- File name incorrect (must be `app-ads.txt`, all lowercase)

**Fixes:**
1. Verify file is at: `https://yourdomain.com/app-ads.txt`
2. Check domain in Play Console matches hosting
3. Ensure filename is exactly `app-ads.txt` (not `app-ads.txt.txt`)

### Issue 2: "Publisher ID mismatch"

**Causes:**
- Wrong publisher ID in file
- Extra spaces or characters

**Fix:**
1. Verify your publisher ID in AdMob (should be `pub-8461786124508841`)
2. Ensure no extra spaces in the file
3. File should be exactly one line

### Issue 3: "Invalid format"

**Causes:**
- Extra blank lines
- Missing commas
- Wrong encoding

**Fix:**
1. Ensure file contains exactly:
   ```
   google.com, pub-8461786124508841, DIRECT, f08c47fec0942fa0
   ```
2. Save as UTF-8 encoding
3. No blank lines before or after

### Issue 4: "Domain not accessible"

**Causes:**
- Website is down
- SSL certificate issues
- File permissions

**Fixes:**
1. Ensure website is live and accessible
2. Check SSL certificate is valid (for https sites)
3. Set file permissions to 644 (readable by all)

### Issue 5: "www vs non-www mismatch"

**Problem:** Your Play Store has `www.example.com` but app-ads.txt is at `example.com` (or vice versa)

**Fix Option 1** - Upload to both:
- Upload to `https://example.com/app-ads.txt`
- Upload to `https://www.example.com/app-ads.txt`

**Fix Option 2** - Set up redirect:
Add to `.htaccess`:
```apache
RewriteEngine On
RewriteCond %{HTTP_HOST} ^example\.com [NC]
RewriteRule ^(.*)$ https://www.example.com/$1 [L,R=301]
```

## Don't Have a Website?

If you don't have a developer website, you have these options:

### Option 1: Create a Simple Website (Free)

Use a free hosting service:

1. **GitHub Pages** (Free)
   - Create a GitHub account
   - Create a repository named `yourusername.github.io`
   - Upload `app-ads.txt` to the repository
   - Your site: `https://yourusername.github.io`
   - Add this URL to Google Play Console

2. **Netlify** (Free)
   - Sign up at netlify.com
   - Drag and drop your `app-ads.txt` file
   - Get a free subdomain: `yourapp.netlify.app`
   - Add this URL to Google Play Console

3. **Google Sites** (Free)
   - Create a free Google Site
   - Note: You'll need to verify file upload capability

### Option 2: Use a Domain with Simple Hosting

Purchase a cheap domain ($10-15/year) with basic hosting:
- Namecheap
- GoDaddy
- Bluehost

Even the cheapest plan works fine for app-ads.txt!

### Option 3: Skip for Now (Not Recommended)

You can skip app-ads.txt, but:
- ⚠️ Reduces trust in your ad inventory
- ⚠️ May lower ad fill rates
- ⚠️ Could reduce eCPM
- ✅ Ads will still work

## Verification Checklist

Use this checklist to ensure everything is correct:

- [ ] app-ads.txt file created with correct publisher ID
- [ ] File uploaded to website root directory
- [ ] File accessible at `https://yourdomain.com/app-ads.txt`
- [ ] Domain in Google Play Console matches hosting
- [ ] Domain includes/excludes `www` correctly
- [ ] No extra spaces or characters in file
- [ ] File is UTF-8 encoded plain text
- [ ] Triggered verification in AdMob Console
- [ ] Waited at least 24 hours for crawling

## After Setup

Once app-ads.txt is verified:

1. **Monitor in AdMob Console**
   - Check if status changes to "Verified"
   - Usually appears within 24-48 hours

2. **Check for Warnings**
   - AdMob will alert you if issues are detected
   - Fix any warnings promptly

3. **Keep File Updated**
   - If you change ad networks, update the file
   - If publisher ID changes (rare), update the file

4. **Don't Delete or Move**
   - Keep app-ads.txt in place permanently
   - File must remain accessible at all times

## Quick Reference

### Your Setup Details

- **Publisher ID:** `pub-8461786124508841`
- **File Content:** `google.com, pub-8461786124508841, DIRECT, f08c47fec0942fa0`
- **File Location:** `app-ads.txt` (created in project root)
- **Where to Upload:** Root of your developer website
- **Final URL:** `https://yourdomain.com/app-ads.txt`

### Testing Commands

**Test file accessibility:**
```bash
curl -I https://yourdomain.com/app-ads.txt
```

Should return: `200 OK` with `Content-Type: text/plain`

**View file content:**
```bash
curl https://yourdomain.com/app-ads.txt
```

Should display:
```
google.com, pub-8461786124508841, DIRECT, f08c47fec0942fa0
```

## Additional Resources

- **IAB Tech Lab Spec:** https://iabtechlab.com/ads-txt/
- **Google AdMob Help:** https://support.google.com/admob/answer/9889911
- **App-ads.txt Guide:** https://developers.google.com/admob/android/app-ads-txt
- **Verification Tool:** Check in AdMob Console → Apps → App Settings

## Need Help?

If you're stuck:

1. **Check AdMob Console** - Look for specific error messages
2. **Test File Access** - Make sure you can access the URL in a browser
3. **Verify Domain** - Ensure Play Store and hosting domains match
4. **Wait Longer** - Sometimes takes up to 48 hours
5. **Contact Support** - AdMob support can help with specific issues

## Summary

1. ✅ File created: `app-ads.txt` in project root
2. 📤 Next: Upload to your website root
3. 🔗 Ensure: Accessible at `https://yourdomain.com/app-ads.txt`
4. ✔️ Verify: In AdMob Console
5. ⏳ Wait: Up to 24-48 hours for verification

---

**Status:** app-ads.txt file created
**Next Step:** Upload to your developer website root
**Your Publisher ID:** pub-8461786124508841
**Verification:** AdMob Console → Apps → App Settings
