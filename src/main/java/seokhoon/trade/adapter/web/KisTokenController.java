package seokhoon.trade.adapter.web;

import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.KisTokenUseCases.*;
import seokhoon.trade.domain.kis.KisEnvironment;

import java.util.List;

@RestController
@RequestMapping("/api/kis/token")
public class KisTokenController {
    private final ManageKisTokenUseCase useCase;

    public KisTokenController(ManageKisTokenUseCase useCase) {
        this.useCase=useCase;
    }

    @GetMapping("/status")
    List<KisTokenStatus> statuses() {
        return useCase.statuses();
    }

    @PostMapping("/refresh")
    KisTokenStatus refresh(@RequestParam KisEnvironment environment) {
        return useCase.refresh(environment);
    }
}
