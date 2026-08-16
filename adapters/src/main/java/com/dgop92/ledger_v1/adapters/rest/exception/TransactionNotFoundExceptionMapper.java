package com.dgop92.ledger_v1.adapters.rest.exception;

import com.dgop92.ledger_v1.adapters.rest.dto.ProblemDetails;
import com.dgop92.ledger_v1.domain.exception.TransactionNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps {@link TransactionNotFoundException} to a 404 RFC 7807 Problem Details response. */
@Provider
public final class TransactionNotFoundExceptionMapper
    implements ExceptionMapper<TransactionNotFoundException> {

  @Override
  public Response toResponse(TransactionNotFoundException exception) {
    ProblemDetails problem =
        new ProblemDetails(
            "about:blank",
            "Transaction Not Found",
            Response.Status.NOT_FOUND.getStatusCode(),
            exception.getMessage(),
            exception.errorCode());
    return Response.status(Response.Status.NOT_FOUND)
        .type("application/problem+json")
        .entity(problem)
        .build();
  }
}
