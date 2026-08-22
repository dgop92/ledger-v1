package com.dgop92.ledger_v1.adapters.rest.exception;

import com.dgop92.ledger_v1.adapters.rest.dto.ProblemDetails;
import com.dgop92.ledger_v1.domain.exception.UnbalancedTransactionException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps {@link UnbalancedTransactionException} to a 400 RFC 7807 Problem Details response. */
@Provider
public final class UnbalancedTransactionExceptionMapper
    implements ExceptionMapper<UnbalancedTransactionException> {

  @Override
  public Response toResponse(UnbalancedTransactionException exception) {
    ProblemDetails problem =
        new ProblemDetails(
            "about:blank",
            "Unbalanced Transaction",
            Response.Status.BAD_REQUEST.getStatusCode(),
            exception.getMessage(),
            exception.errorCode());
    return Response.status(Response.Status.BAD_REQUEST)
        .type("application/problem+json")
        .entity(problem)
        .build();
  }
}
