package com.udacity.project4.locationreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {
    private val reminderDTO1 = ReminderDTO("Royal Tower", "NowZone", "235", 10.7634999, 106.68258)
    private val reminderDTO2 = ReminderDTO("Dai hoc NTT", "Quan 4", "589", 10.7632222, 106.333333)
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @Before
    fun init_dataBase() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
        remindersLocalRepository =
            RemindersLocalRepository(remindersDatabase.reminderDao(), TestCoroutineDispatcher())
    }

    @After
    fun close_DataBase() {
        remindersDatabase.close()
    }

    @Test
    fun reminderNotFound() = runBlocking {
        val result = remindersLocalRepository.getReminder(reminderDTO1.id)
        assertThat(result is Result.Error, `is`(true))

        result as Result.Error

        assertThat(result.message, `is`("Reminder not found!"))

    }

    @Test
    fun inserting_FindById() = runBlocking {
        remindersLocalRepository.saveReminder(reminderDTO1)
        val result =
            remindersLocalRepository.getReminder(reminderDTO1.id) as Result.Success<ReminderDTO>
        val loaded = result.data
        assertThat(loaded.longitude, `is`(reminderDTO1.longitude))
        assertThat(loaded.latitude, `is`(reminderDTO1.latitude))
        assertThat(loaded, CoreMatchers.notNullValue())
        assertThat(loaded.id, `is`(reminderDTO1.id))
        assertThat(loaded.description, `is`(reminderDTO1.description))
        assertThat(loaded.location, `is`(reminderDTO1.location))
        assertThat(loaded.title, `is`(reminderDTO1.title))
    }

    @Test
    fun remindersOrNull() = runBlocking {
        remindersDatabase.reminderDao().saveReminder(reminderDTO2)
        remindersDatabase.reminderDao().saveReminder(reminderDTO1)
        val result: Result<List<ReminderDTO>> = remindersLocalRepository.getReminders()
        assertThat(result is Result.Success, `is`(true))
        if (result is Result.Success) assertThat(result.data.isNotEmpty(), `is`(true))
    }

    @Test
    fun add_delete_SingleReminder() = runBlocking {
        remindersLocalRepository.saveReminder(reminderDTO1)
        remindersLocalRepository.deleteAllReminders()
        assertThat(remindersLocalRepository.getReminder(reminderDTO1.id) is Result.Error, `is`(true))
    }

    @Test
    fun deleteAllReminders() = runBlocking {
        remindersLocalRepository.deleteAllReminders()
        val res = remindersLocalRepository.getReminders() as Result.Success
        val dataRes = res.data
        assertThat(dataRes, `is`(emptyList()))
    }
}