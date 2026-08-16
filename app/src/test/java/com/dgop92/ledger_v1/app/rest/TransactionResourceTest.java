package com.dgop92.ledger_v1.app.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TransactionResourceTest {

  private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

  private String createAccount(String accountType) {
    return given()
        .contentType("application/json")
        .body("{\"name\":\"TX Resource Test Account\",\"accountType\":\"" + accountType + "\"}")
        .when()
        .post("/accounts")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  private String transactionBody(
      String debitAccountId, String creditAccountId, long amountMinorUnits) {
    return "{\"entries\":["
        + "{\"accountId\":\""
        + debitAccountId
        + "\",\"direction\":\"DEBIT\",\"amountMinorUnits\":"
        + amountMinorUnits
        + "},"
        + "{\"accountId\":\""
        + creditAccountId
        + "\",\"direction\":\"CREDIT\",\"amountMinorUnits\":"
        + amountMinorUnits
        + "}]}";
  }

  @Test
  void postsBalancedTransaction() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    given()
        .contentType("application/json")
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .body(transactionBody(debitAccountId, creditAccountId, 1000))
        .when()
        .post("/transactions")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("journalEntries.size()", equalTo(2))
        .body("journalEntries[0].id", notNullValue());
  }

  @Test
  void rejectsMissingIdempotencyKey() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    given()
        .contentType("application/json")
        .body(transactionBody(debitAccountId, creditAccountId, 500))
        .when()
        .post("/transactions")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("MISSING_IDEMPOTENCY_KEY"));
  }

  @Test
  void rejectsUnbalancedTransaction() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");
    String body =
        "{\"entries\":["
            + "{\"accountId\":\""
            + debitAccountId
            + "\",\"direction\":\"DEBIT\",\"amountMinorUnits\":1000},"
            + "{\"accountId\":\""
            + creditAccountId
            + "\",\"direction\":\"CREDIT\",\"amountMinorUnits\":900}]}";

    given()
        .contentType("application/json")
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .body(body)
        .when()
        .post("/transactions")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("UNBALANCED_TRANSACTION"));
  }

  @Test
  void rejectsTooFewEntries() {
    String debitAccountId = createAccount("ASSET");
    String body =
        "{\"entries\":[{\"accountId\":\""
            + debitAccountId
            + "\",\"direction\":\"DEBIT\",\"amountMinorUnits\":1000}]}";

    given()
        .contentType("application/json")
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .body(body)
        .when()
        .post("/transactions")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("INVALID_TRANSACTION"));
  }

  @Test
  void rejectsUnknownAccount() {
    String debitAccountId = createAccount("ASSET");
    String unknownAccountId = UUID.randomUUID().toString();

    given()
        .contentType("application/json")
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .body(transactionBody(debitAccountId, unknownAccountId, 500))
        .when()
        .post("/transactions")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("UNKNOWN_ACCOUNT"));
  }

  @Test
  void rejectsNonPositiveAmount() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");
    String body =
        "{\"entries\":["
            + "{\"accountId\":\""
            + debitAccountId
            + "\",\"direction\":\"DEBIT\",\"amountMinorUnits\":0},"
            + "{\"accountId\":\""
            + creditAccountId
            + "\",\"direction\":\"CREDIT\",\"amountMinorUnits\":0}]}";

    given()
        .contentType("application/json")
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .body(body)
        .when()
        .post("/transactions")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("INVALID_TRANSACTION"));
  }

  @Test
  void rejectsNonIntegerAmount() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");
    String body =
        "{\"entries\":["
            + "{\"accountId\":\""
            + debitAccountId
            + "\",\"direction\":\"DEBIT\",\"amountMinorUnits\":100.5},"
            + "{\"accountId\":\""
            + creditAccountId
            + "\",\"direction\":\"CREDIT\",\"amountMinorUnits\":100.5}]}";

    given()
        .contentType("application/json")
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .body(body)
        .when()
        .post("/transactions")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("INVALID_TRANSACTION"));
  }

  @Test
  void replayingSameIdempotencyKeyAndPayloadReturnsIdenticalJournalEntryIds() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");
    String idempotencyKey = UUID.randomUUID().toString();
    String body = transactionBody(debitAccountId, creditAccountId, 1234);

    List<String> firstEntryIds =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, idempotencyKey)
            .body(body)
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getList("journalEntries.id", String.class);

    List<String> secondEntryIds =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, idempotencyKey)
            .body(body)
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getList("journalEntries.id", String.class);

    Set<String> firstIds = new HashSet<>(firstEntryIds);
    Set<String> secondIds = new HashSet<>(secondEntryIds);
    assertEquals(firstIds, secondIds);
  }

  @Test
  void differentPayloadWithSameIdempotencyKeyReturnsConflict() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");
    String idempotencyKey = UUID.randomUUID().toString();

    given()
        .contentType("application/json")
        .header(IDEMPOTENCY_HEADER, idempotencyKey)
        .body(transactionBody(debitAccountId, creditAccountId, 1000))
        .when()
        .post("/transactions")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .header(IDEMPOTENCY_HEADER, idempotencyKey)
        .body(transactionBody(debitAccountId, creditAccountId, 2000))
        .when()
        .post("/transactions")
        .then()
        .statusCode(409)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("IDEMPOTENCY_CONFLICT"));
  }
}
