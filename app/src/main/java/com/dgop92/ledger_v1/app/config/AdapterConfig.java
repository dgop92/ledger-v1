package com.dgop92.ledger_v1.app.config;

import com.dgop92.ledger_v1.adapters.idgen.UuidV7IdGenerator;
import com.dgop92.ledger_v1.adapters.persistence.JdbiAccountRepository;
import com.dgop92.ledger_v1.application.usecase.CreateAccountUseCase;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

/** CDI wiring binding domain ports to their adapter implementations. */
public class AdapterConfig {

  @Produces
  @ApplicationScoped
  public Jdbi jdbi(DataSource dataSource) {
    return Jdbi.create(dataSource).installPlugin(new PostgresPlugin());
  }

  @Produces
  @ApplicationScoped
  public IdGenerator idGenerator() {
    return new UuidV7IdGenerator();
  }

  @Produces
  @ApplicationScoped
  public AccountRepository accountRepository(Jdbi jdbi) {
    return new JdbiAccountRepository(jdbi);
  }

  @Produces
  @ApplicationScoped
  public CreateAccountUseCase createAccountUseCase(
      AccountRepository accountRepository, IdGenerator idGenerator) {
    return new CreateAccountUseCase(accountRepository, idGenerator);
  }
}
