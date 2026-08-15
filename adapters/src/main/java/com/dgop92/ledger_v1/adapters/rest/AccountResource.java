package com.dgop92.ledger_v1.adapters.rest;

import com.dgop92.ledger_v1.adapters.rest.dto.AccountResponse;
import com.dgop92.ledger_v1.adapters.rest.dto.CreateAccountRequest;
import com.dgop92.ledger_v1.application.usecase.CreateAccountCommand;
import com.dgop92.ledger_v1.application.usecase.CreateAccountUseCase;
import com.dgop92.ledger_v1.application.usecase.GetAccountUseCase;
import com.dgop92.ledger_v1.application.usecase.ListAccountsUseCase;
import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.AccountNotFoundException;
import com.dgop92.ledger_v1.domain.exception.InvalidAccountNameException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/accounts")
public class AccountResource {

  @Inject CreateAccountUseCase createAccountUseCase;

  @Inject GetAccountUseCase getAccountUseCase;

  @Inject ListAccountsUseCase listAccountsUseCase;

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

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response listAccounts() {
    List<AccountResponse> accounts =
        listAccountsUseCase.execute().stream().map(AccountResponse::fromDomain).toList();
    return Response.ok(accounts).build();
  }

  @GET
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAccount(@PathParam("id") String id) {
    AccountId accountId;
    try {
      accountId = new AccountId(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      throw new AccountNotFoundException(id);
    }
    Account account = getAccountUseCase.execute(accountId);
    return Response.ok(AccountResponse.fromDomain(account)).build();
  }
}
