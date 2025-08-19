package com.jumptech;

import androidx.test.core.app.ApplicationProvider;

import com.jumptech.jumppod.R;
import com.jumptech.util.TestData;
import com.jumptech.util.TestUser;

import org.junit.Test;

import java.util.List;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

public class UnscheduledDeliveriesTest extends BaseTest {

    @Test
    public void loginTest() {
        List<TestUser> users = new TestData().users();

        for (TestUser user : users) {
            login(user);
            if (user.isValid()) {
                logout();
            } else {
                checkMessage("Username or password incorrect.");
            }
        }
    }

    private void checkMessage(String message) {
        onView(withId(android.R.id.message))
                .check(matches(withText(message)));
    }

    @Test
    public void acceptDeliveries() {
        TestUser user = new TestData().users().get(0);
        login(user);

        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(0) // Region
                .perform(click());

        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(0) // Distribution
                .perform(click());

        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(0) // Route
                .perform(click());

        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0) // Delivery
                .perform(click());

        // Delivery

        onView(withId(R.id.acceptAllButton))
                .perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        onView(withId(R.id.signature_pad))
                .perform(swipeRight());

        onView(withId(R.id.save_button))
                .perform(click());

        onView(withId(R.id.receivedByText))
                .perform(replaceText("Jorge"), closeSoftKeyboard());

        onView(withId(R.id.acceptRefuseButton))
                .perform(click());

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());

        onView(withText("Unscheduled Pickup"))
                .perform(click());

//       onView(allOf(withContentDescription("More options"),
//               childAtPosition(
//                       childAtPosition(
//                               withId(R.id.toolbar),
//                               2),
//                       2),
//               isDisplayed())).perform(click());


    }
}
