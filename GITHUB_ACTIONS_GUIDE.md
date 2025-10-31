# GitHub Actions Guide for Scale Finder

## What is GitHub Actions?

GitHub Actions is a **CI/CD (Continuous Integration/Continuous Deployment)** platform built into GitHub. Think of it as a robot that automatically does tasks for you whenever you push code or create releases.

## Why Use GitHub Actions?

### 🚀 **Automation**
Instead of manually building APKs on your computer, GitHub Actions builds them automatically in the cloud whenever you push code.

### ✅ **Quality Assurance**
Every time you push code, GitHub Actions:
- Runs your tests automatically
- Checks for code quality issues (lint)
- Verifies your app still builds

### 📦 **Easy Releases**
When you're ready to publish:
1. Create a release tag
2. GitHub Actions automatically builds the APK
3. APK is attached to your release automatically

### 👥 **Team Collaboration**
Everyone can see:
- ✅ If the latest code builds successfully
- ❌ If tests are failing
- 📊 Build history and logs

## What Can You Do With GitHub Actions?

### 1. **Continuous Integration (CI)**
**What:** Automatically test and build your app on every code push

**Benefits:**
- Catch bugs before they reach production
- Know immediately if code doesn't compile
- See test results without running them locally

**Workflow:** `ci.yml` - Runs on every push/PR

### 2. **Automated Releases**
**What:** Build and attach APK when you create a release

**Benefits:**
- No manual APK building
- Consistent release builds
- APK automatically attached to GitHub release

**Workflow:** `release.yml` - Runs when you create a release tag

### 3. **Code Quality Checks**
**What:** Run lint checks separately for fast feedback

**Benefits:**
- Quick feedback on code style issues
- Find problems before merging PRs

**Workflow:** `lint.yml` - Runs on every push/PR

## How It Works

### Step-by-Step Process:

1. **You push code** to GitHub
   ```
   git push origin main
   ```

2. **GitHub detects the push** and triggers workflows

3. **GitHub Actions starts a virtual machine** (Ubuntu) in the cloud

4. **The workflow runs:**
   - Checks out your code
   - Sets up Java & Android SDK
   - Runs tests
   - Builds APKs
   - Uploads results

5. **You see results** in the "Actions" tab on GitHub

### Workflow Structure:

```yaml
on:                  # WHEN to run
  push:              # Trigger on code push
  
jobs:                # WHAT to do
  build:             # Job name
    steps:           # Step-by-step instructions
      - checkout     # Get code
      - setup-java   # Install Java
      - run tests    # Run tests
      - build APK    # Build app
```

## How to Use

### Setting Up (One-Time)

1. **Push your code to GitHub** (if not already)
   ```bash
   git add .
   git commit -m "Add GitHub Actions workflows"
   git push origin main
   ```

2. **Go to your GitHub repository**
   - Click the "Actions" tab
   - You'll see the workflows I created

3. **Workflows run automatically!**
   - First push triggers the CI workflow
   - Check the Actions tab to see it running

### Daily Usage

#### **During Development:**

1. **Write code** locally
2. **Push to GitHub:**
   ```bash
   git push origin main
   ```
3. **Check Actions tab** - See if build passed ✅

#### **Creating a Release:**

1. **Create and push a tag:**
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

2. **On GitHub:**
   - Go to Releases → Draft a new release
   - Select your tag (v1.0.1)
   - Add release notes
   - Click "Publish release"

3. **GitHub Actions automatically:**
   - Builds release APK
   - Attaches it to the release
   - You can download it from the release page

## Viewing Results

### In GitHub:

1. Go to your repository
2. Click **"Actions"** tab (top navigation)
3. You'll see:
   - List of all workflow runs
   - ✅ Green = Success
   - ❌ Red = Failed (click to see why)
   - 🟡 Yellow = Running

4. **Click any run** to see:
   - Build logs
   - Test results
   - Downloadable APKs (in "Artifacts" section)

### Downloading APKs:

1. Go to Actions tab
2. Click on a workflow run
3. Scroll to bottom → "Artifacts" section
4. Download the APK you need

## Example Scenarios

### Scenario 1: You push new code
- ✅ CI workflow runs automatically
- ✅ Tests run
- ✅ APKs are built
- ✅ You can download them if needed

### Scenario 2: You create a release
- ✅ Release workflow runs
- ✅ Release APK is built
- ✅ APK is attached to the release
- ✅ Users can download from release page

### Scenario 3: Someone opens a Pull Request
- ✅ CI workflow runs
- ✅ You see if PR builds successfully
- ✅ You can review test results before merging

## Advanced Options (Later)

Once you're comfortable, you can:

- **Deploy to Play Store** - Automatically upload APKs to Google Play
- **Generate App Bundles** - Build AAB files for Play Store
- **Run on Multiple Android Versions** - Test on different SDK versions
- **Code Coverage Reports** - See how much of your code is tested
- **Slack/Discord Notifications** - Get notified when builds fail

## Troubleshooting

### Workflow Fails?

1. Click on the failed workflow
2. Click on the failed job
3. Expand steps to see error messages
4. Common issues:
   - Syntax errors in code → Fix code, push again
   - Missing dependencies → Check build.gradle.kts
   - Test failures → Fix tests

### Want to Skip CI?

Add `[skip ci]` to your commit message:
```bash
git commit -m "Update README [skip ci]"
```

### Need to Run Manually?

1. Go to Actions tab
2. Select workflow (e.g., "CI")
3. Click "Run workflow" button
4. Choose branch and run

## Summary

**GitHub Actions = Automated robot that:**
- ✅ Builds your app
- ✅ Runs tests
- ✅ Checks code quality
- ✅ Creates release APKs

**You benefit by:**
- 🚀 Faster development (automation)
- ✅ Higher quality (automated checks)
- 📦 Easier releases (one-click APKs)

Just push your code and watch it work! 🎉

