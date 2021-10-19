package com.devreis.businesscard.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.devreis.businesscard.App
import com.devreis.businesscard.R
import com.devreis.businesscard.data.BusinessCard
import com.devreis.businesscard.databinding.ActivityAddBusinessCardBinding
import com.devreis.businesscard.setcolor.ColorPickerPopup
import java.lang.ref.WeakReference

class AddBusinessCardActivity : AppCompatActivity() {
    private val binding by lazy { ActivityAddBusinessCardBinding.inflate(layoutInflater) }
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as App).repository)
    }

    private var mDefaultColor = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        insertListener()
    }

    private fun insertListener() {
        binding.imageButton.setOnClickListener { finish() }
        val phoneFormatter = PhoneNumberFormatter(WeakReference(binding.tiedTelefone), PhoneNumberFormatType.PT_BR)
        binding.tiedTelefone.addTextChangedListener(phoneFormatter)
        binding.btConfirmar.setOnClickListener {
            val businessCard = BusinessCard(
                nome = binding.tiedNome.text.toString(),
                empresa = binding.tiedEmpresa.text.toString(),
                email = binding.tiedEmail.text.toString(),
                telefone = binding.tiedTelefone.text.toString(),
                fundoPersonalizado = mDefaultColor)
            mainViewModel.insert(businessCard)
            Toast.makeText(this, getString(R.string.label_card_inserido), Toast.LENGTH_SHORT).show()
            finish()
        }
        binding.previewSelectedColor.setOnClickListener { v ->
            ColorPickerPopup.Builder(this@AddBusinessCardActivity).initialColor(
                Color.WHITE) // seta a cor inicial da caixa seletora
                .enableBrightness(true) // habilita o slider de brilho
                .enableAlpha(true) // habilita o slider de transparência (canal alfa)
                .okTitle(getString(R.string.label_pronto)) // texto do botão de seleção à esquerda
                .cancelTitle(getString(R.string.label_cancelar)) // texto do botão de cancelar à direita
                .showIndicator(true) // habilita uma pequena amostra da cor selecionada, embaixo do botão cancelar
                .showValue(false) // mostra a cor selecionada em hexadecimal
                // todos os valores podem ser transformados em falsos para desativá-los na caixa de diálogo do seletor de cores.
                .build()
                .show(v, object : ColorPickerPopup.ColorPickerObserver() {
                    override fun onColorPicked(color: Int) {
                        // define a cor que é retornada pelo seletor de cores
                        mDefaultColor = color
                        // agora assim que a janela fecha, define a cor escolhida na caixa de amostra
                        binding.previewSelectedColor.setBackgroundColor(mDefaultColor)
                    }
                })
        }
    }
}