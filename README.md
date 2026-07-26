# Spring and API Development Foundations for Java Graduates

**Course code:** CONSPRING | **Duration:** 3 days | **Java:** 21 | **Database:** MySQL 8

## Overview

Each module is a complete, runnable Maven project. Demo projects use the **Accounts and Transactions** domain; lab and solution projects use the **Stocks and Historical Prices** domain. All modules build on the previous one — each project contains the full working state of the application up to that point.

## Prerequisites

- Java 21 (`java -version` must show 21)
- Maven 3.9+ (`mvn -version`)
- MySQL 8 installed and running locally
- IntelliJ IDEA or VS Code with Java/Spring extensions

## Database Setup

Run this once in the MySQL client before starting Module 03. Modules 01 and 02 need no database.

```sql
CREATE DATABASE IF NOT EXISTS bankdb;
CREATE DATABASE IF NOT EXISTS stocksdb;
CREATE USER IF NOT EXISTS 'appuser'@'localhost' IDENTIFIED BY 'apppass';
GRANT ALL PRIVILEGES ON bankdb.*   TO 'appuser'@'localhost';
GRANT ALL PRIVILEGES ON stocksdb.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
```

The shared schemas are defined in:
- [`bank-schema.sql`](bank-schema.sql) — used by all demo projects (tables: `account`, `transaction`)
- [`stocks-schema.sql`](stocks-schema.sql) — used by all lab and solution projects (tables: `stock`, `historical_price`)

Each project from Module 03 onwards includes a copy of its schema as `src/main/resources/schema.sql`. Spring Boot applies it automatically on startup (`spring.sql.init.mode=always`). All DDL uses `CREATE TABLE IF NOT EXISTS` so it is safe to run repeatedly.

## Repository Structure

```
bank-schema.sql         Shared DDL for demo projects
stocks-schema.sql       Shared DDL for lab and solution projects

demos/                  Instructor demonstration applications (Accounts domain)
  <module>/             Complete runnable Maven project with explanatory comments

labs/                   Delegate exercise projects (Stocks domain)
  <module>/             Complete Maven project with starter code and TODO markers

solutions/              Reference implementations for instructors
  <module>/             Fully implemented version of the corresponding lab
```

## Modules

| # | Module | Lab |
|---|--------|-----|
| 01 | [Introduction to Spring](labs/01-introduction-to-spring/README.md) | IoC and DI with plain Spring |
| 02 | [Introduction to SpringBoot](labs/02-introduction-to-springboot/README.md) | Convert to SpringBoot |
| 03 | [Spring JDBC with SpringBoot](labs/03-spring-jdbc/README.md) | MySQL data access with JdbcTemplate |
| 04 | [REST and Microservices](labs/04-rest/README.md) | REST API with Spring MVC |
| 05 | [Controllers and Service Layers](labs/05-dtos-validation-logging/README.md) | DTOs, validation, error handling |
| 06 | [Best Practices](labs/06-testing/README.md) | Testing, clean code, configuration |
| 07 | [GenAI in Spring Development](labs/07-generative-ai/README.md) | AI-assisted development workflow |
| 08 | [Introduction to Docker](labs/08-docker/README.md) | Containerise the app and database with Docker |
| 09 | [Continuous Integration with GitHub Actions](labs/09-ci-github-actions/README.md) | Automated build and test on every push |
| 10 | [Continuous Delivery and Deployment](labs/10-cd-github-actions/README.md) | Build, push, and deploy a Docker image via GitHub Actions |

## Running a Project

```bash
# Module 01 (plain Spring, no Boot)
mvn compile exec:java -Dexec.mainClass="com.bank.Main"

# Modules 02 onwards
mvn spring-boot:run

# Build a runnable JAR (modules 02 onwards)
mvn package
java -jar target/<artifact>.jar
```
