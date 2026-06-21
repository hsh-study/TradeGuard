package seokhoon.trade.application.service;
import org.junit.jupiter.api.Test; import seokhoon.trade.domain.research.*; import static org.assertj.core.api.Assertions.assertThat;
class NewsClassificationServiceTest {private final NewsClassificationService service=new NewsClassificationService();
 @Test void classifiesDeterministicKeywords(){assertThat(service.classify("대규모 공급계약 수주","계약 체결").category()).isEqualTo(NewsCategory.ORDER_CONTRACT);assertThat(service.classify("유상증자 결정","자금 조달").importance()).isEqualTo(NewsImportance.HIGH);assertThat(service.classify("규제 조사와 소송","실적 하향").sentiment()).isEqualTo(NewsSentiment.NEGATIVE);assertThat(service.classify("목표주가 상향","호조").category()).isEqualTo(NewsCategory.PRICE_TARGET);}
}
