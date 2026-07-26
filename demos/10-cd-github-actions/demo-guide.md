# Demo: Module 10 — Continuous Delivery and Deployment

**Duration:** 15 minutes  
**Prerequisite:** Module 09 CI workflow already on the repo and passing. Docker installed on
the target Linux VM. The VM's public SSH key is added to authorized_keys.

---

## Part 1: The goal (2 min)

> "In Module 09 CI tells us if the code is correct. But the deployment still happens manually -
> we run Docker commands by hand. CD means: a green build automatically delivers a tested image
> to a server. Today we extend our CI workflow to also build a Docker image, push it to GHCR,
> and deploy it to a Linux VM, all without touching the server ourselves."

Draw the pipeline on a whiteboard:

```
push to main
  -> build JAR + run tests  (CI job from module 09)
  -> build Docker image
  -> push to GHCR
  -> SSH to server: docker compose pull && docker compose up -d
```

---

## Part 2: docker-compose.yml (3 min)

Open `demos/10-cd-github-actions/docker-compose.yml`. Walk through it:

```yaml
services:
  mysql:
    image: mysql:8
    volumes:
      - mysql-data:/var/lib/mysql
```

> "The MySQL service uses a named volume. Unlike a bind mount, a named volume persists even
> when the container is removed. This means a deployment does not wipe the database."

```yaml
  stock-app:
    image: ${STOCK_APP_IMAGE}
    depends_on:
      mysql:
        condition: service_healthy
```

> "The app image comes from an environment variable - we will write that value from the workflow.
> depends_on with service_healthy means the app container will not start until MySQL passes its
> health check. Without this, the app starts before MySQL is ready and crashes on first query."

> "Notice the datasource URL uses mysql as the hostname. That is the service name in this file.
> Docker Compose creates a private network and gives each service a DNS name matching its key.
> No host.docker.internal workaround needed."

---

## Part 3: Walk through the CD workflow (5 min)

Open `.github/workflows/cd.yml`.

**Job 1: build-and-push**

```yaml
    permissions:
      contents: read
      packages: write
```

> "By default GitHub Actions has read-only access to packages. packages: write lets the workflow
> push images to GHCR. GITHUB_TOKEN is automatically provided - no secret to create."

```yaml
      - name: Derive lowercase image name
        id: image-name
        run: |
          IMAGE="ghcr.io/$(echo '${{ github.repository }}' | tr '[:upper:]' '[:lower:]'):${{ github.sha }}"
          echo "image=$IMAGE" >> "$GITHUB_OUTPUT"
```

> "Docker image names must be lowercase. We tag with the git commit SHA rather than 'latest'
> so every deployment is traceable to an exact commit. We also push a :latest tag for
> docker compose pull to find."

```yaml
      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
```

> "The build-push-action handles the docker build and docker push in one step. context: . means
> it reads the Dockerfile from the root of the repository."

**Job 2: deploy**

```yaml
  deploy:
    needs: build-and-push
```

> "needs: means this job only starts if build-and-push succeeds. If the tests fail, deployment
> never happens - that is the CD gate."

```yaml
      - name: Copy docker-compose.yml to server
        uses: appleboy/scp-action@v0.1.7
```

> "SCP copies the docker-compose.yml from the repository to the server. This means the
> compose file is always in sync with the code - no manual file management on the server."

```yaml
      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          script: |
            cd ~/stock-app
            echo "STOCK_APP_IMAGE=..." > .env
            docker compose pull
            docker compose up -d
            docker compose ps
```

> "We write a .env file with the exact image tag we just built - tied to this commit SHA.
> docker compose pull downloads the new image. docker compose up -d recreates any containers
> whose image has changed. Containers with unchanged images are left running - zero downtime
> for MySQL."

---

## Part 4: Set up secrets and push (3 min)

Show the GitHub repository Settings > Secrets and variables > Actions page.

Add three secrets:
- `DEPLOY_HOST` — IP address of the Linux VM
- `DEPLOY_USER` — SSH username (e.g. ubuntu)
- `DEPLOY_SSH_KEY` — contents of the private key file (cat ~/.ssh/id_rsa)

> "Secrets are encrypted and never appear in workflow logs. The GITHUB_TOKEN for GHCR is
> provided automatically - we did not need to create it."

Push the workflow file. Show the Actions tab - both jobs run in sequence.

After the deploy job completes, open `http://DEPLOY_HOST:8081` — the app and frontend are live
(port 8081, not 8080, since Jenkins already occupies 8080 on the Linux VM — same reason the
Module 08 lab uses 8081).

**Key message:** From a git push to a live deployment with no manual steps. That is CD.

---

## Instructor notes

- The Linux VM needs Docker and Docker Compose installed: `apt install -y docker.io docker-compose-plugin`
- Add the deploy user to the docker group: `usermod -aG docker USERNAME`
- Make the GHCR package public (GitHub > Packages > package settings) so the server can pull
  without authenticating, or add a `docker login ghcr.io` step in the deploy SSH script
- On second and subsequent pushes, show that MySQL keeps its data (the volume persists)
