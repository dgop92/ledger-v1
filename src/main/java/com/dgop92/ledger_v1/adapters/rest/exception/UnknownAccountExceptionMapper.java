package com.dgop92.ledger_v1.adapters.rest.exception;

import com.dgop92.ledger_v1.adapters.rest.dto.ProblemDetails;
import com.dgop92.ledger_v1.domain.exception.UnknownAccountException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps {@link UnknownAccountException} to a 400 RFC 7807 Problem Details response. */
@Provider
public final class UnknownAccountExceptionMapper
    implements ExceptionMapper<UnknownAccountException> {

  @Override
  public Response toResponse(UnknownAccountException exception) {
    ProblemDetails problem =
        new ProblemDetails(
            "about:blank",
            "Unknown Account",
            Response.Status.BAD_REQUEST.getStatusCode(),
            exception.getMessage(),
            exception.errorCode());
    return Response.status(Response.Status.BAD_REQUEST)
        .type("application/problem+json")
        .entity(problem)
        .build();
  }
}
