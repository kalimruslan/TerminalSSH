package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.model.SavedConnection
import com.ruslan.terminalssh.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavedConnectionsUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    operator fun invoke(): Flow<List<SavedConnection>> {
        return repository.getAllConnections()
    }
}
