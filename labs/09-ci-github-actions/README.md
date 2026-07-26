# Module 09 Lab — Continuous Integration with GitHub Actions

## Objective
Add a GitHub Actions CI workflow to the Stock Tracker Spring Boot application so that every
push and pull request automatically builds the project and runs the tests.

## Prerequisites
- Module 08 lab completed — you should already have your own `stock-tracker` GitHub
  repository, containing the stocks REST API and a working `Dockerfile`
- Git installed locally
- Docker not required for this lab — the tests use Mockito and do not need a database

## Overview
You will:
1. Continue working in the `stock-tracker` repository you created in Module 08
2. Create a `.github/workflows/ci.yml` workflow file
3. Push the workflow and watch GitHub Actions run it
4. Introduce a deliberate test failure and observe the CI failure
5. Fix the failure and watch CI go green again

---

## Steps

### Step 1 — Continue in your repository
This lab reuses the `stock-tracker` repository you created and pushed to in Module 08 — no
new repository is needed.

Docker isn't required for this lab, so if you'd rather work from your Windows VM than the
Linux VM, clone (or pull) your repository there:

```bash
git clone https://github.com/YOUR-USERNAME/stock-tracker.git
cd stock-tracker
```

If you're continuing on the same machine you used for Module 08, just `cd` into your existing
clone — there's nothing new to set up.

---

### Step 2 — Create the workflow file
In your project, create the file `.github/workflows/ci.yml`.

Complete each TODO:

```yaml
# TODO 1: Give the workflow a name, e.g. "CI"
name: ???

# TODO 2: Set the trigger - run on push AND pull_request to the main branch
on:
  push:
    branches: [ ??? ]
  pull_request:
    branches: [ ??? ]

jobs:
  build:
    # TODO 3: Choose a runner - use the latest Ubuntu hosted runner
    runs-on: ???

    steps:
      # TODO 4: Check out the repository code using the official action
      - name: Checkout source
        uses: actions/???@v4

      # TODO 5: Set up Java 21 (Temurin distribution) with Maven cache
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '???'
          distribution: '???'
          cache: ???

      # TODO 6: Build the project with Maven, skipping tests
      - name: Build with Maven
        run: mvn -B package -DskipTests

      # TODO 7: Run the tests
      - name: Run tests
        run: ???
```

---

### Step 3 — Push the workflow and watch it run
```bash
git add .github/workflows/ci.yml
git commit -m "Add CI workflow"
git push
```

Go to your repository on GitHub and click the **Actions** tab.
You should see the workflow run appear within a few seconds.

Click into the run, then into the `build` job, and expand each step to see the log output.
The run should complete with a green tick.

---

### Step 4 — Introduce a deliberate failure
Open `src/main/java/com/stocks/service/StockServiceImpl.java`.

Find the `addStock` method and change the duplicate-check condition so it always throws:

```java
// Temporarily break the duplicate check
throw new IllegalArgumentException("Always broken: " + stock.symbol());
```

Commit and push:

```bash
git add src/main/java/com/stocks/service/StockServiceImpl.java
git commit -m "Break addStock for CI demo"
git push
```

Watch the **Actions** tab — the run should turn red. Click into the failure and find which
test caught it and why.

---

### Step 5 — Fix the failure and go green
Revert your change to `StockServiceImpl`:

```bash
git revert HEAD
git push
```

Watch the Actions tab — CI should go green again.

---

### Step 6 — Add a build status badge (stretch)
GitHub generates a badge URL for your workflow. Find it at:

**Actions tab > your workflow > top-right "..." menu > Create status badge**

Copy the markdown and paste it into your `README.md`:

```markdown
![CI](https://github.com/YOUR-USERNAME/stock-tracker/actions/workflows/ci.yml/badge.svg)
```

Commit and push — the badge appears on your repo's home page.

---

## Acceptance Criteria
- The workflow file is at `.github/workflows/ci.yml`
- A push to `main` triggers the workflow automatically
- Both the build step and the test step appear as separate steps in the Actions UI
- A deliberate test failure causes the workflow run to show red
- Fixing the failure and pushing again returns CI to green

## Key Questions
1. What is the difference between `push` and `pull_request` as triggers?
2. Why do we separate `mvn package -DskipTests` and `mvn test` into two steps?
3. What does `cache: maven` do, and why does it matter for build speed?
4. What is a GitHub-hosted runner, and what is installed on it by default?
5. If your tests needed a real MySQL database, how would you provide one in the workflow?
