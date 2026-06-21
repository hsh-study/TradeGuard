package seokhoon.trade.adapter.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class WatchlistPageController {
    @GetMapping(value = "/operations/watchlist", produces = "text/html;charset=UTF-8")
    @ResponseBody
    String page() throws IOException {
        return resource("static/operations-watchlist.html");
    }

    @GetMapping(value = "/operations/portfolio", produces = "text/html;charset=UTF-8")
    @ResponseBody
    String portfolio() throws IOException {
        return resource("static/operations-portfolio.html");
    }

    @GetMapping(value = "/operations/trading", produces = "text/html;charset=UTF-8")
    @ResponseBody
    String trading() throws IOException {
        return resource("static/operations-trading.html");
    }

    @GetMapping(value = "/operations/chart", produces = "text/html;charset=UTF-8")
    @ResponseBody
    String chart() throws IOException {
        return resource("static/operations-chart.html");
    }

    private static String resource(String path) throws IOException {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
