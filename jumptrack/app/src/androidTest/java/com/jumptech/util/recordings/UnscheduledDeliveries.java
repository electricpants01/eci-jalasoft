package com.jumptech.util.recordings;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.jumptech.jumppod.R;
import com.jumptech.ui.LauncherActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UnscheduledDeliveries {

    @Rule
    public ActivityTestRule<LauncherActivity> mActivityTestRule = new ActivityTestRule<>(LauncherActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.CAMERA");

    @Test
    public void unscheduledDeliveries() {
        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.username),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatEditText.perform(replaceText(""), closeSoftKeyboard());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.username),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatEditText2.perform(click());

        ViewInteraction actionMenuItemView = onView(
                allOf(withId(R.id.invoiceSelectScanMenuItem), withContentDescription("Scan"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        1),
                                0),
                        isDisplayed()));
        actionMenuItemView.perform(click());

        pressBack();

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.username),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatEditText3.perform(click());

        ViewInteraction appCompatEditText4 = onView(
                allOf(withId(R.id.username),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatEditText4.perform(replaceText("j_canedo_r@hotmail.com"), closeSoftKeyboard());

        ViewInteraction appCompatEditText5 = onView(
                allOf(withId(R.id.password),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText5.perform(replaceText("TEST2006"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.loginButton), withText("Login"),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatButton.perform(click());

        DataInteraction linearLayout = onData(anything())
                .inAdapterView(allOf(withId(R.id.GdrList),
                        childAtPosition(
                                withId(R.id.container),
                                1)))
                .atPosition(0);
        linearLayout.perform(click());

        DataInteraction linearLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.GdrList),
                        childAtPosition(
                                withId(R.id.container),
                                1)))
                .atPosition(0);
        linearLayout2.perform(click());

        DataInteraction linearLayout3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.GdrList),
                        childAtPosition(
                                withId(R.id.container),
                                1)))
                .atPosition(0);
        linearLayout3.perform(click());

        DataInteraction linearLayout4 = onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list),
                        childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                5)))
                .atPosition(0);
        linearLayout4.perform(click());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.acceptAllButton), withText("Accept All"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.signingLayout),
                                        2),
                                3),
                        isDisplayed()));
        appCompatButton2.perform(click());

        ViewInteraction button = onView(
                allOf(withId(R.id.save_button), withText("Accept"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        button.perform(click());

        ViewInteraction editText = onView(
                allOf(withId(R.id.receivedByText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        editText.perform(replaceText("TEST"), closeSoftKeyboard());

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.receivedByText), withText("TEST"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        editText2.perform(pressImeActionButton());

        ViewInteraction button2 = onView(
                allOf(withId(R.id.acceptRefuseButton), withText("Done"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        button2.perform(click());

        ViewInteraction overflowMenuButton = onView(
                allOf(withContentDescription("More options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        2),
                                2),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.title), withText("Unscheduled Pickup"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView.perform(click());

        ViewInteraction appCompatButton3 = onView(
                allOf(withId(R.id.btnAddline), withText("Add Item"),
                        childAtPosition(
                                allOf(withId(R.id.editLayout),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                7)),
                                1),
                        isDisplayed()));
        appCompatButton3.perform(click());

        ViewInteraction appCompatEditText6 = onView(
                allOf(withId(R.id.enterReturnSKU),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText6.perform(replaceText("TOYS"), closeSoftKeyboard());

        ViewInteraction appCompatButton4 = onView(
                allOf(withId(R.id.searchReturnButton), withText("Search"),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatButton4.perform(click());

        ViewInteraction appCompatEditText7 = onView(
                allOf(withId(R.id.partialQuantityEditText), withText("1"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText7.perform(replaceText("20"));

        ViewInteraction appCompatEditText8 = onView(
                allOf(withId(R.id.partialQuantityEditText), withText("20"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText8.perform(closeSoftKeyboard());

        ViewInteraction appCompatButton5 = onView(
                allOf(withId(R.id.saveLineButton), withText("Save"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        4),
                                1),
                        isDisplayed()));
        appCompatButton5.perform(click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.noteButton),
                        childAtPosition(
                                allOf(withId(R.id.signingLayout),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                5)),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction appCompatEditText9 = onView(
                allOf(withId(R.id.stopLevelNoteText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        appCompatEditText9.perform(replaceText("TEST"), closeSoftKeyboard());

        ViewInteraction appCompatButton6 = onView(
                allOf(withId(R.id.stopLevelNoteSaveButton), withText("Save"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                1),
                        isDisplayed()));
        appCompatButton6.perform(click());

        ViewInteraction appCompatButton7 = onView(
                allOf(withId(R.id.acceptButton), withText("Accept Pickup"),
                        childAtPosition(
                                allOf(withId(R.id.signingLayout),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                5)),
                                2),
                        isDisplayed()));
        appCompatButton7.perform(click());

        ViewInteraction button3 = onView(
                allOf(withId(R.id.save_button), withText("Accept"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        button3.perform(click());

        ViewInteraction editText3 = onView(
                allOf(withId(R.id.receivedByText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        editText3.perform(replaceText("TEST"), closeSoftKeyboard());

        ViewInteraction editText4 = onView(
                allOf(withId(R.id.receivedByText), withText("TEST"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        editText4.perform(pressImeActionButton());

        ViewInteraction button4 = onView(
                allOf(withId(R.id.acceptRefuseButton), withText("Done"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        button4.perform(click());

        ViewInteraction overflowMenuButton2 = onView(
                allOf(withContentDescription("More options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        2),
                                2),
                        isDisplayed()));
        overflowMenuButton2.perform(click());

        ViewInteraction appCompatTextView2 = onView(
                allOf(withId(R.id.title), withText("Unscheduled Delivery"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView2.perform(click());

        ViewInteraction appCompatButton8 = onView(
                allOf(withId(R.id.btnAddline), withText("Add Item"),
                        childAtPosition(
                                allOf(withId(R.id.editLayout),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                7)),
                                1),
                        isDisplayed()));
        appCompatButton8.perform(click());

        ViewInteraction appCompatEditText10 = onView(
                allOf(withId(R.id.enterReturnSKU),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText10.perform(replaceText("CARS"), closeSoftKeyboard());

        ViewInteraction appCompatButton9 = onView(
                allOf(withId(R.id.searchReturnButton), withText("Search"),
                        childAtPosition(
                                allOf(withId(R.id.container),
                                        childAtPosition(
                                                withId(R.id.drawerLayout),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatButton9.perform(click());

        ViewInteraction appCompatEditText11 = onView(
                allOf(withId(R.id.partialQuantityEditText), withText("1"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText11.perform(replaceText("5"));

        ViewInteraction appCompatEditText12 = onView(
                allOf(withId(R.id.partialQuantityEditText), withText("5"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        appCompatEditText12.perform(closeSoftKeyboard());

        ViewInteraction appCompatButton10 = onView(
                allOf(withId(R.id.saveLineButton), withText("Save"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        4),
                                1),
                        isDisplayed()));
        appCompatButton10.perform(click());

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withId(R.id.noteButton),
                        childAtPosition(
                                allOf(withId(R.id.signingLayout),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                5)),
                                1),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

        ViewInteraction appCompatEditText13 = onView(
                allOf(withId(R.id.stopLevelNoteText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        appCompatEditText13.perform(replaceText("TEST"), closeSoftKeyboard());

        ViewInteraction appCompatButton11 = onView(
                allOf(withId(R.id.stopLevelNoteSaveButton), withText("Save"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                1),
                        isDisplayed()));
        appCompatButton11.perform(click());

        ViewInteraction appCompatButton12 = onView(
                allOf(withId(R.id.acceptButton), withText("Accept Delivery"),
                        childAtPosition(
                                allOf(withId(R.id.signingLayout),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                5)),
                                2),
                        isDisplayed()));
        appCompatButton12.perform(click());

        ViewInteraction button5 = onView(
                allOf(withId(R.id.save_button), withText("Accept"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        button5.perform(click());

        ViewInteraction editText5 = onView(
                allOf(withId(R.id.receivedByText),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        editText5.perform(replaceText("TEST"), closeSoftKeyboard());

        ViewInteraction editText6 = onView(
                allOf(withId(R.id.receivedByText), withText("TEST"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                1),
                        isDisplayed()));
        editText6.perform(pressImeActionButton());

        ViewInteraction button6 = onView(
                allOf(withId(R.id.acceptRefuseButton), withText("Done"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                2),
                        isDisplayed()));
        button6.perform(click());
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
