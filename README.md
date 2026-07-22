# Spring Biblioteca

Webservice REST in Spring Boot per la gestione di una biblioteca.  
Il progetto e' sviluppato nell'ambito del corso UF13 ed e' organizzato in branch feature progressivi.

---

## Stack Tecnologico

| Categoria | Tecnologia |
|---|---|
| Framework | Spring Boot 4.x |
| Linguaggio | Java 21 |
| Persistenza | Spring Data JPA + MySQL 9 |
| Validazione | Jakarta Bean Validation |
| Documentazione API | SpringDoc OpenAPI (Swagger UI) |
| Containerizzazione | Docker, Docker Compose |
| Monitoraggio | Spring Actuator, Micrometer, Prometheus, Grafana |
| Testing | JUnit 5, Mockito, MockMvc |
| Build | Maven |
| Utility | Lombok |

---

## Struttura del Progetto

```
biblioteca/
  src/
    main/java/it/marconi/biblioteca/
      config/          GlobalExceptionHandler
      controllers/     AutoreController, LibroController, HealthCheckController
      domain/          Autore, Libro, AutoreDTO, LibroDTO, APIResponse, Mapper
      exceptions/      APIException, ResourceNotFoundException, ...
      repositories/    AutoreRepository, LibroRepository
      services/        AutoreService, LibroService
    test/java/it/marconi/biblioteca/
      controllers/     AutoreControllerTest
      services/        AutoreServiceTest
  docs/                Documentazione tecnica per task
  Dockerfile           Build multi-stage
  docker-compose.yaml  Orchestrazione stack completo
  prometheus.yml       Configurazione scraping metriche
  grafana/             Provisioning datasource e dashboard
```

---

## Branch e Funzionalita'

| Branch | Contenuto |
|---|---|
| `main` | Codebase di riferimento e README |
| `feature/task-1` | Standardizzazione API e gestione centralizzata degli errori |
| `feature/task-2` | Containerizzazione multi-stage e isolamento profili Spring |
| `feature/task-3` | Monitoraggio con Prometheus e Grafana |
| `feature/task-4` | Suite di test automatizzati (Controller + Service) |

---

## Avvio Rapido

### Locale (senza Docker)

```bash
# Requisiti: JDK 21, MySQL avviato su porta 3306
./mvnw spring-boot:run
```

### Con Docker Compose (profilo dev)

```bash
docker build -t biblioteca:test .
docker compose --profile dev up
```

### Con Docker Compose (profilo prod)

```bash
docker compose --profile prod up -d
```

---

## Endpoint Principali

| Metodo | Endpoint | Descrizione |
|---|---|---|
| `GET` | `/autori` | Lista tutti gli autori |
| `GET` | `/autori/{id}` | Cerca autore per ID |
| `GET` | `/autori/{id}/libri` | Libri di un autore |
| `POST` | `/autori/add` | Aggiunge un autore |
| `DELETE` | `/autori/{id}` | Elimina un autore |
| `GET` | `/libri` | Lista tutti i libri |
| `GET` | `/libri/{isbn}` | Cerca libro per ISBN |
| `POST` | `/libri/add` | Aggiunge un libro |
| `DELETE` | `/libri/{isbn}` | Elimina un libro |
| `GET` | `/test/health` | Health check applicativo |

---

## Struttura della Risposta API

Tutte le risposte seguono il contratto standardizzato `APIResponse<T>`:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Descrizione operazione",
  "data": { ... },
  "timestamp": "2026-07-13T20:00:00"
}
```

In caso di errore il campo `data` e' omesso e `success` e' `false`.

---

## Documentazione Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## Monitoraggio (con Docker Compose avviato)

| Servizio | URL |
|---|---|
| Actuator Health | `http://localhost:9090/actuator/health` |
| Metriche Prometheus | `http://localhost:9090/actuator/prometheus` |
| Prometheus UI | `http://localhost:9093` |
| Grafana | `http://localhost:3000` (admin / admin) |

---

## Esecuzione dei Test

```bash
./mvnw test -Dtest="AutoreServiceTest,AutoreControllerTest"
```

Risultato atteso: **14 test, 0 fallimenti**.

---

## Documentazione Tecnica per Task

La documentazione dettagliata di ogni task e' disponibile nella cartella `docs/` del rispettivo branch feature:

| Task | Branch | Documento |
|---|---|---|
| Task 1 - Standardizzazione API | `feature/task-1` | [docs/task-1-standardizzazione-api.md](docs/task-1-standardizzazione-api.md) |
| Task 2 - Containerizzazione | `feature/task-2` | [docs/task-2-containerizzazione.md](docs/task-2-containerizzazione.md) |
| Task 3 - Monitoraggio | `feature/task-3` | [docs/task-3-monitoraggio.md](docs/task-3-monitoraggio.md) |
| Task 4 - Testing | `feature/task-4` | [docs/task-4-testing.md](docs/task-4-testing.md) |
