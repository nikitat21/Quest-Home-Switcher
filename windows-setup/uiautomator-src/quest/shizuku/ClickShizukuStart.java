package quest.shizuku;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

public class ClickShizukuStart extends UiAutomatorTestCase {
    public void testClickWirelessStart() throws Exception {
        UiObject section = new UiObject(new UiSelector().text("Start via Wireless debugging"));
        if (!section.waitForExists(8000)) {
            fail("Shizuku wireless start section was not found");
        }

        // In Shizuku 11.7 the wireless Start control is the first clickable
        // element named Start; UiAutomator clicks the accessibility node itself,
        // so Meta's transformed VR panel coordinates are not involved.
        UiObject start = new UiObject(new UiSelector().text("Start").clickable(true).instance(0));
        if (!start.waitForExists(3000)) {
            fail("Shizuku 11.7 wireless Start control was not found");
        }
        if (!start.click()) {
            fail("Shizuku 11.7 wireless Start control could not be clicked");
        }
    }
}
