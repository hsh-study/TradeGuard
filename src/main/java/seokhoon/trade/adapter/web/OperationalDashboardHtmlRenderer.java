package seokhoon.trade.adapter.web;

import seokhoon.trade.application.port.in.OperationalDashboardSummary;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;
import seokhoon.trade.domain.operations.BootReadinessReport;

final class OperationalDashboardHtmlRenderer {
    private static final Pattern URL = Pattern.compile("(?i)https?://\\S+");
    private static final Pattern SENSITIVE_ASSIGNMENT = Pattern.compile(
            "(?i)(token|app[ _-]?key|app[ _-]?secret|account[ _-]?no|webhook|source[ _-]?url|receipt[ _-]?no|provider[ _-]?name)\\s*[:=]\\s*\\S+");

    String render(OperationalDashboardSummary d) {
        return render(d, Optional.empty());
    }

    String render(OperationalDashboardSummary d, Optional<BootReadinessReport> bootReadiness) {
        return render(d, bootReadiness, "overview");
    }

    String render(OperationalDashboardSummary d, Optional<BootReadinessReport> bootReadiness,
            String requestedView) {
        View view = View.from(requestedView);
        String overall = !d.blockingIssues().isEmpty() ? "BLOCKED" : !d.warnings().isEmpty() ? "WARNING" : "OK";
        String tone = overall.toLowerCase();
        StringBuilder html = new StringBuilder(16_384);
        html.append("""
                <!doctype html><html lang="ko"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>TradeGuard Operational Dashboard</title>
                <style>
                :root{color-scheme:light;--bg:#f3f6f8;--ink:#17212b;--muted:#607080;--line:#d9e1e7;--ok:#16794b;--okbg:#e7f6ee;--warn:#865d00;--warnbg:#fff3cd;--bad:#a12622;--badbg:#fdebea}
                *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,-apple-system,"Segoe UI",sans-serif}
                main{max-width:1180px;margin:auto;padding:28px 18px 48px}h1{margin:0;font-size:1.7rem}h2{font-size:1.05rem;margin:0 0 13px}.subtitle{color:var(--muted);margin:3px 0 20px}
                .toolbar,.summary{display:flex;gap:12px;align-items:end;flex-wrap:wrap}.summary{align-items:center;margin-bottom:18px}.toolbar{padding:14px;background:white;border:1px solid var(--line);border-radius:10px;margin-bottom:18px}
                label{font-weight:650}input[type=date]{display:block;margin-top:4px;padding:7px;border:1px solid #aebbc5;border-radius:6px}button{padding:8px 12px;border:1px solid #71808c;border-radius:6px;background:white;cursor:pointer}.toggle{display:flex;gap:7px;align-items:center;padding-bottom:7px}
                .badge{font-weight:800;padding:5px 10px;border-radius:999px}.ok{color:var(--ok);background:var(--okbg)}.warning{color:var(--warn);background:var(--warnbg)}.blocked{color:var(--bad);background:var(--badbg)}
                .priority{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-bottom:14px}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.card{background:white;border:1px solid var(--line);border-radius:10px;padding:16px;box-shadow:0 1px 2px #14202b0a}.card.blocking{border-left:5px solid var(--bad)}.card.warnings{border-left:5px solid #d4a20b}.card.actions{border-left:5px solid #3578a8}
                dl{display:grid;grid-template-columns:minmax(130px,1fr) minmax(90px,1.2fr);gap:7px 14px;margin:0}dt{color:var(--muted)}dd{margin:0;text-align:right;font-weight:650;overflow-wrap:anywhere}ul{margin:0;padding-left:20px}.empty{color:var(--ok);font-weight:650}.section-warning{color:var(--warn);margin-top:12px;font-size:.9rem}
                .nav{display:flex;gap:8px;flex-wrap:wrap;margin:0 0 18px}.nav a{padding:8px 12px;border:1px solid var(--line);border-radius:8px;background:white;color:#175d8f;text-decoration:none}.nav a.active{background:#175d8f;color:white;border-color:#175d8f;font-weight:750}.notice{margin-top:14px;border-left:5px solid #3578a8}.links{margin-top:20px}.links a{display:inline-block;margin:4px 14px 4px 0;color:#175d8f}footer{color:var(--muted);margin-top:18px;font-size:.88rem}
                @media(max-width:800px){.priority,.grid{grid-template-columns:1fr}main{padding-top:18px}}
                </style></head><body><main>
                <h1>TradeGuard Operational Dashboard</h1><p class="subtitle">저장된 현재 운영 상태를 보여주는 read-only 화면</p>
                """
        ).append("<form class=\"toolbar\" method=\"get\"><label>Base date<input type=\"date\" name=\"baseDate\" value=\"")
                .append(escape(d.baseDate())).append("\"><input type=\"hidden\" name=\"view\" value=\"")
                .append(view.id).append("\"></label><button type=\"submit\">조회</button><button type=\"button\" id=\"refresh\">새로고침</button><label class=\"toggle\"><input type=\"checkbox\" id=\"autoRefresh\"> 60초 자동 새로고침</label></form>");
        navigation(html, d, view);
        html.append("<div class=\"summary\"><span class=\"badge ").append(tone).append("\">상태: ")
                .append(overall).append("</span><span><strong>Base date:</strong> ").append(escape(d.baseDate()))
                .append("</span><span><strong>Generated at:</strong> ").append(escape(d.generatedAt())).append("</span></div>");
        bootReadiness.ifPresent(report -> html.append("<div class=\"summary\"><span class=\"badge ")
                .append(report.overallStatus() == BootReadinessReport.OverallStatus.READY
                        ? "ok" : report.overallStatus().name().toLowerCase()).append("\">Boot readiness: ")
                .append(report.overallStatus()).append("</span><span><strong>Checked at:</strong> ")
                .append(escape(report.checkedAt())).append("</span></div>"));

        List<String> actions = new ArrayList<>(d.recommendedActions());
        if (bootReadiness.map(BootReadinessReport::overallStatus)
                .filter(status -> status == BootReadinessReport.OverallStatus.BLOCKED).isPresent()) {
            actions.add("Review blocked Boot Readiness Report before operating the application");
        }
        if (view == View.OVERVIEW) {
            html.append("<div class=\"priority\">");
            listCard(html, "Blocking Issues", "blocking", d.blockingIssues(), "없음 — 차단 이슈가 없습니다.");
            listCard(html, "Warnings", "warnings", d.warnings(), "없음 — 경고가 없습니다.");
            listCard(html, "Recommended Actions", "actions", actions, "없음 — 권장 조치가 없습니다.");
            html.append("</div>");
        }
        html.append("<div class=\"grid\">");
        if (view == View.OVERVIEW || view == View.MARKET) renderMarket(html, d);
        if (view == View.RESEARCH) renderResearch(html, d);
        if (view == View.OVERVIEW || view == View.SYSTEM) renderSystem(html, d);
        if (view == View.OVERVIEW || view == View.TRADING) renderTrading(html, d);
        html.append("</div>");
        if (view == View.TRADING) tradingApis(html);
        systemApis(html, d);
        html.append("<footer>이 화면은 조회 전용이며 provider 호출, 주문 생성 또는 자동매매 상태 변경을 수행하지 않습니다.</footer>");
        html.append("""
                <script>
                (()=>{const refresh=()=>location.reload();document.getElementById('refresh').addEventListener('click',refresh);let timer;document.getElementById('autoRefresh').addEventListener('change',e=>{clearInterval(timer);if(e.target.checked)timer=setInterval(refresh,60000)});})();
                </script></main></body></html>
                """);
        return html.toString();
    }

