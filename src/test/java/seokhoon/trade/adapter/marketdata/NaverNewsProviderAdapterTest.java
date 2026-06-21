package seokhoon.trade.adapter.marketdata;
import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.assertThat;
class NaverNewsProviderAdapterTest {@Test void removesHtmlAndDecodesEntitiesWithoutKeepingExecutableContent(){assertThat(NaverNewsProviderAdapter.clean("<b>삼성전자</b> &amp; 실적 <script>bad()</script>")).isEqualTo("삼성전자 & 실적");}}
