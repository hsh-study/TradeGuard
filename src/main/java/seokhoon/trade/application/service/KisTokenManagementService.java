package seokhoon.trade.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seokhoon.trade.application.port.in.KisTokenUseCases.*;
import seokhoon.trade.application.port.out.KisAccessTokenProvider;
import seokhoon.trade.domain.kis.*;

import java.time.*;
import java.util.Arrays;
import java.util.List;

@Service
public class KisTokenManagementService implements ManageKisTokenUseCase {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final KisAccessTokenProvider provider;
    private final Clock clock;

    @Autowired
    public KisTokenManagementService(KisAccessTokenProvider provider) {
        this(provider,Clock.systemUTC());
    }

    KisTokenManagementService(KisAccessTokenProvider provider,Clock clock) {
        this.provider=provider;
        this.clock=clock;
    }

    @Override
    public List<KisTokenStatus> statuses() {
        return Arrays.stream(KisEnvironment.values())
                .map(this::status)
                .toList();
    }

    @Override
    public KisTokenStatus refresh(KisEnvironment environment) {
        provider.refresh(environment);
        return status(environment);
    }

    private KisTokenStatus status(KisEnvironment environment) {
        Instant now=clock.instant();
        return provider.findTokenMetadata(environment)
                .map(token->new KisTokenStatus(environment,true,
                        token.expiresAt(),
                        Duration.between(now,token.expiresAt()).getSeconds(),
                        LocalDate.ofInstant(token.issuedAt(),SEOUL)))
                .orElseGet(()->new KisTokenStatus(environment,false,
                        null,0,null));
    }
}
