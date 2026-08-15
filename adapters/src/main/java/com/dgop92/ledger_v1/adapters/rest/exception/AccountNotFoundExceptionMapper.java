package com.dgop92.ledger_v1.adapters.rest.exception;

import com.dgop92.ledger_v1.adapters.rest.dto.ProblemDetails;
import com.dgop92.ledger_v1.domain.exception.AccountNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps {@link AccountNotFoundException} to a 404 RFC 7807 Problem Details response. */
@Provider
public final class AccountNotFoundExceptionMapper
    implements ExceptionMapper<AccountNotFoundException> {

  @Override
  public Response toResponse(AccountNotFoundException exception) {
    ProblemDetails problem =
        new ProblemDetails(
            "about:blank",
            "Account Not Found",
            Response.Status.NOT_FOUND.getStatusCode(),
            exception.getMessage(),
            exception.errorCode());
    return Response.status(Response.Status.NOT_FOUND)
        .type("application/problem+json")
        .entity(problem)
        .build();
  }
}
