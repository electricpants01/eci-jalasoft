package com.jumptech;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.jakewharton.espresso.OkHttp3IdlingResource;
import com.jumptech.jumppod.R;
import com.jumptech.networking.RetrofitService;
import com.jumptech.ui.LoginActivity;
import com.jumptech.util.TestUser;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BaseTest {

    @Rule
    public ActivityTestRule<LoginActivity> loginActivityTestRule = new ActivityTestRule<>(LoginActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = GrantPermissionRule.grant("android.permission.ACCESS_FINE_LOCATION");

    private OkHttp3IdlingResource idlingResource = null;

    @Before
    public void setUp() {
        idlingResource = OkHttp3IdlingResource.create("okhttp", RetrofitService.httpClient.build());
        IdlingRegistry.getInstance().register(idlingResource);
    }

    @After
    public void tearDown() {
        if (idlingResource != null) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    protected void login(TestUser user) {
        onView(withId(R.id.username))
                .perform(replaceText(user.getUsername()), closeSoftKeyboard());

        onView(withId(R.id.password))
                .perform(replaceText(user.getPassword()), closeSoftKeyboard());

        onView(withId(R.id.loginButton))
                .perform(swipeUp());
    }

    protected void logout() {
        onView(withId(R.id.drawerLayout))
                .perform(DrawerActions.open());

        onView(withId(R.id.navigationView))
                .perform(NavigationViewActions.navigateTo(R.id.LogoutMenuItem));

        onView(withId(android.R.id.button1))
                .perform(click()); // OK
    }

    protected void checkAlertMessage(String message) {
        onView(withId(android.R.id.message))
                .check(matches(withText(message)));
    }


    protected Matcher<View> childAtPosition(final Matcher<View> parentMatcher, final int position) {

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
