package com.my.challenger.service.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppVersionServiceTest {

    @Test
    void testCompareVersions() {
        // Equal versions
        assertEquals(0, AppVersionService.compareVersions("1.0.0.268", "1.0.0.268"));
        
        // Newer version (last segment)
        assertTrue(AppVersionService.compareVersions("1.0.0.268", "1.0.0.274") < 0);
        assertTrue(AppVersionService.compareVersions("1.0.0.274", "1.0.0.268") > 0);
        
        // Newer version (middle segment)
        assertTrue(AppVersionService.compareVersions("1.0.0.268", "1.1.0.268") < 0);
        
        // Mismatch that caused the issue
        // Before fix: current=1.0.0.268, latest=0.0.1.273 -> comparison returns 1 (current > latest)
        // After fix: current=1.0.0.268, latest=1.0.0.274 -> comparison returns -1 (current < latest)
        assertTrue(AppVersionService.compareVersions("1.0.0.268", "0.0.1.273") > 0);
        assertTrue(AppVersionService.compareVersions("1.0.0.268", "1.0.0.274") < 0);
        
        // Handling v prefix (though compareVersions doesn't handle it, the service strips it)
        // Service strips 'v' before calling compareVersions
    }
}
