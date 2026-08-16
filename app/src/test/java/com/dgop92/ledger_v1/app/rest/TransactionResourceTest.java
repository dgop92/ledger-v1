package com.dgop92.ledger_v1.app.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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

  @Test
  void getsKnownTransaction() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String transactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 1000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .when()
        .get("/transactions/{id}", transactionId)
        .then()
        .statusCode(200)
        .body("id", equalTo(transactionId))
        .body("journalEntries.size()", equalTo(2));
  }

  @Test
  void returnsNotFoundForUnknownTransaction() {
    given()
        .when()
        .get("/transactions/{id}", "00000000-0000-0000-0000-000000000000")
        .then()
        .statusCode(404)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("TRANSACTION_NOT_FOUND"));
  }

  @Test
  void returnsNotFoundForMalformedTransactionId() {
    given()
        .when()
        .get("/transactions/{id}", "not-a-uuid")
        .then()
        .statusCode(404)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("TRANSACTION_NOT_FOUND"));
  }

  @Test
  void listsTransactionsForAccountWithMultipleTransactions() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String firstId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 100))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String secondId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 200))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .queryParam("accountId", debitAccountId)
        .when()
        .get("/transactions")
        .then()
        .statusCode(200)
        .body("id", hasItems(firstId, secondId));
  }

  @Test
  void listsEmptyForAccountWithNoTransactions() {
    String accountId = createAccount("ASSET");

    given()
        .queryParam("accountId", accountId)
        .when()
        .get("/transactions")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void listsEmptyForUnknownAccount() {
    given()
        .queryParam("accountId", UUID.randomUUID().toString())
        .when()
        .get("/transactions")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  void returnsBadRequestWhenAccountIdMissing() {
    given()
        .when()
        .get("/transactions")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("MISSING_ACCOUNT_ID"));
  }

  @Test
  void returnsBadRequestWhenAccountIdMalformed() {
    given()
        .queryParam("accountId", "not-a-uuid")
        .when()
        .get("/transactions")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("INVALID_ACCOUNT_ID"));
  }

  @Test
  void reversesTransactionWithInvertedEntries() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String transactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 1000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String reversalId =
        given()
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .when()
            .post("/transactions/{id}/reverse", transactionId)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("originalTransactionId", equalTo(transactionId))
            .body("journalEntries.size()", equalTo(2))
            .body(
                "journalEntries.find { it.accountId == '" + debitAccountId + "' }.direction",
                equalTo("CREDIT"))
            .body(
                "journalEntries.find { it.accountId == '" + creditAccountId + "' }.direction",
                equalTo("DEBIT"))
            .extract()
            .path("id");

    given()
        .when()
        .get("/transactions/{id}", reversalId)
        .then()
        .statusCode(200)
        .body("id", equalTo(reversalId))
        .body("originalTransactionId", equalTo(transactionId))
        .body("journalEntries.size()", equalTo(2))
        .body(
            "journalEntries.find { it.accountId == '" + debitAccountId + "' }.direction",
            equalTo("CREDIT"))
        .body(
            "journalEntries.find { it.accountId == '" + creditAccountId + "' }.direction",
            equalTo("DEBIT"));
  }

  @Test
  void originalTransactionUnchangedAfterReversal() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String transactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 1000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .when()
        .get("/transactions/{id}", transactionId)
        .then()
        .statusCode(200)
        .body("id", equalTo(transactionId))
        .body("journalEntries.size()", equalTo(2))
        .body("originalTransactionId", is(nullValue()));

    given()
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .when()
        .post("/transactions/{id}/reverse", transactionId)
        .then()
        .statusCode(201);

    given()
        .when()
        .get("/transactions/{id}", transactionId)
        .then()
        .statusCode(200)
        .body("id", equalTo(transactionId))
        .body("journalEntries.size()", equalTo(2))
        .body("originalTransactionId", is(nullValue()));
  }

  @Test
  void reverseReturnsNotFoundForUnknownTransaction() {
    given()
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .when()
        .post("/transactions/{id}/reverse", "00000000-0000-0000-0000-000000000000")
        .then()
        .statusCode(404)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("TRANSACTION_NOT_FOUND"));
  }

  @Test
  void reverseReturnsNotFoundForMalformedTransactionId() {
    given()
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .when()
        .post("/transactions/{id}/reverse", "not-a-uuid")
        .then()
        .statusCode(404)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("TRANSACTION_NOT_FOUND"));
  }

  @Test
  void reverseRejectsMissingIdempotencyKey() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String transactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 1000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .when()
        .post("/transactions/{id}/reverse", transactionId)
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("MISSING_IDEMPOTENCY_KEY"));
  }

  @Test
  void reversingAlreadyReversedTransactionReturnsConflict() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String transactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 1000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .when()
        .post("/transactions/{id}/reverse", transactionId)
        .then()
        .statusCode(201);

    given()
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .when()
        .post("/transactions/{id}/reverse", transactionId)
        .then()
        .statusCode(409)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("ALREADY_REVERSED"));
  }

  @Test
  void reversingAReversalReturnsConflict() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String transactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 1000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String reversalId =
        given()
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .when()
            .post("/transactions/{id}/reverse", transactionId)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
        .when()
        .post("/transactions/{id}/reverse", reversalId)
        .then()
        .statusCode(409)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("ALREADY_REVERSED"));
  }

  @Test
  void reverseIdempotentReplayReturnsSameResultWithoutDuplicate() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String transactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 1000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String idempotencyKey = UUID.randomUUID().toString();

    String firstReversalId =
        given()
            .header(IDEMPOTENCY_HEADER, idempotencyKey)
            .when()
            .post("/transactions/{id}/reverse", transactionId)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String secondReversalId =
        given()
            .header(IDEMPOTENCY_HEADER, idempotencyKey)
            .when()
            .post("/transactions/{id}/reverse", transactionId)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    assertEquals(firstReversalId, secondReversalId);
  }

  @Test
  void reverseIdempotencyConflictForDifferentOriginal() {
    String debitAccountId = createAccount("ASSET");
    String creditAccountId = createAccount("LIABILITY");

    String firstTransactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 1000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String secondTransactionId =
        given()
            .contentType("application/json")
            .header(IDEMPOTENCY_HEADER, UUID.randomUUID().toString())
            .body(transactionBody(debitAccountId, creditAccountId, 2000))
            .when()
            .post("/transactions")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String idempotencyKey = UUID.randomUUID().toString();

    given()
        .header(IDEMPOTENCY_HEADER, idempotencyKey)
        .when()
        .post("/transactions/{id}/reverse", firstTransactionId)
        .then()
        .statusCode(201);

    given()
        .header(IDEMPOTENCY_HEADER, idempotencyKey)
        .when()
        .post("/transactions/{id}/reverse", secondTransactionId)
        .then()
        .statusCode(409)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("IDEMPOTENCY_CONFLICT"));
  }
}
