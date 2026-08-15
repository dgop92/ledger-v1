package com.dgop92.ledger_v1.adapters.rest.exception;

import com.dgop92.ledger_v1.adapters.rest.dto.ProblemDetails;
import com.dgop92.ledger_v1.domain.exception.IdempotencyConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps {@link IdempotencyConflictException} to a 409 RFC 7807 Problem Details response. */
@Provider
public final class IdempotencyConflictExceptionMapper
    implements ExceptionMapper<IdempotencyConflictException> {

  @Override
  public Response toResponse(IdempotencyConflictException exception) {
    ProblemDetails problem =
        new ProblemDetails(
            "about:blank",
            "Idempotency Conflict",
            Response.Status.CONFLICT.getStatusCode(),
            exception.getMessage(),
            exception.errorCode());
    return Response.status(Response.Status.CONFLICT)
        .type("application/problem+json")
        .entity(problem)
        .build();
  }
}
