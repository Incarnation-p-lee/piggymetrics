package com.piggymetrics.account.controller;

import com.piggymetrics.account.domain.Account;
import com.piggymetrics.account.domain.Saving;
import com.piggymetrics.account.domain.User;
import com.piggymetrics.account.service.AccountService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;

@RestController
public class AccountController {

    private final AccountService accountService;

    private final MeterRegistry meterRegistry;

    private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;

    private Account defaultAccount;

    public AccountController(AccountService accountService, MeterRegistry meterRegistry,
                             Resilience4JCircuitBreakerFactory circuitBreakerFactory) {
        this.accountService = accountService;
        this.meterRegistry = meterRegistry;
        this.circuitBreakerFactory = circuitBreakerFactory;

        bindMetrics();
    }

    private void bindMetrics() {
        CircuitBreakerRegistry breakerRegistry = CircuitBreakerRegistry.ofDefaults();

        breakerRegistry.circuitBreaker("account-service");

        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(breakerRegistry).bindTo(meterRegistry);
    }

    @PreAuthorize("#oauth2.hasScope('server') or #name.equals('demo')")
    @RequestMapping(path = "/{name}", method = RequestMethod.GET)
    public Account getAccountByName(@PathVariable String name) {
        return circuitBreakerFactory.create("getAccountByName")
                .run(() -> accountService.findByName(name), throwable -> getDefaultAccount());
    }

    @RequestMapping(path = "/current", method = RequestMethod.GET)
    public Account getCurrentAccount(Principal principal) {
        return circuitBreakerFactory.create("getCurrentAccount")
                .run(() -> accountService.findByName(principal.getName()), throwable -> getDefaultAccount());
    }

    @RequestMapping(path = "/current", method = RequestMethod.PUT)
    public void saveCurrentAccount(Principal principal, @Valid @RequestBody Account account) {
        accountService.saveChanges(principal.getName(), account);
    }

    @RequestMapping(path = "/", method = RequestMethod.POST)
    public Account createNewAccount(@Valid @RequestBody User user) {
        return circuitBreakerFactory.create("createNewAccount")
                .run(() -> accountService.create(user), throwable -> getDefaultAccount());
    }

    private Account getDefaultAccount() {
        if (defaultAccount == null) {
            defaultAccount = new Account();

            defaultAccount.setName("Anonymous");
            defaultAccount.setNote("Not Available");
            defaultAccount.setExpenses(new ArrayList<>());
            defaultAccount.setIncomes(new ArrayList<>());
            defaultAccount.setSaving(new Saving());
        }

        return defaultAccount;
    }
}
