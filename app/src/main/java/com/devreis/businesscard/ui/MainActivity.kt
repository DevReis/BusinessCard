package com.devreis.businesscard.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.devreis.businesscard.App
import com.devreis.businesscard.databinding.ActivityMainBinding
import com.devreis.businesscard.util.Image

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as App).repository)
    }

    private val adapter by lazy { BusinessCardAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.recyclerView.adapter = adapter
        getAllBusinessCards()
        insertListener()
    }

    private fun insertListener() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddBusinessCardActivity::class.java))
        }
        adapter.listenerShare = { card ->
            Image.share(this, card)
        }
    }

    private fun getAllBusinessCards() {
        mainViewModel.getAll().observe(this, { businessCards ->
            adapter.submitList(businessCards)
        })
    }
}