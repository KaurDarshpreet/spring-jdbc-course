# Module 08 Lab — Introduction to Docker

## Objective
Run a MySQL database as a Docker container, then write a `Dockerfile` for the Stock Tracker
Spring Boot application, build and run it as a second container, and connect the two together.
Finally, add a simple HTML/JS frontend served from inside the Spring Boot container.

## Before you start: your own repository
The course repository is read-only for you, and Modules 09 and 10 need a repository you can
push to (to trigger GitHub Actions and deploy from). So this lab starts by creating your own
`stock-tracker` GitHub repository — you will keep using this same repository for Modules 09
and 10.

Also, if you are using a Windows VM, it does not have Docker installed and cannot run Docker
due to the underlying AWS architecture. All Docker commands in this lab must therefore run on
the Linux VM, so after creating your repository you will clone it onto the Linux machine.

## What is provided
The `labs/08-docker/` folder in this course repository contains a complete Spring Boot
application (the stocks REST API from Module 06). It connects to MySQL on `localhost:3306`.
On startup it seeds three stocks. You will push this into your own repository in Step 1.

---

## Steps

### Step 1 — Create your own repository and push the starter app
1. Log in to GitHub and create a new repository called `stock-tracker` (leave it empty — do
   not add a README or .gitignore via the UI)
2. From your local clone of the course repository, push the contents of `labs/08-docker/` as
   the initial commit of your new repository:

```bash
cd labs/08-docker
git init
git add .
git commit -m "Initial commit - stocks REST API"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/stock-tracker.git
git push -u origin main
```

Open the repository on GitHub and confirm the source code is there.

---

### Step 2 — Clone your repository onto the Linux VM
SSH into the Linux VM and clone the repository you just created:

```bash
git clone https://github.com/YOUR-USERNAME/stock-tracker.git
cd stock-tracker
```

You will do the rest of this lab inside this clone, on the Linux machine.

---

### Step 3 — Run MySQL in a container
No MySQL installation needed. Pull and run the official MySQL 8 image:

```bash
docker run -d \
  --name stocksdb \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=stocksdb \
  -e MYSQL_USER=appuser \
  -e MYSQL_PASSWORD=apppass \
  -p 3306:3306 \
  mysql:8
```

Wait about 10 seconds then check it is ready:

```bash
docker logs stocksdb
# Look for the line: ready for connections
```

---

### Step 4 — Write the Dockerfile for the Java Application
Create a file called `Dockerfile` (no extension) in the root of your `stock-tracker`
repository. Complete each TODO:

```dockerfile
# Stage 1: build the JAR
# TODO 1: Start FROM the maven:3.9-eclipse-temurin-21 image. Name this stage 'build'.
FROM ???

WORKDIR /app

# TODO 2: Copy pom.xml into the working directory
COPY ??? .

# TODO 3: Download all dependencies into the image layer cache
#         Hint: mvn dependency:go-offline -q
RUN ???

# TODO 4: Copy the src directory into the image
COPY ??? ./src

# TODO 5: Package the application, skipping tests
#         Hint: mvn package -DskipTests -q
RUN ???

# Stage 2: run the JAR in a minimal image
# TODO 6: Start FROM eclipse-temurin:21-jre-alpine
FROM ???

WORKDIR /app

# TODO 7: Copy the JAR from the build stage into this image as app.jar
#         Hint: use --from=build and /app/target/*.jar
COPY ??? app.jar

# TODO 8: Tell Docker which port the app listens on
EXPOSE ???

# TODO 9: Set the command to run the JAR
#         Hint: ENTRYPOINT with JSON array syntax
ENTRYPOINT ???
```

---

### Step 5 — Build the image
```bash
docker build -t stock-app:v1 .
```

Watch the output. Run the build a second time and observe the CACHED layers:

```bash
docker build -t stock-app:v1 .
```

---

### Step 6 — Run the app container
The app container needs to reach MySQL. We pass the datasource URL as an environment variable
using `host.docker.internal` so the app container can reach the MySQL container via the host.

On Linux, unlike Docker Desktop, `host.docker.internal` does not resolve automatically — add
`--add-host=host.docker.internal:host-gateway` to make it work (requires Docker 20.10+):

```bash
docker run -d \
  -p 8081:8080 \
  --name stock-app \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/stocksdb?useSSL=false&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=appuser \
  -e SPRING_DATASOURCE_PASSWORD=apppass \
  stock-app:v1
```

Note that we are using Port 8081. this is because your Linux machine is running Jenkins that is already using 8080.

Verify it is running and test the API:

```bash
docker ps
docker logs stock-app
curl http://localhost:8081/api/stocks
```

You can also test the API from your Windows VM.

---

### Step 7 — Optional - Add a frontend
In the root of your repository, create `src/main/resources/static/index.html` with an HTML
page that:

1. Has a heading "Stock Tracker"
2. Uses `fetch('/api/stocks')` to load stocks from the API
3. Displays them in a `<table>` with columns: Symbol, Company, Sector, Exchange

Rebuild and re-run:

```bash
docker stop stock-app && docker rm stock-app
docker build -t stock-app:v2 .
docker run -d -p 8081:8080 --name stock-app \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/stocksdb?useSSL=false&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=appuser \
  -e SPRING_DATASOURCE_PASSWORD=apppass \
  stock-app:v2
```

On your linux machine, find out the private IP address by using the command `hostname -I` (or `ip addr show`). It will be something like: 10.x.x.x

On your Windows machine, open `http://YOURLINUXPRIVATEIP:8081` in a browser — your table should be populated with stocks.

---

### Step 8 — Commit and push your work
Commit the `Dockerfile` (and `index.html` if you added the optional frontend) and push to
your repository — Modules 09 and 10 build directly on top of this:

```bash
git add Dockerfile src/main/resources/static/index.html
git commit -m "Add Dockerfile and frontend"
git push
```

---

### Step 9 — Optional: Explore useful Docker commands
```bash
docker ps                          # list running containers
docker ps -a                       # include stopped containers
docker logs stock-app              # view application output
docker exec -it stock-app sh       # shell inside the container
docker stop stock-app              # stop
docker rm stock-app                # remove
docker images                      # list local images
docker rmi stock-app:v1            # remove an image
docker stop stocksdb && docker rm stocksdb
```

---

## Acceptance Criteria
- Your own `stock-tracker` GitHub repository exists and contains the stocks application
- MySQL runs as a Docker container with no local MySQL installation
- `docker build` completes without errors
- The app container starts and `GET /api/stocks` returns three stocks
- The second build (unchanged `pom.xml`) shows CACHED for the dependency layer
- `http://YOURLINUXPRIVATEIP:8081` serves your HTML page with stocks loaded from the API
- The `Dockerfile` (and frontend, if added) is committed and pushed to your GitHub repository

## Key Questions
1. Why do we COPY `pom.xml` and run `mvn dependency:go-offline` before copying `src/`?
2. Why does the final image use `eclipse-temurin:21-jre-alpine` instead of the Maven image?
3. What is the difference between `EXPOSE` and `-p` in `docker run`?
4. Why do we pass `SPRING_DATASOURCE_URL` as an environment variable rather than baking it into `application.properties`?
5. What would happen to the data in MySQL if you ran `docker rm stocksdb`?
6. Why did this lab have you create your own GitHub repository rather than working directly in the course repository?
