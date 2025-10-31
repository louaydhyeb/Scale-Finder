# GitHub Actions Workflows

This directory contains CI/CD workflows for the Scale Finder Android app.

## Available Workflows

### 1. CI (`ci.yml`)
**Triggers:** Every push/PR to main/master/develop branches

**What it does:**
- Runs lint checks
- Runs unit tests
- Builds debug and release APKs
- Uploads APKs as downloadable artifacts

**How to use:**
- Push code to your branch → workflow runs automatically
- Check results in the "Actions" tab on GitHub
- Download built APKs from the workflow run page

### 2. Release Build (`release.yml`)
**Triggers:** When you create a GitHub release tag

**What it does:**
- Builds release APK
- Attaches APK to the GitHub release

**How to use:**
1. Create a new tag: `git tag v1.0.0`
2. Push the tag: `git push origin v1.0.0`
3. Go to GitHub → Releases → Draft a new release
4. Select your tag and publish
5. Workflow automatically builds and attaches APK

### 3. Lint Check (`lint.yml`)
**Triggers:** Every push/PR (quick feedback)

**What it does:**
- Runs Android lint checks only
- Provides fast feedback on code quality issues

## How to View Results

1. Go to your GitHub repository
2. Click the "Actions" tab
3. Click on any workflow run to see:
   - Build logs
   - Test results
   - Downloadable artifacts (APKs)

## Benefits

✅ **Automated Testing** - Catch bugs before they reach production
✅ **Build Verification** - Know your code compiles
✅ **Release Automation** - One-click APK generation for releases
✅ **Code Quality** - Lint checks keep code consistent
✅ **Team Collaboration** - Everyone can see build status

## Common Workflow Status

- ✅ Green checkmark = Everything passed
- ❌ Red X = Something failed (check logs)
- 🟡 Yellow circle = Currently running
- ⏸️ Gray = Skipped or cancelled