    private static void navigation(StringBuilder html, OperationalDashboardSummary d, View active) {
        html.append("<nav class=\"nav\" aria-label=\"운영 메뉴\">");
        for (View view : View.values()) {
            html.append("<a href=\"/operations/dashboard?baseDate=").append(escape(d.baseDate()))
                    .append("&amp;view=").append(view.id).append("\" class=\"")
                    .append(view == active ? "active" : "").append("\">")
                    .append(view.label).append("</a>");
        }
        html.append("</nav>");
    }

    private static void renderMarket(StringBuilder html, OperationalDashboardSummary d) {
        var m=d.marketDateStatus(); card(html,"Market Date",m.warnings(),"Trading day",yesNo(m.isTradingDay()),"Previous trading day",m.previousTradingDay(),"Next trading day",m.nextTradingDay(),"Calendar source",m.calendarSource());
        var e=d.earlyMarketStatus(); card(html,"Early Market",e.warnings(),"Pre-open candidates",e.preOpenCandidateCount(),"Compressed candidates",e.compressedCandidateCount(),"Follow-ups",e.followUpCount(),"Performance captures",e.performanceCaptureCount(),"Data capture",e.dataCaptureStatus(),"Latest run",e.latestRunStatus());
        var c=d.closingBetStatus(); card(html,"Closing Bet",c.warnings(),"Pre-scan candidates",c.preScanCandidateCount(),"Final candidates",c.finalCandidateCount(),"Pre-scan status",c.latestPreScanStatus(),"Final review status",c.latestFinalReviewStatus());
    }

