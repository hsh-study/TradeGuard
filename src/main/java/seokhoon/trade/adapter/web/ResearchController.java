package seokhoon.trade.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.ResearchUseCases.*;
import seokhoon.trade.domain.market.Sector;
import seokhoon.trade.domain.market.SectorDailySnapshot;
import seokhoon.trade.domain.market.SectorType;
import seokhoon.trade.domain.market.StockSectorMapping;
import seokhoon.trade.domain.research.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/research")
public class ResearchController {
    private final ThesisUseCase thesisUseCase;
    private final CatalystUseCase catalystUseCase;
    private final MorningNoteUseCase morningNoteUseCase;
    private final SectorUseCase sectorUseCase;

    public ResearchController(
            ThesisUseCase thesisUseCase,
            CatalystUseCase catalystUseCase,
            MorningNoteUseCase morningNoteUseCase,
            SectorUseCase sectorUseCase
    ) {
        this.thesisUseCase = thesisUseCase;
        this.catalystUseCase = catalystUseCase;
        this.morningNoteUseCase = morningNoteUseCase;
        this.sectorUseCase = sectorUseCase;
    }

    @PostMapping("/theses")
    InvestmentThesis createThesis(@Valid @RequestBody ThesisRequest request) {
        return thesisUseCase.create(request.toCreateCommand());
    }

    @GetMapping("/theses")
    List<InvestmentThesis> findTheses(@RequestParam(required = false) String stockCode) {
        return thesisUseCase.find(stockCode);
    }

    @PatchMapping("/theses/{id}")
    InvestmentThesis updateThesis(
            @PathVariable long id,
            @Valid @RequestBody ThesisPatchRequest request
    ) {
        return thesisUseCase.update(id, request.toCommand());
    }

    @PostMapping("/theses/{id}/close")
    InvestmentThesis closeThesis(@PathVariable long id) {
        return thesisUseCase.close(id);
    }

    @PostMapping("/catalysts")
    InvestmentCatalyst createCatalyst(@Valid @RequestBody CatalystRequest request) {
        return catalystUseCase.create(request.toCreateCommand());
    }

    @GetMapping("/catalysts")
    List<InvestmentCatalyst> findCatalysts(
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return catalystUseCase.find(stockCode, from, to);
    }

    @PatchMapping("/catalysts/{id}")
    InvestmentCatalyst updateCatalyst(
            @PathVariable long id,
            @RequestBody CatalystPatchRequest request
    ) {
        return catalystUseCase.update(id, request.toCommand());
    }

    @PostMapping("/morning-note")
    MorningNote generateMorningNote(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return morningNoteUseCase.generate(tradeDate);
    }

    @GetMapping("/morning-note")
    MorningNote loadMorningNote(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return morningNoteUseCase.load(tradeDate);
    }

    @PostMapping("/sectors")
    Sector createSector(@Valid @RequestBody SectorRequest request) {
        return sectorUseCase.create(request.toCommand());
    }

    @PostMapping("/sectors/{sectorCode}/stocks")
    StockSectorMapping addSectorStock(
            @PathVariable String sectorCode,
            @Valid @RequestBody SectorStockRequest request
    ) {
        return sectorUseCase.addStock(sectorCode, request.toCommand());
    }

    @GetMapping("/sectors")
    List<Sector> findSectors() {
        return sectorUseCase.findAll();
    }

    @GetMapping("/sectors/{sectorCode}/snapshot")
    SectorDailySnapshot loadSectorSnapshot(
            @PathVariable String sectorCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return sectorUseCase.loadSnapshot(sectorCode, tradeDate);
    }

    @PostMapping("/sectors/snapshots")
    SectorSnapshotGenerationResult generateSectorSnapshots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate
    ) {
        return sectorUseCase.generateSnapshots(tradeDate);
    }

    public record ThesisRequest(
            @NotBlank String stockCode,
            @NotBlank String title,
            @NotBlank String coreAssumption,
            @NotBlank String invalidationCondition,
            @PositiveOrZero BigDecimal targetPrice,
            @NotBlank String stopLossCondition,
            @Min(0) @Max(100) int confidence,
            ThesisStatus status
    ) {
        CreateThesisCommand toCreateCommand() {
            return new CreateThesisCommand(stockCode, title, coreAssumption,
                    invalidationCondition, targetPrice, stopLossCondition, confidence, status);
        }
    }

    public record ThesisPatchRequest(
            String title,
            String coreAssumption,
            String invalidationCondition,
            @PositiveOrZero BigDecimal targetPrice,
            String stopLossCondition,
            @Min(0) @Max(100) Integer confidence,
            ThesisStatus status
    ) {
        UpdateThesisCommand toCommand() {
            return new UpdateThesisCommand(title, coreAssumption, invalidationCondition,
                    targetPrice, stopLossCondition, confidence, status);
        }
    }

    public record CatalystRequest(
            String stockCode,
            @NotBlank String title,
            @NotNull CatalystType catalystType,
            @NotNull LocalDate expectedDate,
            @NotNull CatalystImportance importance,
            CatalystStatus status,
            String sourceUrl,
            String memo
    ) {
        CreateCatalystCommand toCreateCommand() {
            return new CreateCatalystCommand(stockCode, title, catalystType, expectedDate,
                    importance, status, sourceUrl, memo);
        }
    }

    public record CatalystPatchRequest(
            String stockCode,
            String title,
            CatalystType catalystType,
            LocalDate expectedDate,
            CatalystImportance importance,
            CatalystStatus status,
            String sourceUrl,
            String memo
    ) {
        UpdateCatalystCommand toCommand() {
            return new UpdateCatalystCommand(stockCode, title, catalystType, expectedDate,
                    importance, status, sourceUrl, memo);
        }
    }

    public record SectorRequest(
            @NotBlank String sectorCode,
            @NotBlank String sectorName,
            SectorType sectorType
    ) {
        CreateSectorCommand toCommand() {
            return new CreateSectorCommand(sectorCode, sectorName, sectorType);
        }
    }

    public record SectorStockRequest(
            @NotBlank String stockCode,
            String source
    ) {
        AddSectorStockCommand toCommand() {
            return new AddSectorStockCommand(stockCode, source);
        }
    }
}
