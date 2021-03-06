package org.togglz.core.manager;

import java.util.List;
import java.util.ServiceLoader;

import org.togglz.core.Feature;
import org.togglz.core.FeatureMetaData;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.spi.ActivationStrategy;
import org.togglz.core.user.FeatureUser;
import org.togglz.core.user.UserProvider;
import org.togglz.core.util.Lists;
import org.togglz.core.util.Validate;

/**
 * Default implementation of {@link FeatureManager}
 * 
 * @author Christian Kaltepoth
 * 
 */
public class DefaultFeatureManager implements FeatureManager {

    private final StateRepository stateRepository;
    private final Class<? extends Feature> featureClazz;
    private final UserProvider userProvider;
    private final List<ActivationStrategy> strategies;

    DefaultFeatureManager(Class<? extends Feature> featureClazz, StateRepository stateRepository, UserProvider userProvider) {
        this.featureClazz = featureClazz;
        this.stateRepository = stateRepository;
        this.userProvider = userProvider;
        this.strategies = Lists.asList(ServiceLoader.load(ActivationStrategy.class).iterator());
        Validate.notEmpty(strategies, "No ActivationStrategy implementations found");
    }

    public Feature[] getFeatures() {
        return featureClazz.getEnumConstants();
    }

    public boolean isActive(Feature feature) {

        FeatureState state = stateRepository.getFeatureState(feature);

        if (state == null) {
            FeatureMetaData metaData = FeatureMetaData.build(feature);
            return metaData.isEnabledByDefault();
        }

        if (state.isEnabled()) {

            // if no strategy is selected, the decision is simple
            String strategyId = state.getStrategyId();
            if (strategyId == null) {
                return true;
            }

            FeatureUser user = userProvider.getCurrentUser();

            // check the selected strategy
            for (ActivationStrategy strategy : strategies) {
                if (strategy.getId().equalsIgnoreCase(strategyId)) {
                    return strategy.isActive(state, user);
                }
            }
        }

        // if the strategy was not found, the feature should be off
        return false;

    }

    public FeatureState getFeatureState(Feature feature) {
        FeatureState state = stateRepository.getFeatureState(feature);
        if (state == null) {
            FeatureMetaData metaData = FeatureMetaData.build(feature);
            state = new FeatureState(feature, metaData.isEnabledByDefault());
        }
        return state;
    }

    public void setFeatureState(FeatureState state) {
        stateRepository.setFeatureState(state);
    }

    @Override
    public FeatureUser getCurrentFeatureUser() {
        return userProvider.getCurrentUser();
    }

}
