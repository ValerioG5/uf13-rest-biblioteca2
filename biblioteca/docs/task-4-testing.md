# Task 4 - Robustezza del Codice e Automazione dei Test

**Branch:** `feature/task-4`  
**Tecnologie:** JUnit 5, Mockito, Spring MockMvc, @WebMvcTest, @ExtendWith(MockitoExtension)

---

## Contesto

Con l'introduzione della struttura di risposta standardizzata (Task 1) e del gestore centralizzato degli errori, non esisteva alcuna suite di test automatizzati che garantisse la correttezza del comportamento. Qualsiasi refactoring o modifica futura poteva introdurre regressioni silenziose, in particolare sulle regole di business critiche come l'azzeramento dell'ID in fase di salvataggio.

---

## Obiettivo

Costruire una suite di test automatizzati che copra il Web Layer (Controller) tramite test di integrazione e il Service Layer tramite unit test puri, garantendo che le specifiche di business siano sempre rispettate e verificabili in modo deterministico.

---

## Scelte Progettuali

### 1. Dipendenza di test

Il `pom.xml` viene corretto aggiungendo `spring-boot-starter-test` come dipendenza di test standard, che include:
- JUnit 5 (`junit-jupiter`)
- Mockito (`mockito-core`, `mockito-junit-jupiter`)
- AssertJ
- MockMvc (tramite `spring-test`)
- JsonPath

In Spring Boot 4.x, l'annotazione `@WebMvcTest` si trova nel package `org.springframework.boot.webmvc.test.autoconfigure` (non nel classico `org.springframework.boot.test.autoconfigure.web.servlet` di Spring Boot 3.x).

### 2. AutoreControllerTest - Test di integrazione Web Layer

**Strategia:** `@WebMvcTest(AutoreController.class)`

`@WebMvcTest` carica solo il contesto Web (controller, `@ControllerAdvice`, `@ExceptionHandler`) senza avviare il server HTTP reale e senza caricare il contesto JPA o il database. E' la soluzione ottimale per testare il Web Layer in isolamento.

I servizi (`AutoreService`, `LibroService`) vengono sostituiti con mock tramite `@MockitoBean`, che in Spring 7 / Spring Boot 4.x e' la sostituzione di `@MockBean`.

Il `GlobalExceptionHandler` (annotato `@RestControllerAdvice`) viene incluso automaticamente nel contesto `@WebMvcTest`, permettendo di verificare il comportamento delle risposte di errore senza configurazione aggiuntiva.

**Tabella dei test del controller:**

| Metodo di test | Endpoint | HTTP atteso | Asserzioni jsonPath |
|---|---|---|---|
| `getAll_shouldReturn200WithAutoriList` | `GET /autori` | 200 | `$.success=true`, `$.data` array di 2 elementi |
| `getAutore_existingId_shouldReturn200WithAutore` | `GET /autori/1` | 200 | `$.data.id=1`, `$.data.nome`, `$.data.cognome` |
| `getAutore_notFound_shouldReturn404WithErrorEnvelope` | `GET /autori/999` | 404 | `$.success=false`, `$.code=RESOURCE_NOT_FOUND`, `$.data` assente |
| `addAutore_validPayload_shouldReturn200WithSavedAutore` | `POST /autori/add` | 200 | `$.data.id=10`, `$.data.nome`, `$.code=SUCCESS` |
| `addAutore_blankNome_shouldReturn400WithValidationError` | `POST /autori/add` | 400 | `$.code=VALIDATION_ERROR`, `$.data.nome` contiene messaggio |
| `deleteAutore_existingId_shouldReturn200WithSuccessMessage` | `DELETE /autori/1` | 200 | `$.success=true`, `$.data` assente |
| `deleteAutore_notFound_shouldReturn404` | `DELETE /autori/99` | 404 | `$.success=false`, `$.code=RESOURCE_NOT_FOUND` |

**Verifica struttura envelope:** ogni test verifica sistematicamente sia i nodi di struttura (`$.success`, `$.code`, `$.message`) sia i nodi di payload (`$.data.*`), garantendo che l'envelope sia sempre presente e corretto indipendentemente dallo scenario.

### 3. AutoreServiceTest - Unit test puri

**Strategia:** `@ExtendWith(MockitoExtension.class)`

Il Service viene istanziato da `@InjectMocks` con tutte le dipendenze sostituite da `@Mock`. Non viene avviato nessun contesto Spring: il test e' puro codice Java.

