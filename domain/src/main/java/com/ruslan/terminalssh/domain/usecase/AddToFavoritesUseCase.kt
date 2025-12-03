package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.repository.FavoriteCommandRepository
import javax.inject.Inject

class AddToFavoritesUseCase @Inject constructor(
    private val repository: FavoriteCommandRepository
) {
    suspend operator fun invoke(connectionId: Long, command: String, description: String? = null) {
        repository.addToFavorites(connectionId, command, description)
    }
}
