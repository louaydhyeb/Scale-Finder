# Git Flow & GitHub Actions Workflow Guide

## Branch Strategy

```
master (production)
  └── dev (development)
       └── feature/feature-name (feature branches)
```

### Branch Roles:
- **master**: Production-ready code, stable releases
- **dev**: Active development, integration branch
- **feature/***: Individual features developed from dev

## Workflow Overview

### 1. Feature Development

```bash
# Start from dev
git checkout dev
git pull origin dev

# Create feature branch
git checkout -b feature/my-new-feature

# Make changes, commit
git add .
git commit -m "Add new feature"
git push -u origin feature/my-new-feature
```

### 2. Create Pull Request to Dev

1. Go to GitHub → **Pull requests** tab
2. Click **"New pull request"**
3. Set:
   - **Base:** `dev` (merge TO)
   - **Compare:** `feature/my-new-feature` (merge FROM)
4. Fill in description and create PR

### 3. Automated CI on PR

**What happens automatically:**
- ✅ Code validation runs
- ✅ Lint checks
- ✅ Tests execute
- ✅ APK builds
- ✅ Results commented on PR

**After CI passes:**
- Review the PR
- Merge into `dev` (merge button on PR page)

### 4. Merge Dev to Master (Button Trigger)

When `dev` is stable and ready for production:

1. **Go to GitHub → Actions tab**
2. **Select "Merge Dev to Master" workflow**
3. **Click "Run workflow"** button (top right)
4. **Fill in options:**
   - Version name (optional): e.g., "1.0.1"
   - Create tag? (optional): Check if you want to tag this release
5. **Click "Run workflow"** (green button)

**What happens:**
1. ✅ Validates dev branch (lint, tests, build)
2. ✅ Merges dev → master (if validation passes)
3. ✅ Builds master branch
4. ✅ Creates tag (if requested)
5. ✅ Creates release (if tag created)

## Workflow Files Explained

### `ci.yml`
**Purpose:** Continuous Integration  
**Triggers:** Push to dev/master, PRs to dev/master  
**What it does:**
- Runs lint
- Runs tests
- Builds debug & release APKs
- Uploads APKs as artifacts

### `pr-to-dev.yml`
**Purpose:** Validate PRs before merge  
**Triggers:** PRs targeting dev branch  
**What it does:**
- Validates code on PR
- Comments results on PR
- Prevents broken code from merging

### `merge-dev-to-master.yml` ⭐
**Purpose:** Merge dev to master with button  
**Triggers:** Manual (workflow_dispatch)  
**What it does:**
1. Validates dev branch
2. Merges dev → master (if validation passes)
3. Builds master
4. Creates tag/release (optional)

**How to use:**
1. GitHub → Actions
2. "Merge Dev to Master"
3. "Run workflow" button
4. Fill options and run

### `release.yml`
**Purpose:** Build release APKs  
**Triggers:** When you create a GitHub release tag  
**What it does:**
- Builds release APK
- Attaches to GitHub release

### `lint.yml`
**Purpose:** Fast lint feedback  
**Triggers:** Push/PR to dev/master  
**What it does:**
- Quick lint check only

## Step-by-Step: Complete Feature Flow

### Development Phase

```bash
# 1. Update local dev
git checkout dev
git pull origin dev

# 2. Create feature branch
git checkout -b feature/add-dark-mode

# 3. Make changes and commit
git add .
git commit -m "Add dark mode feature"
git push -u origin feature/add-dark-mode
```

### PR Phase

1. **Create PR on GitHub:**
   - Base: `dev`
   - Compare: `feature/add-dark-mode`
   - CI runs automatically ✅

2. **Review:**
   - Check CI results in PR
   - Review code
   - Request changes if needed

3. **Merge PR:**
   - Click "Merge pull request"
   - Feature is now in `dev`

### Release Phase (Dev → Master)

1. **Go to Actions tab**
2. **Select "Merge Dev to Master"**
3. **Click "Run workflow"**
4. **Options:**
   ```
   Version: 1.0.1
   Create tag: ✓ (checked)
   ```
5. **Click "Run workflow"**

**Result:**
- ✅ Dev validated
- ✅ Dev merged to master
- ✅ Master built
- ✅ Tag `v1.0.1` created
- ✅ Release created with APK

## Protecting Branches (Recommended)

Set up branch protection rules:

1. **Go to:** Settings → Branches
2. **Add rule for `master`:**
   - ✅ Require pull request reviews
   - ✅ Require status checks to pass
   - ✅ Require "Merge Dev to Master" workflow to pass
   - ✅ Include administrators

3. **Add rule for `dev`:**
   - ✅ Require status checks to pass
   - ✅ Require PR reviews (1 approval)

## GitHub Actions UI Guide

### Viewing Workflows

1. **Actions Tab** → See all workflow runs
2. **Green ✅** = Success
3. **Red ❌** = Failed (click to see why)
4. **Yellow 🟡** = Running

### Running Manual Workflow

**"Merge Dev to Master" workflow:**
1. Actions → "Merge Dev to Master"
2. Click **"Run workflow"** (right side)
3. Select branch: `dev`
4. Fill inputs (version, tag)
5. Click **"Run workflow"** button

### Downloading Artifacts

1. Go to a workflow run
2. Scroll to bottom → **"Artifacts"** section
3. Download APK files

## Common Scenarios

### Scenario 1: Feature Ready for Dev
✅ Create PR to dev → CI runs → Merge

### Scenario 2: Dev Ready for Production
✅ Actions → Merge Dev to Master → Run workflow → Done

### Scenario 3: Hotfix on Master
```bash
# Create hotfix from master
git checkout master
git checkout -b hotfix/fix-bug
# Fix bug, commit, push
# Create PR to master
```

### Scenario 4: Need to Update Feature Branch
```bash
git checkout feature/my-feature
git pull origin dev  # Get latest from dev
# Resolve conflicts if any
git push origin feature/my-feature
```

## Best Practices

✅ **Always create feature branches from dev**
```bash
git checkout dev
git pull origin dev
git checkout -b feature/new-feature
```

✅ **Keep PRs small** - Easier to review
✅ **Wait for CI** - Don't merge if tests fail
✅ **Test before merging dev → master**
✅ **Use semantic versioning** - v1.0.0, v1.1.0, etc.
✅ **Write clear commit messages**

## Troubleshooting

### Workflow Fails?
1. Click failed workflow
2. Expand failed job
3. Read error messages
4. Fix code and push again

### Can't Merge Dev to Master?
- Check if validation passed
- Make sure dev builds successfully
- Check branch protection rules

### PR Shows Failed Checks?
1. Click "Details" on failed check
2. Review logs
3. Fix issues locally
4. Push fixes to feature branch

## Quick Reference

```bash
# Daily workflow
git checkout dev
git pull origin dev
git checkout -b feature/name
# ... make changes ...
git push origin feature/name
# Create PR to dev on GitHub

# Release workflow
# GitHub → Actions → Merge Dev to Master → Run workflow
```

## Summary

**Development Flow:**
```
feature → [PR] → dev → [Button] → master
```

**CI Runs:**
- ✅ On every push to dev/master
- ✅ On every PR to dev
- ✅ Before merging dev to master

**Release:**
- 🚀 One-click merge dev → master
- 🏷️ Optional tag creation
- 📦 Automatic APK generation

