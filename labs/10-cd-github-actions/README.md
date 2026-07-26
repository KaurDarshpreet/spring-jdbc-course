# Module 10 Lab — Continuous Delivery and Deployment

## Objective
Extend the CI workflow from Module 09 to build a Docker image, push it to GitHub Container
Registry (GHCR), and deploy it to a Linux server using Docker Compose — triggered automatically
on every push to `main`.

## Prerequisites
- Module 09 lab completed (stocks app pushed to GitHub with a passing CI workflow)
- A Dockerfile in the repository root (from Module 08)
- `docker-compose.yml` provided in this folder — copy it to your repository root
- A Linux VM with Docker installed and your SSH public key in `~/.ssh/authorized_keys`
- Your Linux VM's IP address, SSH username, and private key to hand

## What is provided
- `docker-compose.yml` — defines the `mysql` and `stock-app` services. Copy this to the
  root of your stock-tracker repository.
- `.github/workflows/cd.yml` — a workflow template with TODO stubs for you to complete.

---

## Steps

### Step 1 — Prepare the Linux VM
SSH into your Linux VM and verify Docker and Docker Compose are available:

```bash
docker --version
docker compose version
```

Create the deployment directory:

```bash
mkdir -p ~/stock-app
```

Log out.

---

### Step 2 — Add the provided files to your repository
Copy both files into your stock-tracker repository:

- `docker-compose.yml` → root of the repository
- `.github/workflows/cd.yml` → `.github/workflows/cd.yml`

---

### Step 3 — Make the GHCR package public
After your first push, GitHub creates a private package by default. Make it public so the
Linux VM can pull the image without authentication:

1. Go to your GitHub profile > **Packages**
2. Click your `stock-app` package
3. Package settings > Change visibility > **Public**

*(Alternatively, add a `docker login ghcr.io` step in the deploy SSH script — see Key Questions.)*

---

### Step 4 — Create repository secrets
Go to your GitHub repository > **Settings** > **Secrets and variables** > **Actions** >
**New repository secret** and add these three secrets:

| Secret name | Value |
|---|---|
| `DEPLOY_HOST` | IP address of your Linux VM |
| `DEPLOY_USER` | Your SSH username on the VM (e.g. `ubuntu`) |
| `DEPLOY_SSH_KEY` | The full contents of your SSH private key file |

To copy your private key contents:
```bash
cat ~/.ssh/id_rsa
```
Copy everything including the `-----BEGIN` and `-----END` lines.

`GITHUB_TOKEN` is provided automatically — you do not need to create it.

---

### Step 5 — Complete the CD workflow
Open `.github/workflows/cd.yml` and complete the TODOs:

```yaml
name: CD

on:
  push:
    branches: [ main ]

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    # TODO 1: Add permissions to allow pushing packages to GHCR
    permissions:
      contents: read
      packages: ???

    outputs:
      image: ${{ steps.image-name.outputs.image }}

    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build with Maven
        run: mvn -B package -DskipTests

      # TODO 2: Add a step to run the tests
      - name: Run tests
        run: ???

      - name: Derive lowercase image name
        id: image-name
        run: |
          IMAGE="ghcr.io/$(echo '${{ github.repository }}' | tr '[:upper:]' '[:lower:]'):${{ github.sha }}"
          echo "image=$IMAGE" >> "$GITHUB_OUTPUT"

      # TODO 3: Log in to GHCR using docker/login-action@v3
      #         registry: ghcr.io
      #         username: ${{ github.actor }}
      #         password: ${{ secrets.GITHUB_TOKEN }}
      - name: Log in to GitHub Container Registry
        uses: ???
        with:
          registry: ???
          username: ???
          password: ???

      # TODO 4: Build and push the image using docker/build-push-action@v5
      #         context: .   push: true
      #         tags: ${{ steps.image-name.outputs.image }}
      - name: Build and push Docker image
        uses: ???
        with:
          context: .
          push: ???
          tags: |
            ???
            ghcr.io/${{ github.repository_owner }}/stock-app:latest

  deploy:
    # TODO 5: Make this job depend on build-and-push succeeding
    needs: ???
    runs-on: ubuntu-latest

    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      # TODO 6: Copy docker-compose.yml to ~/stock-app/ on the server
      #         Use appleboy/scp-action@v0.1.7
      #         host, username, key come from secrets
      - name: Copy docker-compose.yml to server
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.??? }}
          username: ${{ secrets.??? }}
          key: ${{ secrets.??? }}
          source: docker-compose.yml
          target: ~/stock-app/

      # TODO 7: SSH to the server and run:
      #           cd ~/stock-app
      #           echo "STOCK_APP_IMAGE=<image>" > .env
      #           docker compose pull
      #           docker compose up -d
      #           docker compose ps
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd ~/stock-app
            echo "STOCK_APP_IMAGE=${{ needs.build-and-push.outputs.image }}" > .env
            docker compose ???
            docker compose ???
            docker compose ps
```

---

### Step 6 — Push and watch the pipeline run
Commit everything and push:

```bash
git add docker-compose.yml .github/workflows/cd.yml
git commit -m "Add CD workflow and docker-compose"
git push
```

Go to the **Actions** tab on GitHub. You should see two jobs run in sequence:
1. `build-and-push` — builds the JAR, runs tests, pushes to GHCR
2. `deploy` — copies the compose file, SSHs to the server, brings up the app

---

### Step 7 — Verify the deployment
Once the pipeline is green, from your Windows VM open `http://YOURLINUXPRIVATEIP:8081` in a
browser (the same private IP and port pattern used in Module 08 — port 8081 because Jenkins
on the Linux VM already occupies 8080, and only 8081 is open in the security group).

You should see the Stock Tracker frontend loading data from the API.

On the Linux VM, verify the containers are running:

```bash
docker compose -f ~/stock-app/docker-compose.yml ps
docker compose -f ~/stock-app/docker-compose.yml logs stock-app
```

---

### Step 8 — Deploy a change end-to-end
Make a visible change — for example, add a new stock to the seed data in `StockApplication.java`:

```java
service.addStock(new Stock(null, "SHEL", "Shell PLC", "Energy", "LSE"));
```

Commit and push. Watch the full pipeline run. After it completes, refresh the browser —
the new stock should appear without any manual steps on the server.

---

## Acceptance Criteria
- A push to `main` triggers both jobs automatically
- The `deploy` job only runs if `build-and-push` succeeds
- The Docker image appears in your GitHub Packages
- `http://YOURLINUXPRIVATEIP:8081` serves the stock tracker frontend
- A code change pushed to `main` is live on the server after the pipeline completes
- MySQL data persists across deployments (the `mysql-data` volume is not removed)

## Key Questions
1. Why does the `deploy` job need `needs: build-and-push`? What would happen without it?
2. Why do we tag images with the git commit SHA rather than always using `:latest`?
3. What is the purpose of `depends_on: condition: service_healthy` in `docker-compose.yml`?
4. Why does a `docker compose up -d` not restart the MySQL container on each deployment?
5. How would you authenticate the Linux VM to pull from a private GHCR package?
6. What is the `mysql-data` volume, and what happens to the database if you run `docker compose down -v`?
