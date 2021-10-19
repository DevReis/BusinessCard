package com.devreis.businesscard.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devreis.businesscard.data.BusinessCard
import com.devreis.businesscard.data.BusinessCardRepo

class MainViewModel(private val businessCardRepo: BusinessCardRepo) : ViewModel() {
    fun insert(businessCard: BusinessCard) {
        businessCardRepo.insert(businessCard)
    }

    fun getAll(): LiveData<List<BusinessCard>> {
        return businessCardRepo.getAll()
    }
}

class MainViewModelFactory(private val repo: BusinessCardRepo) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}