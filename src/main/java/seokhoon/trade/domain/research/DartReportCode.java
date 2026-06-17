package seokhoon.trade.domain.research;

public enum DartReportCode {
    Q1("11013", 1),
    Q2("11012", 2),
    Q3("11014", 3),
    Q4("11011", 4);

    private final String code;
    private final int fiscalQuarter;

    DartReportCode(String code, int fiscalQuarter) {
        this.code = code;
        this.fiscalQuarter = fiscalQuarter;
    }

    public String code() {
        return code;
    }

    public int fiscalQuarter() {
        return fiscalQuarter;
    }

    public static int fiscalQuarterOf(String reportCode) {
        for (DartReportCode value : values()) {
            if (value.code.equals(reportCode)) {
                return value.fiscalQuarter;
            }
        }
        throw new IllegalArgumentException("Unsupported DART reportCode: " + reportCode);
    }

    public static DartReportCode ofQuarter(int quarter) {
        for (DartReportCode value : values()) {
            if (value.fiscalQuarter == quarter) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported fiscal quarter: " + quarter);
    }
}
