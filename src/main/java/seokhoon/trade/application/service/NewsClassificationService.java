package seokhoon.trade.application.service;
import org.springframework.stereotype.Service; import seokhoon.trade.domain.research.*; import java.util.*;
@Service
public class NewsClassificationService {
    public Classification classify(String title,String summary){String text=((title==null?"":title)+" "+(summary==null?"":summary)).toLowerCase(Locale.ROOT);
        NewsCategory category=category(text);NewsSentiment sentiment=sentiment(text);NewsImportance importance=importance(text,category);return new Classification(category,sentiment,importance,reason(category,sentiment));}
    private static NewsCategory category(String t){
        if(any(t,"전환사채"," cb ","신주인수권"," bw "))return NewsCategory.CONVERTIBLE_BOND;
        if(any(t,"유상증자","무상증자","증자"))return NewsCategory.CAPITAL_RAISE;
        if(any(t,"소송","제재","규제","조사","압수수색","횡령","배임"))return NewsCategory.REGULATORY;
        if(any(t,"수주","공급계약","계약 체결","납품"))return NewsCategory.ORDER_CONTRACT;
        if(any(t,"영업이익","매출","실적","흑자","적자"))return NewsCategory.EARNINGS;
        if(any(t,"목표주가","목표가","투자의견"))return NewsCategory.PRICE_TARGET;
        if(any(t,"대주주","최대주주","지분 매각"))return NewsCategory.MAJOR_SHAREHOLDER;
        if(any(t,"대표이사","경영권","사임","선임"))return NewsCategory.MANAGEMENT;
        if(any(t,"정책","정부","지원책"))return NewsCategory.POLICY;
        if(any(t,"금리","환율","물가","고용"))return NewsCategory.MACRO;
        if(any(t,"반도체","전력","원전"," ai ","로봇","테마"))return NewsCategory.SECTOR_THEME;
        if(any(t,"출시","신제품","허가","임상"))return NewsCategory.PRODUCT;
        if(any(t,"급락","부도","상장폐지","리콜","위기"))return NewsCategory.RISK;
        return NewsCategory.ETC;}
    private static NewsSentiment sentiment(String t){if(any(t,"적자","급락","하향","소송","제재","횡령","배임","부진","감소","리콜","상장폐지"))return NewsSentiment.NEGATIVE;if(any(t,"흑자","수주","상향","성장","증가","최대","호조","승인"))return NewsSentiment.POSITIVE;return t.isBlank()?NewsSentiment.UNKNOWN:NewsSentiment.NEUTRAL;}
    private static NewsImportance importance(String t,NewsCategory c){if(EnumSet.of(NewsCategory.CAPITAL_RAISE,NewsCategory.CONVERTIBLE_BOND,NewsCategory.REGULATORY,NewsCategory.RISK).contains(c)||any(t,"대규모","최대 규모","상장폐지","횡령","배임"))return NewsImportance.HIGH;if(c==NewsCategory.ETC)return NewsImportance.LOW;return NewsImportance.MEDIUM;}
    private static boolean any(String t,String... values){return Arrays.stream(values).anyMatch(t::contains);} private static String reason(NewsCategory c,NewsSentiment s){return "KEYWORD_RULE category="+c+" sentiment="+s;}
    public record Classification(NewsCategory category,NewsSentiment sentiment,NewsImportance importance,String reason){}
}
