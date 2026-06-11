package seokhoon.trade.adapter.marketdata;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

class AfterHoursProviderCondition implements Condition {
    static final String PROVIDER_PROPERTY =
            "tradeguard.market-data.after-hours-provider";
    static final String LEGACY_ENABLED_PROPERTY =
            "tradeguard.market-data.after-hours-enabled";

    @Override
    public boolean matches(
            ConditionContext context,
            AnnotatedTypeMetadata metadata
    ) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(
                ConditionalOnAfterHoursProvider.class.getName()
        );
        if (attributes == null) {
            return false;
        }
        String expected = (String) attributes.get("value");
        return expected.equalsIgnoreCase(effectiveProvider(context));
    }

    private static String effectiveProvider(ConditionContext context) {
        String provider = context.getEnvironment().getProperty(PROVIDER_PROPERTY);
        if (provider != null && !provider.isBlank()) {
            return provider.trim();
        }
        boolean legacyEnabled = context.getEnvironment().getProperty(
                LEGACY_ENABLED_PROPERTY,
                Boolean.class,
                true
        );
        return legacyEnabled ? "fake" : "disabled";
    }
}
