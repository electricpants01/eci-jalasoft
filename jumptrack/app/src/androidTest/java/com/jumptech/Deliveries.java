package com.jumptech;

import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;

import com.jumptech.jumppod.R;
import com.jumptech.util.TestData;
import com.jumptech.util.TestUser;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

public class Deliveries extends BaseTest {

    @Test
    public void deliveries() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html

        TestUser user = new TestData().users().get(0);
        login(user);


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(3) // Region
                .perform(click());

        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(5) // Distribution
                .perform(click());


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(0) // Route
                .perform(click());

        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0) // Delivery
                .perform(click());

        ViewInteraction textView = onView(
                allOf(withId(R.id.address),

                        isDisplayed()));
        textView.check(matches(withText("333 South 7th Street\nSte 1000\nMinneapolis, MN 55402")));
    }


    @Test
    public void notes() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html

        TestUser user = new TestData().users().get(0);
        login(user);


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(3) // Region
                .perform(click());

        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(5) // Distribution
                .perform(click());


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(0) // Route
                .perform(click());

        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0) // Delivery
                .perform(click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.noteButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.signingLayout),
                                        2),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.stopLevelNoteText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("Notes from emulator"), closeSoftKeyboard());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.stopLevelNoteSaveButton), withText("Save"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                1),
                        isDisplayed()));

        appCompatButton2.perform(click());

        onView(withId(R.id.acceptAllButton))
                .perform(click());

        onView(withId(R.id.signature_pad))
                .perform(swipeRight());

        onView(withId(R.id.save_button))
                .perform(click());

        onView(withId(R.id.receivedByText))
                .perform(replaceText("Accept Delivery"), closeSoftKeyboard());

        onView(withId(R.id.acceptRefuseButton))
                .perform(click());


        onView(childAtPosition(withId(android.R.id.tabs), 1))
                .perform(click());

        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0)
                .perform(click());

        onView(withId(R.id.stopName))
                .check(matches(withText("OK Software")));
    }

    @Test
    public void partialReasons() {
        TestUser user = new TestData().users().get(0);
        login(user);


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(3) // Region
                .perform(click());

        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(5) // Distribution
                .perform(click());


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(0) // Route
                .perform(click());

        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0) // Delivery
                .perform(click());


        onView(ViewMatchers.withId(R.id.rvDeliveries)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        onView(ViewMatchers.withId(R.id.rvProducts)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));


        onView(withId(R.id.partialQuantityEditText))
                .perform(replaceText("1"), closeSoftKeyboard());

        //  , withText(is("8")).
        ViewInteraction appCompatCheckedTextView = onView(
                allOf(withId(R.id.rbOption), withText("Not needed"),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                1),
                        isDisplayed()));
        appCompatCheckedTextView.perform(click());

        ViewInteraction appCompatCheckedTextView2 = onView(
                allOf(withId(R.id.rbOption), withText("Other"),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        appCompatCheckedTextView2.perform(click());

        ViewInteraction appCompatCheckedTextView3 = onView(
                allOf(withId(R.id.rbOption), withText("reason4"),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                5),
                        isDisplayed()));
        appCompatCheckedTextView3.perform(click());

        pressBack();


        onView(withId(R.id.acceptButton))
                .perform(click());

        onView(withId(R.id.signature_pad))
                .perform(swipeRight());

        onView(withId(R.id.save_button))
                .perform(click());

        onView(withId(R.id.receivedByText))
                .perform(replaceText("With partial reasons"), closeSoftKeyboard());

        onView(withId(R.id.acceptRefuseButton))
                .perform(click());

        onView(childAtPosition(withId(android.R.id.tabs), 1))
                .perform(click());

        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0)
                .perform(click());


        onView(ViewMatchers.withId(R.id.rvDeliveries)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        ViewInteraction linearLayout11 = onView(
                allOf(withId(R.id.deliveryInfoLayout),
                        childAtPosition(
                                allOf(withId(R.id.rvProducts),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                2)),
                                0),
                        isDisplayed()));
        linearLayout11.perform(click());
        ViewInteraction checkedTextView = onView(
                allOf(withId(R.id.rbOption),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                                1)),
                                1),
                        isDisplayed()));
        checkedTextView.check(matches(isDisplayed()));

        ViewInteraction checkedTextView2 = onView(
                allOf(withId(R.id.rbOption),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                                1)),
                                3),
                        isDisplayed()));
        checkedTextView2.check(matches(isDisplayed()));

        ViewInteraction checkedTextView3 = onView(
                allOf(withId(R.id.rbOption),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                                1)),
                                5),
                        isDisplayed()));
        checkedTextView3.check(matches(isDisplayed()));

        ViewInteraction checkedTextView4 = onView(
                allOf(withId(R.id.rbOption),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                                1)),
                                5),
                        isDisplayed()));
        checkedTextView4.check(matches(isDisplayed()));


    }

    @Test
    public void partialDelivery() {
        TestUser user = new TestData().users().get(0);
        login(user);


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(3) // Region
                .perform(click());

        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(5) // Distribution
                .perform(click());


        onData(anything()).inAdapterView(withId(R.id.GdrList)).atPosition(0) // Route
                .perform(click());

        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0) // Delivery
                .perform(click());


        onView(ViewMatchers.withId(R.id.rvDeliveries)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        onView(ViewMatchers.withId(R.id.rvProducts)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));


        onView(withId(R.id.partialQuantityEditText))
                .perform(replaceText("1"), closeSoftKeyboard());

        //  , withText(is("8")).
        ViewInteraction appCompatCheckedTextView = onView(
                allOf(withId(R.id.rbOption), withText("Not needed"),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                1),
                        isDisplayed()));
        appCompatCheckedTextView.perform(click());

        ViewInteraction appCompatCheckedTextView2 = onView(
                allOf(withId(R.id.rbOption), withText("Other"),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()));
        appCompatCheckedTextView2.perform(click());

        ViewInteraction appCompatCheckedTextView3 = onView(
                allOf(withId(R.id.rbOption), withText("reason4"),
                        childAtPosition(
                                allOf(withId(R.id.partialReasonsRecyclerView),
                                        childAtPosition(
                                                withClassName(is("android.widget.LinearLayout")),
                                                1)),
                                5),
                        isDisplayed()));
        appCompatCheckedTextView3.perform(click());

        pressBack();
        pressBack();

        openActionBarOverflowOrOptionsMenu(
                ApplicationProvider.getApplicationContext());

        onView(withText("Enable Partial"))
                .perform(click());


        ViewInteraction appCompatCheckBox = onView(
                allOf(withId(R.id.cbxDeliverySelected),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.rvDeliveries),
                                        1),
                                3),
                        isDisplayed()));
        appCompatCheckBox.perform(click());


        onView(withId(R.id.acceptAllButton))
                .perform(click());

        onView(withId(R.id.signature_pad))
                .perform(swipeRight());

        onView(withId(R.id.save_button))
                .perform(click());

        onView(withId(R.id.receivedByText))
                .perform(replaceText("Selected delivery With partial reasons"), closeSoftKeyboard());

        onView(withId(R.id.acceptRefuseButton))
                .perform(click());

        onView(childAtPosition(withId(android.R.id.tabs), 1))
                .perform(click());

        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0)
                .perform(click());


    }
}