    private static void renderResearch(StringBuilder html, OperationalDashboardSummary d) {
        var n=d.morningNoteStatus(); card(html,"Morning Note",n.warnings(),"Generated",yesNo(n.generated()),"Trade date",n.tradeDate(),"Action items",n.actionItemCount(),"Critical actions",n.criticalActionItemCount(),"Latest generated",n.latestGeneratedAt(),"Discord enabled",yesNo(n.discordEnabled()));
        var i=d.investorFlowStatus(); card(html,"Investor Flow",i.warnings(),"Ready",yesNo(i.readinessReady()),"Provider enabled",yesNo(i.providerEnabled()),"Amount unit",i.amountUnit(),"Amount unit verified",yesNo(i.amountUnitVerified()),"Stock import",i.latestStockImportStatus(),"Market import",i.latestMarketImportStatus(),"Supply-demand analysis",i.latestSupplyDemandAnalysisStatus(),"Strong accumulation",i.strongAccumulationCount(),"Distribution",i.distributionCount());
        var ea=d.earningsStatus(); card(html,"Earnings",ea.warnings(),"Analyses",ea.earningsAnalysisCount(),"Strong",ea.strongCount(),"Weak",ea.weakCount(),"Data insufficient",ea.dataInsufficientCount(),"Upcoming earnings",ea.upcomingEarningsCount(),"Review required",ea.reviewRequiredCount());
        var da=d.dartStatus(); card(html,"DART / Disclosure",da.warnings(),"DART enabled",yesNo(da.providerEnabled()),"Financial import",da.latestFinancialImportStatus(),"Corp code import",da.latestCorpCodeImportStatus(),"Mappings missing",da.mappingMissingCount(),"Failed imports",da.failedImportCount(),"Disclosure enabled",yesNo(da.disclosureProviderEnabled()),"Disclosure import",da.latestDisclosureImportStatus(),"Failed disclosures",da.failedDisclosureImportCount(),"High importance disclosures",da.highImportanceDisclosureCount());
        var v=d.valuationStatus(); card(html,"Valuation",v.warnings(),"Auto snapshot",v.latestAutoSnapshotStatus(),"Generated",v.generatedCount(),"Insufficient",v.insufficientCount(),"Shares outstanding missing",v.sharesOutstandingMissingCount());
        var news=d.newsStatus(); card(html,"News",news.warnings(),"Provider enabled",yesNo(news.providerEnabled()),"Latest import",news.latestImportStatus(),"Imported today",news.importedTodayCount(),"High importance",news.highImportanceNewsCount(),"Risk",news.riskNewsCount(),"Failed",news.failedImportCount());
        var co=d.consensusStatus(); card(html,"Consensus",co.warnings(),"Earnings consensus",co.earningsConsensusCount(),"Target price consensus",co.targetPriceConsensusCount(),"Stale",co.staleConsensusCount(),"Missing for upcoming earnings",co.missingConsensusForUpcomingEarningsCount());
        var p=d.paperTradingReportStatus(); card(html,"Paper Trading Report",p.warnings(),"Generated",yesNo(p.generated()),"Run ID",p.latestRunId(),"Candidates",p.totalCandidates(),"Win rate",percent(p.winRate()),"Average return",percent(p.averageReturnRate()),"Data insufficient",p.dataInsufficientCount());
        var r=d.replayBacktestStatus(); card(html,"Replay Backtest",r.warnings(),"Run ID",r.latestRunId(),"Strategy",r.latestStrategy(),"Status",r.latestStatus(),"Win rate",percent(r.latestWinRate()),"Average return",percent(r.latestAverageReturnRate()));
    }

    private static void renderSystem(StringBuilder html, OperationalDashboardSummary d) {
        var s=d.schedulerStatus(); card(html,"Scheduler",s.warnings(),"Total today",s.totalToday(),"Succeeded",s.successCount(),"Failed",s.failedCount(),"Skipped",s.skippedCount(),"Latest failed jobs",s.latestFailures().stream().map(f->f.schedulerName()+" ("+f.failedAt()+")").toList());
        var k=d.kisTokenStatus(); card(html,"KIS Token",k.warnings(),"Cache mode",k.cacheMode(),"Real status",k.realTokenStatus(),"Demo status",k.demoTokenStatus(),"Refresh in progress",yesNo(k.refreshInProgress()));
    }