**Tabella dei test del service:**

| Metodo di test | Metodo testato | Asserzione chiave |
|---|---|---|
| `save_shouldSetIdToNullBeforePersisting` | `save()` | `verify(repo).save(argThat(e -> e.getId() == null))` |
| `save_withNullId_shouldPersistWithNullId` | `save()` | ID null confermato anche senza forzatura client |
| `findAll_shouldDelegateToRepoAndMapResults` | `findAll()` | `verify(autoreRepo).findAll()`, lista 2 DTO corretti |
| `getById_existingId_shouldReturnDto` | `getById()` | DTO non null, campi corretti |
| `getById_notFound_shouldThrowResourceNotFoundException` | `getById()` | `assertThrows(ResourceNotFoundException.class)` |
| `deleteById_existingId_shouldCallRepositoryDelete` | `deleteById()` | `verify(repo).deleteById(id)`, no interazioni extra |
| `deleteById_notFound_shouldThrowWithoutDeletingAnything` | `deleteById()` | eccezione lanciata, `verify(repo, never()).deleteById(999)` |

### 4. Regola di business critica: azzeramento ID

Il test piu' rilevante dal punto di vista del business e':

```java
verify(autoreRepo).save(argThat(entity -> {
    assertThat(entity.getId()).isNull();
    return true;
}));
```

`argThat` con un `ArgumentMatcher` personalizzato verifica che l'oggetto passato al metodo `save` del repository abbia l'ID pari a `null`, indipendentemente dal valore presente nel DTO di input. Questo prova che la regola di business (`entity.setId(null)`) e' stata effettivamente applicata prima della persistenza.

### 5. Struttura dei test

```
src/test/java/it/marconi/biblioteca/
  controllers/
    AutoreControllerTest.java     (7 test, @WebMvcTest)
  services/
    AutoreServiceTest.java        (7 test, @ExtendWith(MockitoExtension))
```

---

## File Coinvolti

| File | Ruolo |
|---|---|
| `pom.xml` | Aggiunta dipendenza `spring-boot-starter-test` |
| `test/.../controllers/AutoreControllerTest.java` | 7 test integrazione Web Layer |
| `test/.../services/AutoreServiceTest.java` | 7 unit test puri Service Layer |

---

## Esecuzione dei Test

**Tutti i test della suite:**
```bash
./mvnw test -Dtest="AutoreServiceTest,AutoreControllerTest"
```

**Solo controller:**
```bash
./mvnw test -Dtest="AutoreControllerTest"
```

**Solo service:**
```bash
./mvnw test -Dtest="AutoreServiceTest"
```

**Output atteso:**
```
Tests run: 7, Failures: 0, Errors: 0  -- AutoreControllerTest
Tests run: 7, Failures: 0, Errors: 0  -- AutoreServiceTest

Tests run: 14, Failures: 0, Errors: 0
BUILD SUCCESS
```

---

## Criteri di Accettazione Soddisfatti

- Tutte le 14 asserzioni passano senza errori (`BUILD SUCCESS`)
- I test del controller verificano la struttura dell'envelope JSON su ogni scenario (successo e errore)
- Il test di integrazione verifica che il `GlobalExceptionHandler` intercetti correttamente le eccezioni e produca risposte 404/400 strutturate
- Il test unitario prova con `argThat` che l'ID venga azzerato prima del salvataggio nel database
- I test del service verificano con `verify(..., never())` che `deleteById` non venga mai invocato su una risorsa inesistente
- Nessun test richiede un database reale o un server HTTP avviato

---

## Nota Tecnica: Spring Boot 4.x

In Spring Boot 4.x i package per le annotazioni di test sono stati riorganizzati rispetto alla versione 3.x:

| Annotazione | Spring Boot 3.x | Spring Boot 4.x |
|---|---|---|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `@MockBean` | `org.springframework.boot.test.mock.mockito` | sostituito da `@MockitoBean` in `org.springframework.test.context.bean.override.mockito` |

---

## Commit di Riferimento

| Hash | Descrizione |
|---|---|
| `f25c3c6` | Aggiunta dipendenza spring-boot-starter-test e AutoreControllerTest con @WebMvcTest |
| `63dd742` | Aggiunto AutoreServiceTest con @ExtendWith(MockitoExtension) e verifica azzeramento ID |
