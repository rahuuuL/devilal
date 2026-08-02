package com.terminal_devilal.indicators.pdv.cache;

import java.time.LocalDate;
import java.util.List;

import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;

public final class PDVCacheUtils {

    private PDVCacheUtils() {
    }

    public static int lowerBound(List<PriceDeliveryVolumeEntity> list, LocalDate targetDate) {
        int low = 0;
        int high = list.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (list.get(mid).getDate().isBefore(targetDate)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int upperBound(List<PriceDeliveryVolumeEntity> list, LocalDate targetDate) {
        int low = 0;
        int high = list.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (!list.get(mid).getDate().isAfter(targetDate)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low - 1;
    }
}
