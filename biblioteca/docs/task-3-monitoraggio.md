# Task 3 - Monitoraggio e Osservabilita' con Prometheus e Grafana

**Branch:** `feature/task-3`  
**Tecnologie:** Spring Boot Actuator, Micrometer, Prometheus, Grafana, Docker Compose

---

## Contesto

Lo stack containerizzato (Task 2) non disponeva di alcun sistema di monitoraggio. In produzione era impossibile sapere se l'applicazione era operativa, quante richieste stava gestendo, qual era il tasso di errore, o se il database era raggiungibile, senza accedere manualmente ai log del container.

---

## Obiettivo

Integrare uno stack di osservabilita' completo basato su Prometheus (raccolta metriche) e Grafana (visualizzazione), esposto tramite Spring Boot Actuator e Micrometer, con endpoint di test per la simulazione di scenari reali.

---

## Scelte Progettuali

### 1. Spring Boot Actuator ed esposizione metriche

Le dipendenze `spring-boot-starter-actuator` e `micrometer-registry-prometheus` sono aggiunte al `pom.xml`. Micrometer funge da facade che traduce le metriche interne di Spring in formato Prometheus (OpenMetrics).

La configurazione in `application.properties` espone i management endpoint necessari:

```properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
```

L'endpoint `/actuator/prometheus` viene usato da Prometheus come scrape target.

### 2. Prometheus - Configurazione dello scraping

Il file `prometheus.yml` configura Prometheus per raccogliere metriche da entrambi i profili applicativi ogni 15 secondi:

```
Job: biblioteca-app
  Target: app-dev:8080   /actuator/prometheus  (label: instance=dev)
  Target: app-prod:8080  /actuator/prometheus  (label: instance=prod)
  scrape_interval: 15s
  scrape_timeout: 10s
```

Il label `instance` viene assegnato tramite `relabel_configs`, permettendo a Grafana di filtrare le metriche per ambiente.

### 3. Grafana - Visualizzazione

Grafana e' configurato in `docker-compose.yaml` con:
- Provisioning automatico del datasource Prometheus all'avvio
- Porta 3000 esposta sull'host
- Volume per la persistenza delle dashboard create

### 4. Aggiornamento Docker Compose

Il `docker-compose.yaml` viene esteso con i nuovi servizi:

| Servizio | Porta | Descrizione |
|---|---|---|
| `mysql-db` | 3306 | Database (invariato) |
| `app-dev` | 9090:8080 | Applicazione dev |
| `app-prod` | 9091:8080 | Applicazione prod |
| `prometheus` | 9093:9090 | Server Prometheus |
| `grafana` | 3000:3000 | Dashboard Grafana |

### 5. HealthCheckController - Endpoint di test

Viene aggiunto un controller dedicato (`/test`) per permettere la simulazione controllata di scenari di errore senza toccare il codice di business:

| Endpoint | Comportamento |
|---|---|
| `GET /test/health` | Restituisce `200 OK` con payload `"OK"` |
| `GET /test/error` | Lancia una `RuntimeException` -> `500 Internal Server Error` |
| `GET /test/stress` | Avvia 20 thread che generano errori in successione rapida |

L'endpoint `/test/stress` e' progettato per superare le soglie di alert configurabili in Prometheus (es. >5 errori in 1 minuto), permettendo di verificare che il sistema di alerting funzioni correttamente prima del deploy in produzione.

### 6. Metriche chiave disponibili

Le metriche esposte dall'integrazione Actuator + Micrometer includono:

| Metrica | Descrizione |
|---|---|
| `http_server_requests_seconds_count` | Numero totale di richieste HTTP per endpoint e status |
| `http_server_requests_seconds_sum` | Tempo totale di risposta (usato per calcolare latenza media) |
| `jvm_memory_used_bytes` | Utilizzo memoria JVM per area (heap, non-heap) |
| `jvm_threads_live_threads` | Thread attivi nella JVM |
| `hikaricp_connections_active` | Connessioni database attive nel pool |
| `process_cpu_usage` | Utilizzo CPU del processo |

---

## File Coinvolti

| File | Ruolo |
|---|---|
| `pom.xml` | Dipendenze Actuator e Micrometer Prometheus |
| `src/main/resources/application.properties` | Configurazione esposizione endpoint Actuator |
| `prometheus.yml` | Configurazione scraping Prometheus |
| `docker-compose.yaml` | Servizi Prometheus e Grafana |
| `grafana/` | Configurazione provisioning datasource e dashboard |
| `controllers/HealthCheckController.java` | Endpoint di test per simulazione errori |

---

## Accesso ai Servizi

| Servizio | URL |
|---|---|
| Applicazione (dev) | `http://localhost:9090` |
| Applicazione (prod) | `http://localhost:9091` |
| Actuator Health | `http://localhost:9090/actuator/health` |
| Metriche Prometheus | `http://localhost:9090/actuator/prometheus` |
| Prometheus UI | `http://localhost:9093` |
| Grafana | `http://localhost:3000` (admin/admin) |

---

## Criteri di Accettazione Soddisfatti

- Le metriche dell'applicazione sono accessibili in formato Prometheus all'endpoint `/actuator/prometheus`
- Prometheus raccoglie metriche da entrambi gli ambienti (dev e prod) ogni 15 secondi
- Grafana e' raggiungibile e configurato con Prometheus come datasource
- Gli endpoint di test permettono la simulazione di errori singoli e multipli per validare alert
- I dettagli dello stato di salute (database, disco, JVM) sono visibili tramite `/actuator/health`

---

## Commit di Riferimento

| Hash | Descrizione |
|---|---|
| `3c070d8` | Aggiornato docker-compose per Prometheus e Grafana |
| `fd81795` | Aggiunto HealthCheckController per test di monitoraggio |
| `f2b924b` | Aggiunta configurazione monitoraggio e osservabilita' |
| `6cb5b01` | Configurazione esposizione endpoint di gestione |
