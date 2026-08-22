package com.dgop92.ledger_v1.adapters.rest.exception;

import com.dgop92.ledger_v1.adapters.rest.dto.ProblemDetails;
import com.dgop92.ledger_v1.domain.exception.AlreadyReversedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps {@link AlreadyReversedException} to a 409 RFC 7807 Problem Details response. */
@Provider
public final class AlreadyReversedExceptionMapper
    implements ExceptionMapper<AlreadyReversedException> {

  @Override
  public Response toResponse(AlreadyReversedException exception) {
    ProblemDetails problem =
        new ProblemDetails(
            "about:blank",
            "Already Reversed",
            Response.Status.CONFLICT.getStatusCode(),
            exception.getMessage(),
            exception.errorCode());
    return Response.status(Response.Status.CONFLICT)
        .type("application/problem+json")
        .entity(problem)
        .build();
  }
}
