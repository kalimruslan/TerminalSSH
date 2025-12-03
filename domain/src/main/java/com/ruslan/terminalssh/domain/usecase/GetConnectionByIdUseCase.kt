package com.ruslan.terminalssh.domain.usecase

import com.ruslan.terminalssh.domain.model.SavedConnection
import com.ruslan.terminalssh.domain.repository.ConnectionRepository
import javax.inject.Inject

class GetConnectionByIdUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    suspend operator fun invoke(id: Long): SavedConnection? {
        return repository.getById(id)
    }
}
