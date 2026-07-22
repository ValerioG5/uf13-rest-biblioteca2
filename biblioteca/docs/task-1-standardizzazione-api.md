# Task 1 - Standardizzazione API e Gestione Centralizzata degli Errori

**Branch:** `feature/task-1`  
**Tecnologie:** Spring Boot 4.x, Java 21, Jakarta EE 10

---

## Contesto

Il progetto preesistente esponeva endpoint REST senza un contratto di risposta uniforme: ogni controller restituiva tipi diversi (`String`, oggetti naked, `Optional`) e gli errori non erano gestiti in modo consistente, rendendo il client impossibilitato a distinguere successi da fallimenti senza analizzare l'HTTP status code e il corpo della risposta caso per caso.

---

## Obiettivo

Introdurre un envelope di risposta standardizzato per tutte le API e un gestore centralizzato delle eccezioni, in modo che ogni risposta - sia di successo sia di errore - rispetti sempre lo stesso schema JSON.

---

## Scelte Progettuali

### 1. APIResponse - Envelope generico

Il contratto di risposta è modellato come un Java `record` generico parametrizzato su `<T>`:

```
APIResponse<T>
  success    : boolean
  code       : String
  message    : String
  data       : T  (null in caso di errore)
  timestamp  : LocalDateTime
```

La scelta del `record` è motivata da:
- Immutabilità garantita dal compilatore
- Assenza di boilerplate (no getter/setter)
- Compatibilità nativa con la serializzazione Jackson

L'annotazione `@JsonInclude(NON_NULL)` esclude automaticamente il campo `data` dalla serializzazione quando è null, evitando il rumore `"data": null` nelle risposte di errore.

I factory method statici (`ok`, `success`, `error`) centralizzano la creazione, impedendo che i controller costruiscano manualmente l'envelope e introducano incongruenze.

### 2. Gerarchia delle eccezioni custom

```
RuntimeException
  APIException  (abstract, porta code + message)
    ResourceNotFoundException   -> HTTP 404  (RESOURCE_NOT_FOUND)
    InvalidRequestException     -> HTTP 400  (INVALID_REQUEST)
    InternalServerException     -> HTTP 500  (INTERNAL_SERVER_ERROR)
```

La superclasse astratta `APIException` centralizza il campo `code`, che identifica il tipo di errore in modo machine-readable, disaccoppiando la logica del client dall'HTTP status.

### 3. GlobalExceptionHandler

Annotato con `@RestControllerAdvice`, intercetta le eccezioni a livello di Dispatcher Servlet prima che raggiungano il client. Gestisce i seguenti casi:

| Eccezione | HTTP Status | Code |
|---|---|---|
| `ResourceNotFoundException` | 404 | `RESOURCE_NOT_FOUND` |
| `InvalidRequestException` | 400 | `INVALID_REQUEST` |
| `InternalServerException` | 500 | `INTERNAL_SERVER_ERROR` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `MethodArgumentTypeMismatchException` | 400 | `INVALID_PARAMETER` |
| `NoHandlerFoundException` | 404 | `NOT_FOUND` |
| `Exception` (catch-all) | 500 | `INTERNAL_ERROR` |

Il catch-all finale garantisce che nessuno stack trace raggiunga mai il client in produzione.

### 4. AutoreService - Regola di business: azzeramento ID

Nel metodo `save()`, dopo la conversione DTO -> entity tramite mapper, l'ID dell'entity viene forzato a `null`:

```java
Autore entity = mapper.toEntity(autore);
entity.setId(null);  // impedisce la corruzione di record esistenti
entity = autoreRepo.save(entity);
```

Questa regola protegge il database da client malevoli o errati che includono un ID nella richiesta POST, che altrimenti potrebbero sovrascrivere record esistenti.

---

## File Coinvolti

| File | Ruolo |
|---|---|
| `domain/APIResponse.java` | Envelope generico di risposta |
| `exceptions/APIException.java` | Superclasse astratta eccezioni custom |
| `exceptions/ResourceNotFoundException.java` | 404 - risorsa non trovata |
| `exceptions/InvalidRequestException.java` | 400 - richiesta non valida |
| `exceptions/InternalServerException.java` | 500 - errore interno |
| `config/GlobalExceptionHandler.java` | Handler centralizzato con `@RestControllerAdvice` |
| `controllers/AutoreController.java` | Controller refactored con envelope |
| `services/AutoreService.java` | Service con regola azzeramento ID |

---

## Schema JSON di Risposta

**Successo (es. GET /autori/1):**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Autore trovato",
  "data": {
    "id": 1,
    "nome": "Dante",
    "cognome": "Alighieri"
  },
  "timestamp": "2026-07-13T20:00:00"
}
```

**Errore (es. GET /autori/999):**
```json
{
  "success": false,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Autore con ID 999 non trovato",
  "timestamp": "2026-07-13T20:00:00"
}
```

**Errore di validazione (es. POST /autori/add con nome vuoto):**
```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Validazione fallita",
  "data": {
    "nome": "Il nome e' obbligatorio"
  },
  "timestamp": "2026-07-13T20:00:00"
}
```

---

## Criteri di Accettazione Soddisfatti

- Ogni risposta API rispetta lo schema `{success, code, message, data, timestamp}`
- Le risposte di errore non espongono mai stack trace al client
- Il campo `data` e' assente (non serializzato) nelle risposte di errore senza payload
- Un client malevolo che invia un ID nel body della POST non puo' sovrascrivere record esistenti
- La validazione dei campi obbligatori restituisce errori per campo nel nodo `data`

---

## Commit di Riferimento

| Hash | Descrizione |
|---|---|
| `21302bf` | Implementato gestore globale delle eccezioni per l'API |
| `4a76b82` | Aggiunta eccezioni personalizzate per la gestione degli errori API |
| `a3a38c7` | Introduzione struttura standardizzata API |
| `bbd2043` | Refactor AutoreController e AutoreService e aggiunto error handling |
