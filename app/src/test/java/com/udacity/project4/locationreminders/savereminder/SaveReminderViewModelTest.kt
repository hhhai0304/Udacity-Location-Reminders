package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class SaveReminderViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var fakeReminderDataSource: FakeDataSource

    @Before
    fun setupSaveReminderViewModel() {
        fakeReminderDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeReminderDataSource
        )
    }

    private fun createFakeReminderDataItem(): ReminderDataItem {
        return ReminderDataItem(
            "Royal Tower", "NowZone", "235", 10.7634999, 106.68258
        )
    }

    private fun createFakeErrorReminderDataItem(): ReminderDataItem {
        return ReminderDataItem(
            "Royal Tower", "NowZone", "", 10.7634999, 106.68258
        )
    }

    @Test
    fun saveReminder_savesReminderToDataSourceAndHidesLoading() =
        runTest {
            val item = createFakeReminderDataItem()
            mainCoroutineRule.pauseDispatcher()
            saveReminderViewModel.saveReminder(item)
            Assert.assertTrue(saveReminderViewModel.showLoading.getOrAwaitValue())
            mainCoroutineRule.resumeDispatcher()
            Assert.assertFalse(saveReminderViewModel.showLoading.getOrAwaitValue())

        }

    @Test
    fun validateEnteredData_showsSnackBarErrorMessage() =
        runTest {
            val item = createFakeErrorReminderDataItem()
            Assert.assertFalse(saveReminderViewModel.validateEnteredData(item))
            Assert.assertEquals(
                R.string.err_select_location,
                saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
            )
        }

    @After
    fun tearDown() {
        stopKoin()
    }
}