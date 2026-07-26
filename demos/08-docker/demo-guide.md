# Demo: Module 08 — Introduction to Docker

**Duration:** 12 minutes  
**Prerequisite:** Docker Desktop running; Java 21 and Maven installed locally

---

## Part 1: Docker solves the "install MySQL" problem (3 min)

Open a terminal. Before Docker, running this app meant installing and configuring MySQL locally.
Show the alternative:

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

> "We just started a fully configured MySQL 8 server in one command. No installer, no wizard,
> no path variables. The image came from Docker Hub and MySQL is running in an isolated container."

Wait ~10 seconds then verify it is ready:

```bash
docker logs stocksdb
# Look for: ready for connections
```

> "The -e flags are environment variables - the MySQL image reads these to create the database
> and user automatically. The -p flag maps port 3306 on our machine into the container."

Now run the app locally, pointing at the containerised MySQL:

```bash
cd demos/08-docker
mvn spring-boot:run
```

Open `http://localhost:8080/api/stocks` — data is served from MySQL running in Docker.

**Key talking point:** We did not install MySQL. We ran it. That is the first win Docker gives us.

---

## Part 2: Now containerise the app itself (2 min)

> "Great - but the app still runs on our machine with Java installed. What if we want to ship
> the app the same way we shipped MySQL - as a container?"

Stop the app (`Ctrl+C`). Open `Dockerfile` and walk through it:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
```
> "We start FROM an existing image that has Maven and Java 21. AS build names this stage."

```dockerfile
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
```
> "We copy pom.xml *before* the source code. Docker caches each layer. If only the source
> changes, Docker reuses the cached dependency download - much faster rebuilds."

```dockerfile
COPY src ./src
RUN mvn package -DskipTests -q
```
> "Now we copy source and compile. We skip tests here - CI will run them in Module 09."

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```
> "The second FROM starts a fresh, much smaller image - just a JRE, no Maven, no source.
> We COPY only the JAR from the build stage. This is a multi-stage build. The final image
> is typically 6-8x smaller than using the Maven image throughout."

---

## Part 3: Build, run, and connect the two containers (5 min)

Build the app image:

```bash
docker build -t stock-app:demo .
```

Show layer output. Run a second build to demonstrate caching:

```bash
docker build -t stock-app:demo .
```
> "Notice CACHED next to the dependency step - that layer did not run again because pom.xml
> did not change."

Now run the app container, connecting it to the MySQL container.
We use `host.docker.internal` so the app container can reach the MySQL container via the host.
On Linux, this hostname does not resolve automatically — `--add-host` is required
(Docker 20.10+):

```bash
docker run -d \
  -p 8080:8080 \
  --name stock-app \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/stocksdb?useSSL=false&allowPublicKeyRetrieval=true" \
  -e SPRING_DATASOURCE_USERNAME=appuser \
  -e SPRING_DATASOURCE_PASSWORD=apppass \
  stock-app:demo
```

> **Demoing on Windows or Mac (Docker Desktop) instead of the Linux VM?** Docker Desktop
> resolves `host.docker.internal` natively, so the `--add-host` flag above is unnecessary
> there (it's harmless to leave in, but you can drop it). You can also use `localhost:8080`
> directly rather than the VM's private IP and the 8081 workaround the delegates use for
> their Linux VMs — Docker Desktop doesn't have the Jenkins port clash that the lab VMs do.

Check logs and test:

```bash
docker logs stock-app
curl http://localhost:8080/api/stocks
```

Open `http://localhost:8080` — the stock tracker frontend loads data from the API, which reads
from MySQL — all running in Docker.

Show useful CLI commands:

```bash
docker ps                         # running containers
docker logs stock-app             # app output
docker exec -it stock-app sh      # shell inside the container
docker stop stock-app             # stop
docker rm stock-app               # remove
docker stop stocksdb && docker rm stocksdb
```

**Key message:** Docker makes the environment part of the deliverable. MySQL, the JRE, and the
app itself all run from images. Any machine with Docker reproduces this environment exactly.

---

## Instructor notes

- If port 3306 is already in use on the host, change the -p mapping: `-p 3307:3306`
