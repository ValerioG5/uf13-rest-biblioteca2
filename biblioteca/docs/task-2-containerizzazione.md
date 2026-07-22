# Task 2 - Containerizzazione Multi-Stage e Isolamento dei Profili

**Branch:** `feature/task-2`  
**Tecnologie:** Docker, Docker Compose, Spring Profiles

---

## Contesto

L'applicazione era eseguibile solo in locale con MySQL installato sulla macchina host. Non esisteva un meccanismo riproducibile per avviare l'intero stack (app + database) in un ambiente isolato, né una separazione tra configurazioni di sviluppo e produzione.

---

## Obiettivo

Containerizzare l'applicazione con un Dockerfile multi-stage ottimizzato e orchestrare lo stack completo con Docker Compose, introducendo profili Spring separati (`dev`, `prod`) per gestire configurazioni ambiente-specifiche.

---

## Scelte Progettuali

### 1. Dockerfile Multi-Stage

Il Dockerfile e' strutturato in due stage distinti:

**Stage 1 - Build (`maven:3.9-eclipse-temurin-21-alpine`):**
- Utilizza un'immagine Maven con JDK 21 su Alpine per compilare il sorgente
- Esegue `mvn clean package -DskipTests` per produrre il JAR
- L'immagine Alpine minimizza lo spazio usato durante il build

**Stage 2 - Runtime (`eclipse-temurin:21-jre-alpine`):**
- Utilizza solo il JRE (non il JDK intero), riducendo drasticamente la superficie di attacco e le dimensioni finali dell'immagine
- Copia esclusivamente il JAR compilato dallo stage precedente (`COPY --from=build`)
- Nessun sorgente, nessun Maven, nessun tool di build nel container di produzione

Il vantaggio principale del multi-stage e' la separazione netta tra ambiente di build e ambiente di runtime: il container finale non contiene strumenti di sviluppo che potrebbero essere vettori di vulnerabilita'.

```
Immagine finale stimata: ~200MB vs ~500MB con immagine JDK singola
```

### 2. Docker Compose con profili

Il file `docker-compose.yaml` definisce tre servizi:

| Servizio | Profilo | Porta | Descrizione |
|---|---|---|---|
| `mysql-db` | (sempre attivo) | 3306 | Database MySQL 9.0 |
| `app-dev` | `dev` | 9090:8080 | Applicazione in modalita' sviluppo |
| `app-prod` | `prod` | 9091:8080 | Applicazione in modalita' produzione |

I profili Docker Compose permettono di avviare solo il subset di servizi necessario, evitando che dev e prod girino contemporaneamente sulla stessa macchina per errore.

### 3. Health Check sul database

Il servizio `mysql-db` ha un health check configurato:

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  timeout: 20s
  retries: 10
  interval: 10s
```

I servizi applicativi usano `depends_on: condition: service_healthy`, garantendo che l'applicazione Spring non venga avviata finche' MySQL non e' effettivamente pronto ad accettare connessioni. Questo risolve il problema del race condition tra app e DB all'avvio.

### 4. Variabili d'ambiente

Le credenziali del database non sono hardcoded nel Dockerfile ma passate come variabili d'ambiente in `docker-compose.yaml`, mappate alle proprieta' Spring tramite `application.properties`:

```
DB_HOST, DB_NAME, DB_USER, DB_PASSWORD -> spring.datasource.*
SPRING_PROFILES_ACTIVE -> spring.profiles.active
```

### 5. Volume per i log in produzione

Il profilo `prod` monta un volume locale `./logs:/var/log/app`, rendendo i log dell'applicazione accessibili sull'host senza dover entrare nel container. Il profilo `dev` non ha questo volume per semplificare il setup locale.

---

## File Coinvolti

| File | Ruolo |
|---|---|
| `Dockerfile` | Build multi-stage dell'immagine Docker |
| `docker-compose.yaml` | Orchestrazione stack con profili dev/prod |
| `src/main/resources/application.properties` | Configurazione base con placeholder env vars |
| `src/main/resources/application-dev.properties` | Override configurazioni per profilo dev |
| `src/main/resources/application-prod.properties` | Override configurazioni per profilo prod |

---

## Comandi di Utilizzo

**Build dell'immagine:**
```bash
docker build -t biblioteca:test .
```

**Avvio stack in modalita' dev:**
```bash
docker compose --profile dev up
```

**Avvio stack in modalita' prod:**
```bash
docker compose --profile prod up -d
```

**Verifica health del database:**
```bash
docker inspect mysql_container --format='{{.State.Health.Status}}'
```

---

## Criteri di Accettazione Soddisfatti

- L'immagine Docker finale contiene solo il JRE e il JAR, senza sorgenti o tool di build
- L'applicazione non si avvia finche' MySQL non supera il health check
- I profili `dev` e `prod` sono selezionabili a runtime senza modificare il codice
- Le credenziali del database non sono hardcoded nell'immagine
- I log di produzione sono accessibili sull'host tramite volume mount

---

## Commit di Riferimento

| Hash | Descrizione |
|---|---|
| `2903fb2` | Task 2 - Containerizzazione multi-stage e isolamento profili |
