package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {
    private var shouldReturnError = false
    private var remindersList = mutableListOf<ReminderDTO>()


    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Error getting reminders")
        }
        remindersList.let { return Result.Success(it) }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = remindersList.find { reminderDTO ->
            reminderDTO.id == id
        }

        return when {
            shouldReturnError -> {
                Result.Error("Error getting reminder $id")
            }

            reminder != null -> {
                Result.Success(reminder)
            }

            else -> {
                Result.Error("Reminder not found!")
            }
        }
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }

}