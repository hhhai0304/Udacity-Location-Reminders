package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@LargeTest
@RunWith(AndroidJUnit4::class)
class RemindersActivityTest :
    KoinTest {
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerDataBindingIdling() {
        IdlingRegistry.getInstance()
            .register(com.udacity.project4.utils.Espresso.counting_id_resource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun afterRegistered() {
        IdlingRegistry.getInstance()
            .register(com.udacity.project4.utils.Espresso.counting_id_resource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @Test
    fun onClickFloatingActionButton() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.selectLocation)).check(matches(isDisplayed()))
        activityScenario.close()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 29)
    fun addNewReminderSuccessAndDisplayMessage() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        val activity = getActivity(activityScenario)!!
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("Title todo"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Description todo"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.googleMap)).perform(click())
        onView(withId(R.id.btnSaveLocation)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        // Only work on API 29 and below
        // https://github.com/android/android-test/issues/803
        onView(withText(R.string.reminder_saved)).inRoot(
            withDecorView(not(`is`(activity.window?.decorView)))
        ).check(matches(isDisplayed()))

        onView(withText("Title todo")).check(matches(isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun addNewReminderWithEmptyLocationAndDisplayFailureMessage() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("Title todo"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Description todo"))
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withText(R.string.err_select_location)).check(matches(isDisplayed()))
        activityScenario.close()
    }
}
