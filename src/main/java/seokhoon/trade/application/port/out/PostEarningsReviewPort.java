package seokhoon.trade.application.port.out;

import seokhoon.trade.domain.research.PostEarningsReview;
import seokhoon.trade.domain.research.ThesisImpact;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PostEarningsReviewPort {
    PostEarningsReview save(PostEarningsReview value);
    Optional<PostEarningsReview> findByEarningsEventId(long earningsEventId);
    List<PostEarningsReview> findReviewsByStockCode(String stockCode);
    List<PostEarningsReview> findByReviewDateBetween(LocalDate from, LocalDate to);
    List<PostEarningsReview> findByThesisImpactIn(List<ThesisImpact> thesisImpacts);
}
