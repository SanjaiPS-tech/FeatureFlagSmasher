package com.featureflaglite.featureflagsmasher.mapper;

import com.featureflaglite.featureflagsmasher.dto.ChangeLogResponse;
import com.featureflaglite.featureflagsmasher.dto.FeatureFlagResponse;
import com.featureflaglite.featureflagsmasher.dto.FlagStateResponse;
import com.featureflaglite.featureflagsmasher.entity.ChangeLog;
import com.featureflaglite.featureflagsmasher.entity.FeatureFlag;
import com.featureflaglite.featureflagsmasher.entity.FlagState;

/**
 * Utility class for mapping entities to DTOs.
 * Keeps mapping logic centralized and avoids duplication.
 */
public final class EntityMapper {

    private EntityMapper() {
        // Prevent instantiation
    }

    public static FeatureFlagResponse toFeatureFlagResponse(FeatureFlag flag) {
        return new FeatureFlagResponse(
                flag.getId(),
                flag.getName(),
                flag.getDescription(),
                flag.isDefaultState(),
                flag.getCreatedAt(),
                flag.getUpdatedAt()
        );
    }

    public static FlagStateResponse toFlagStateResponse(FlagState state) {
        return new FlagStateResponse(
                state.getFeatureFlag().getName(),
                state.getEnvironment().getName(),
                state.isEnabled(),
                state.getRolloutPercentage(),
                state.getUpdatedAt()
        );
    }

    public static ChangeLogResponse toChangeLogResponse(ChangeLog changeLog) {
        ChangeLogResponse response = new ChangeLogResponse();
        response.setId(changeLog.getId());
        response.setFlagName(changeLog.getFeatureFlag().getName());
        response.setEnvironment(changeLog.getEnvironment().getName());
        response.setOldEnabled(changeLog.getOldEnabled());
        response.setNewEnabled(changeLog.isNewEnabled());
        response.setOldRolloutPercentage(changeLog.getOldRolloutPercentage());
        response.setNewRolloutPercentage(changeLog.getNewRolloutPercentage());
        response.setChangedBy(changeLog.getChangedBy());
        response.setChangedAt(changeLog.getChangedAt());
        return response;
    }
}
