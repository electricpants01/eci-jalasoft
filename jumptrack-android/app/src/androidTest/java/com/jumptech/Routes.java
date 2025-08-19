package com.jumptech;


import android.view.View;

import androidx.test.espresso.ViewInteraction;

import com.jumptech.jumppod.R;
import com.jumptech.util.TestData;
import com.jumptech.util.TestUser;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

public class Routes extends BaseTest {

    @Test
    public void routes() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html

        TestUser user = new TestData().users().get(0);
        login(user);


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(3) // Region
                .perform(click());

        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(1) // Distribution
                .perform(click());


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(0) // Route
                .perform(click());


        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html


        ViewInteraction textView = onView(allOf(withId(android.R.id.message), withText("No deliveries for route."),
                childAtPosition(childAtPosition(IsInstanceOf.<View>instanceOf(android.widget.ScrollView.class),
                        0), 1), isDisplayed()));
        textView.check(matches(withText("No deliveries for route.")));
    }
}
