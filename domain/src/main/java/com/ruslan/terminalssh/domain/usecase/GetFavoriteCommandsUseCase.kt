package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.model.FavoriteCommand
import com.ruslan.terminalssh.domain.repository.FavoriteCommandRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoriteCommandsUseCase @Inject constructor(
    private val repository: FavoriteCommandRepository
) {
    operator fun invoke(connectionId: Long): Flow<List<FavoriteCommand>> {
        return repository.getCommandsForConnection(connectionId)
    }
}
