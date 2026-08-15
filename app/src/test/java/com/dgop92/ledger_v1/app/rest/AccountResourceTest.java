package com.dgop92.ledger_v1.app.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AccountResourceTest {

  @Test
  void createsAccountWithValidType() {
    given()
        .contentType("application/json")
        .body("{\"name\":\"Cash\",\"accountType\":\"ASSET\"}")
        .when()
        .post("/accounts")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("name", equalTo("Cash"))
        .body("accountType", equalTo("ASSET"));
  }

  @Test
  void rejectsInvalidAccountType() {
    given()
        .contentType("application/json")
        .body("{\"name\":\"Cash\",\"accountType\":\"BOGUS\"}")
        .when()
        .post("/accounts")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("INVALID_ACCOUNT_TYPE"));
  }

  @Test
  void rejectsBlankName() {
    given()
        .contentType("application/json")
        .body("{\"name\":\"   \",\"accountType\":\"ASSET\"}")
        .when()
        .post("/accounts")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("INVALID_ACCOUNT_NAME"));
  }

  @Test
  void rejectsEmptyRequestBody() {
    given()
        .contentType("application/json")
        .body("null")
        .when()
        .post("/accounts")
        .then()
        .statusCode(400)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("INVALID_ACCOUNT_NAME"));
  }

  @Test
  void allowsDuplicateNamesWithDistinctIds() {
    String firstId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"Duplicate\",\"accountType\":\"LIABILITY\"}")
            .when()
            .post("/accounts")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    String secondId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"Duplicate\",\"accountType\":\"LIABILITY\"}")
            .when()
            .post("/accounts")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    assertNotEquals(firstId, secondId);
  }

  @Test
  void listsCreatedAccounts() {
    String createdId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"Listed Account\",\"accountType\":\"REVENUE\"}")
            .when()
            .post("/accounts")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().get("/accounts").then().statusCode(200).body("id", hasItem(createdId));
  }

  @Test
  void getsKnownAccount() {
    String createdId =
        given()
            .contentType("application/json")
            .body("{\"name\":\"Gettable Account\",\"accountType\":\"EXPENSE\"}")
            .when()
            .post("/accounts")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .when()
        .get("/accounts/{id}", createdId)
        .then()
        .statusCode(200)
        .body("id", equalTo(createdId))
        .body("name", equalTo("Gettable Account"))
        .body("accountType", equalTo("EXPENSE"));
  }

  @Test
  void returnsNotFoundForUnknownAccount() {
    given()
        .when()
        .get("/accounts/{id}", "00000000-0000-0000-0000-000000000000")
        .then()
        .statusCode(404)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
  }

  @Test
  void returnsNotFoundForMalformedAccountId() {
    given()
        .when()
        .get("/accounts/{id}", "not-a-uuid")
        .then()
        .statusCode(404)
        .contentType(is("application/problem+json"))
        .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
  }
}
