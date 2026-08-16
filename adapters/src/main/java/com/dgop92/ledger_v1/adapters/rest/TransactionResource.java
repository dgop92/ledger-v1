package com.dgop92.ledger_v1.adapters.rest;

import com.dgop92.ledger_v1.adapters.rest.dto.PostTransactionRequest;
import com.dgop92.ledger_v1.adapters.rest.dto.ProblemDetails;
import com.dgop92.ledger_v1.adapters.rest.dto.TransactionResponse;
import com.dgop92.ledger_v1.application.usecase.PostTransactionCommand;
import com.dgop92.ledger_v1.application.usecase.PostTransactionUseCase;
import com.dgop92.ledger_v1.domain.exception.InvalidTransactionException;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/transactions")
public class TransactionResource {

  private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
  private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;

  @Inject PostTransactionUseCase postTransactionUseCase;

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
}
