package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.FavoriteCommandRepository
import javax.inject.Inject

class RemoveFromFavoritesUseCase @Inject constructor(
    private val repository: FavoriteCommandRepository
) {
    suspend operator fun invoke(connectionId: Long, command: String) {
        repository.removeFromFavorites(connectionId, command)
    }
}
