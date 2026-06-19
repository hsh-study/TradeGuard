package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import seokhoon.trade.application.port.in.ImportDisclosureActualEvidenceUseCase;
import seokhoon.trade.domain.research.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/research/disclosures")
public class DisclosureActualController {
    private final ImportDisclosureActualEvidenceUseCase useCase;
    public DisclosureActualController(ImportDisclosureActualEvidenceUseCase useCase){this.useCase=useCase;}

    @PostMapping("/import")
    DisclosureEvidenceImportHistory importStock(@RequestParam String stockCode,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return useCase.importStock(stockCode,from,to);}
    @PostMapping("/import-watchlist")
    List<DisclosureEvidenceImportHistory> importWatchlist(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate baseDate){return useCase.importWatchlist(baseDate);}
    @GetMapping("/import-histories")
    List<DisclosureEvidenceImportHistory> histories(@RequestParam String stockCode){return useCase.findHistories(stockCode);}
    @GetMapping("/evidences")
    List<CatalystEvidence> evidences(@RequestParam String stockCode,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return useCase.findEvidences(stockCode,from,to);}
}
