package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.model.SavedConnection
import com.ruslan.terminalssh.domain.repository.ConnectionRepository
import javax.inject.Inject

class SaveConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    suspend operator fun invoke(connection: SavedConnection): Long {
        return repository.save(connection)
    }
}
