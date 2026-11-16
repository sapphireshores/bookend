# Bookend-Library Asset & Loan Management Engine

A zero-dependency, full-stack library asset, circulation, and time-simulation engine built in **Java 17** using the standard JDK HTTP server and binary state persistence.

## Architecture
- **Core Domain & Rules Engine (`src/`):** Polymorphic item and member hierarchy (`Book`, `Magazine`, `ReferenceBook`, `Student`, `Staff`, `Faculty`), custom exception hierarchy, FIFO reservation queues, and automated late-fine calculation.
- **HTTP/1.1 REST Backend (`WebServer.java`):** Lightweight multi-route REST API built directly on `com.sun.net.httpserver.HttpServer` with thread-safe `synchronized` state transitions and atomic disk persistence (`library_state.dat`).
- **Web Dashboard (`index.html`):** Single-page Tailwind CSS + Fetch API client for real-time catalog management, loan execution, and day-by-day overdue simulation.

## API Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/state` | Returns simulated day, live inventory, and overdue loans |
| `POST` | `/api/items` | Registers a `BOOK`, `MAGAZINE`, or `REFERENCE` item |
| `POST` | `/api/members` | Registers a `STUDENT`, `STAFF`, or `FACULTY` member |
| `POST` | `/api/borrow` | Issues an item and computes due day |
| `POST` | `/api/return` | Processes return, calculates late fines, and auto-fulfills FIFO reservations |
| `POST` | `/api/advance` | Advances the simulation clock by $N$ days |

## Quick Start
```bash
javac -d bin src/*.java
java -cp bin WebServer
Open http://localhost:8080 in your browser.