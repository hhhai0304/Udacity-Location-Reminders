package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    private lateinit var remindersRepository: FakeDataSource

    //Subject under test
    private lateinit var viewModel: RemindersListViewModel

    @Before
    fun setupViewModel() {
        remindersRepository = FakeDataSource()
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminders_showLoading() {
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()
        Assert.assertTrue(viewModel.showLoading.getOrAwaitValue())
        mainCoroutineRule.resumeDispatcher()
        Assert.assertFalse(viewModel.showLoading.getOrAwaitValue())
    }

    @Test
    fun loadReminders_remainderListNotEmpty() = mainCoroutineRule.runBlockingTest  {
        val reminder = ReminderDTO("Royal Tower", "NowZone", "235", 10.7634999, 106.68258)
        remindersRepository.saveReminder(reminder)
        viewModel.loadReminders()
        Assert.assertTrue(viewModel.remindersList.getOrAwaitValue().isNotEmpty())
    }

    @Test
    fun loadReminders_updateSnackBarValue() {
        mainCoroutineRule.pauseDispatcher()
        remindersRepository.setReturnError(true)
        viewModel.loadReminders()
        mainCoroutineRule.resumeDispatcher()
        Assert.assertEquals("Error getting reminders", viewModel.showSnackBar.getOrAwaitValue())
    }
}