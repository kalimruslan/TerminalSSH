package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.model.SavedConnection
import com.ruslan.terminalssh.domain.repository.ConnectionRepository
import javax.inject.Inject

class DeleteConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    suspend operator fun invoke(connection: SavedConnection) {
        repository.delete(connection)
    }
}
