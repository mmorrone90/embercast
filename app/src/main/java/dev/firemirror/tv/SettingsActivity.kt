package dev.firemirror.tv

import android.os.Bundle
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import dev.firemirror.tv.databinding.ActivitySettingsBinding

class SettingsActivity : TvActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        binding = ActivitySettingsBinding.bind(findViewById<ViewGroup>(android.R.id.content).getChildAt(0))
        val preferences = ReceiverPreferences(this)
        if (savedInstanceState == null) {
            binding.receiverNameInput.setText(preferences.receiverName)
            binding.diagnosticLogging.isChecked = preferences.diagnosticLogging
        }
        binding.saveButton.setOnClickListener {
            val name = binding.receiverNameInput.text.toString().trim()
            if (!ReceiverPreferences.isValidName(name)) {
                binding.receiverNameInput.error = getString(R.string.name_error)
                binding.receiverNameInput.requestFocus()
            } else {
                preferences.save(name, binding.diagnosticLogging.isChecked)
                finish()
            }
        }
        binding.receiverNameInput.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                binding.saveButton.requestFocus()
                true
            } else false
        }
        binding.cancelButton.setOnClickListener { finish() }
    }
}
