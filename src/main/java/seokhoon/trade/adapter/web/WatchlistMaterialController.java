package seokhoon.trade.adapter.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seokhoon.trade.application.port.in.WatchlistMaterialUseCase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/stocks/{stockCode}/materials")
public class WatchlistMaterialController {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final WatchlistMaterialUseCase useCase;
    private final Clock clock;

    @Autowired
    public WatchlistMaterialController(WatchlistMaterialUseCase useCase) {
        this(useCase, Clock.system(SEOUL));
    }

    WatchlistMaterialController(WatchlistMaterialUseCase useCase, Clock clock) {
        this.useCase = useCase;
        this.clock = clock;
    }

    @PostMapping("/collect")
    WatchlistMaterialUseCase.CollectionResult collect(
            @PathVariable String stockCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        return useCase.collect(stockCode, start, end);
    }

    @GetMapping
    List<WatchlistMaterialUseCase.MaterialItem> find(
            @PathVariable String stockCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate end = to == null ? LocalDate.now(clock) : to;
        LocalDate start = from == null ? end.minusDays(90) : from;
        return useCase.find(stockCode, start, end);
    }
}
