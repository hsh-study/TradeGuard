package seokhoon.trade.domain.research;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DisclosureActualPolicyTest {
    @Test void infersConservativeCatalystTypesFromTitles() {
        assertThat(DisclosureActualPolicy.relatedCatalystType("단일판매ㆍ공급계약 체결")).isEqualTo(CatalystType.ORDER_CONTRACT);
        assertThat(DisclosureActualPolicy.relatedCatalystType("분기보고서 제출")).isEqualTo(CatalystType.EARNINGS);
        assertThat(DisclosureActualPolicy.relatedCatalystType("기타 경영사항")).isEqualTo(CatalystType.DISCLOSURE);
    }
    @Test void classifiesDilutionAndGovernanceDisclosuresAsHighImportance() {
        assertThat(DisclosureActualPolicy.importance("유상증자 결정")).isEqualTo(CatalystImportance.HIGH);
        assertThat(DisclosureActualPolicy.importance("최대주주 변경")).isEqualTo(CatalystImportance.HIGH);
        assertThat(DisclosureActualPolicy.disclosureType("전환사채 발행",null)).isEqualTo("HIGH_RISK_DISCLOSURE");
    }
}
