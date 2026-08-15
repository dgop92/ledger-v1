package com.dgop92.ledger_v1.adapters.rest;

import com.dgop92.ledger_v1.adapters.rest.dto.AccountResponse;
import com.dgop92.ledger_v1.adapters.rest.dto.CreateAccountRequest;
import com.dgop92.ledger_v1.application.usecase.CreateAccountCommand;
import com.dgop92.ledger_v1.application.usecase.CreateAccountUseCase;
import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.exception.InvalidAccountNameException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/accounts")
public class AccountResource {

  @Inject CreateAccountUseCase createAccountUseCase;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createAccount(CreateAccountRequest request) {
    if (request == null) {
      throw new InvalidAccountNameException("request body must not be empty");
    }
    Account account =
        createAccountUseCase.execute(
            new CreateAccountCommand(request.name(), request.accountType()));
    return Response.status(Response.Status.CREATED)
        .entity(AccountResponse.fromDomain(account))
        .build();
  }
}
