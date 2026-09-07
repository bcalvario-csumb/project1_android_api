package com.example.project1.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project1.data.ProductsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState{
    data object Loading : HomeUiState
    data class Success (val products: String) : HomeUiState
    data class Error (val message : String) : HomeUiState
}
class HomeViewModel (private val repo: ProductsRepository =
ProductsRepository()) : ViewModel(){
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    init {
        load()
    }
    fun load() = viewModelScope.launch{
        _uiState.value = HomeUiState.Loading
        _uiState.value = runCatching {repo.fetchProducts()}.fold(
            onSuccess = {HomeUiState.Success(it)},
            onFailure = { HomeUiState.Error(it.message ?: "Unknown Error")}
        )
    }
}
