package com.terminal_devilal.decision.indicator;

import com.terminal_devilal.decision.exception.DecisionException;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IndicatorProviderRegistry {
    private final Map<String, IndicatorProvider> providers;

    public IndicatorProviderRegistry(List<IndicatorProvider> providerList) {
        providers = providerList.stream()
                .collect(Collectors.toUnmodifiableMap(IndicatorProvider::getIndicatorCode, Function.identity()));
    }

    public IndicatorProvider get(String code) {
        IndicatorProvider provider = providers.get(code);
        if (provider == null) {
            throw new DecisionException("PROVIDER_NOT_FOUND", "No provider registered for indicator: " + code);
        }
        return provider;
    }

    public Set<String> codes() {
        return providers.keySet();
    }
}