    private static void renderTrading(StringBuilder html, OperationalDashboardSummary d) {
        var l=d.liveTradingReadinessStatus(); card(html,"Live Trading Readiness",l.warnings(),"Ready",yesNo(l.ready()),"Live trading enabled",yesNo(l.liveTradingEnabled()),"KIS trading enabled",yesNo(l.kisTradingEnabled()),"Kill switch enabled",yesNo(l.killSwitchEnabled()),"Blocking reasons",l.blockingReasons());
    }

    private static void tradingApis(StringBuilder html) {
        html.append("<section class=\"card notice\"><h2>실거래 안전 게이트</h2><p>조회 API는 바로 사용할 수 있습니다. 주문 생성·매도·취소는 Live Trading Readiness가 READY이고 운영자가 요청 내용을 재확인하는 별도 화면에서만 제공합니다.</p></section>");
        html.append("<section class=\"card links\"><h2>투자 운영 메뉴</h2><a href=\"/operations/watchlist\">관심종목 · 차트</a><a href=\"/operations/portfolio\">보유종목</a><a href=\"/operations/trading\">지정가 매매</a><a href=\"/operations/accounts\">계좌 관리</a><a href=\"/api/live-trading/readiness\">Readiness</a><a href=\"/api/live-orders\">전체 주문</a><a href=\"/api/live-orders/open\">미체결 주문</a></section>");
    }

    private static void systemApis(StringBuilder html, OperationalDashboardSummary d) {
        html.append("<section class=\"card links\"><h2>시스템·API</h2><a href=\"/api/operations/boot-readiness\">Boot Readiness API</a><a href=\"/api/operations/dashboard?baseDate=").append(escape(d.baseDate())).append("\">Dashboard API</a><a href=\"/actuator/health\">Actuator Health</a><a href=\"/actuator/prometheus\">Actuator Prometheus</a><a href=\"http://localhost:19090\">Prometheus</a><a href=\"http://localhost:13000\">Grafana</a></section>");
    }

    private enum View {
        OVERVIEW("overview", "운영 요약"), MARKET("market", "시장·전략"),
        RESEARCH("research", "리서치"), TRADING("trading", "주문·포지션"),
        SYSTEM("system", "시스템·API");

        private final String id;
        private final String label;
        View(String id, String label) { this.id = id; this.label = label; }
        private static View from(String value) {
            if (value != null) for (View view : values()) if (view.id.equalsIgnoreCase(value)) return view;
            return OVERVIEW;
        }
    }

    private static void card(StringBuilder html, String title, List<String> warnings, Object... rows) {
        html.append("<section class=\"card\"><h2>").append(escape(title)).append("</h2><dl>");
        for (int index=0; index<rows.length; index+=2) {
            html.append("<dt>").append(escape(rows[index])).append("</dt><dd>").append(value(rows[index+1])).append("</dd>");
        }
        html.append("</dl>");
        if (!warnings.isEmpty()) html.append("<div class=\"section-warning\"><strong>Warnings:</strong> ").append(value(warnings)).append("</div>");
        html.append("</section>");
    }

    private static void listCard(StringBuilder html, String title, String css, List<String> values, String empty) {
        html.append("<section class=\"card ").append(css).append("\"><h2>").append(title).append("</h2>");
        if (values.isEmpty()) html.append("<div class=\"empty\">").append(escape(empty)).append("</div>");
        else html.append("<ul>").append(values.stream().map(v -> "<li>"+escape(redact(v))+"</li>").reduce("",String::concat)).append("</ul>");
        html.append("</section>");
    }

    private static String value(Object value) {
        if (value == null) return "—";
        if (value instanceof List<?> list) {
            if (list.isEmpty()) return "없음";
            return list.stream().map(String::valueOf).map(OperationalDashboardHtmlRenderer::redact)
                    .map(OperationalDashboardHtmlRenderer::escape).reduce((a,b)->a+"<br>"+b).orElse("없음");
        }
        return escape(redact(String.valueOf(value)));
    }

    private static String percent(Object value) { return value == null ? "—" : value + "%"; }
    private static String yesNo(boolean value) { return value ? "YES" : "NO"; }
    private static String redact(String value) {
        if (value == null) return "";
        return URL.matcher(SENSITIVE_ASSIGNMENT.matcher(value).replaceAll("[REDACTED]")).replaceAll("[REDACTED]");
    }
    private static String escape(Object value) {
        if (value == null) return "—";
        return String.valueOf(value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
}
