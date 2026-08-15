package com.dgop92.ledger_v1.app.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
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
}
