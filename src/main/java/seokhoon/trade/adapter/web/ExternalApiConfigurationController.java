package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.ExternalApiConfigurationUseCase;
import seokhoon.trade.domain.kis.KisEnvironment;
import java.util.*;

@RestController
@RequestMapping("/api/external-api-configurations")
public class ExternalApiConfigurationController {
    private final ExternalApiConfigurationUseCase configurations;
    public ExternalApiConfigurationController(ExternalApiConfigurationUseCase configurations){this.configurations=configurations;}
    @GetMapping("/kis") List<ExternalApiConfigurationUseCase.KisConfigView> kis(){return configurations.kisConfigs();}
    @PutMapping("/kis/{environment}") ExternalApiConfigurationUseCase.KisConfigView saveKis(@PathVariable KisEnvironment environment,@Valid @RequestBody KisRequest r){return configurations.saveKis(new ExternalApiConfigurationUseCase.KisConfigCommand(environment,r.appKey(),r.appSecret(),r.baseUrl(),r.active()));}
    @GetMapping("/dart") Optional<ExternalApiConfigurationUseCase.DartConfigView> dart(){return configurations.dartConfig();}
    @PutMapping("/dart") ExternalApiConfigurationUseCase.DartConfigView saveDart(@Valid @RequestBody DartRequest r){return configurations.saveDart(new ExternalApiConfigurationUseCase.DartConfigCommand(r.apiKey(),r.baseUrl(),r.active()));}
    public record KisRequest(@NotBlank String appKey,@NotBlank String appSecret,@NotBlank String baseUrl,boolean active){}
    public record DartRequest(@NotBlank String apiKey,@NotBlank String baseUrl,boolean active){}
}
