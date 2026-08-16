package com.dgop92.ledger_v1.adapters.rest;

import com.dgop92.ledger_v1.adapters.rest.dto.PostTransactionRequest;
import com.dgop92.ledger_v1.adapters.rest.dto.ProblemDetails;
import com.dgop92.ledger_v1.adapters.rest.dto.TransactionResponse;
import com.dgop92.ledger_v1.application.usecase.GetTransactionUseCase;
import com.dgop92.ledger_v1.application.usecase.ListTransactionsUseCase;
import com.dgop92.ledger_v1.application.usecase.PostTransactionCommand;
import com.dgop92.ledger_v1.application.usecase.PostTransactionUseCase;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.InvalidTransactionException;
import com.dgop92.ledger_v1.domain.exception.TransactionNotFoundException;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/transactions")
public class TransactionResource {

  private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
  private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

  @Inject PostTransactionUseCase postTransactionUseCase;

  @Inject GetTransactionUseCase getTransactionUseCase;

  @Inject ListTransactionsUseCase listTransactionsUseCase;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postTransaction(
      @HeaderParam(IDEMPOTENCY_KEY_HEADER) String idempotencyKey, PostTransactionRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      ProblemDetails problem =
          new ProblemDetails(
              "about:blank",
              "Missing Idempotency Key",
              Response.Status.BAD_REQUEST.getStatusCode(),
              "The Idempotency-Key header is required",
              "MISSING_IDEMPOTENCY_KEY");
      return Response.status(Response.Status.BAD_REQUEST)
          .type("application/problem+json")
          .entity(problem)
          .build();
    }

    if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
      throw new InvalidTransactionException(
          "Idempotency-Key must not exceed " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
    }

    if (request == null || request.entries() == null) {
      throw new InvalidTransactionException("request body must not be empty");
    }

    List<PostTransactionCommand.EntryInput> entries =
        request.entries().stream()
            .map(
                entry -> {
                  if (entry == null) {
                    throw new InvalidTransactionException("journal entry must not be null");
                  }
                  return new PostTransactionCommand.EntryInput(
                      entry.accountId(), entry.direction(), entry.amountMinorUnits());
                })
            .toList();

    Transaction transaction =
        postTransactionUseCase.execute(new PostTransactionCommand(entries, idempotencyKey));
    return Response.status(Response.Status.CREATED)
        .entity(TransactionResponse.fromDomain(transaction))
        .build();
  }

  @GET
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTransaction(@PathParam("id") String id) {
    TransactionId transactionId;
    try {
      transactionId = new TransactionId(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      throw new TransactionNotFoundException(id);
    }
    Transaction transaction = getTransactionUseCase.execute(transactionId);
    return Response.ok(TransactionResponse.fromDomain(transaction)).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response listTransactions(@QueryParam("accountId") String accountId) {
    if (accountId == null || accountId.isBlank()) {
      ProblemDetails problem =
          new ProblemDetails(
              "about:blank",
              "Missing Account Id",
              Response.Status.BAD_REQUEST.getStatusCode(),
              "The accountId query parameter is required",
              "MISSING_ACCOUNT_ID");
      return Response.status(Response.Status.BAD_REQUEST)
          .type("application/problem+json")
          .entity(problem)
          .build();
    }

    AccountId parsedAccountId;
    try {
      parsedAccountId = new AccountId(UUID.fromString(accountId));
    } catch (IllegalArgumentException e) {
      ProblemDetails problem =
          new ProblemDetails(
              "about:blank",
              "Invalid Account Id",
              Response.Status.BAD_REQUEST.getStatusCode(),
              "The accountId query parameter must be a valid UUID",
              "INVALID_ACCOUNT_ID");
      return Response.status(Response.Status.BAD_REQUEST)
          .type("application/problem+json")
          .entity(problem)
          .build();
    }
    List<TransactionResponse> transactions =
        listTransactionsUseCase.execute(parsedAccountId).stream()
            .map(TransactionResponse::fromDomain)
            .toList();
    return Response.ok(transactions).build();
  }
}
