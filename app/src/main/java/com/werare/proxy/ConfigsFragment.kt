package com.werare.proxy

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ConfigsFragment : Fragment(R.layout.fragment_configs) {

    private val createDoc = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            val c = collect()
            ConfigManager.export(requireContext(), it, c)
            toast("Сохранено: ${c.name}.WerareProxy")
        }
    }

    private val openDoc = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val c = ConfigManager.import(requireContext(), it)
            if (c == null) toast("Не удалось прочитать .WerareProxy файл")
            else { fill(c); toast("Импортировано: ${c.name}") }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinnerType = view.findViewById<Spinner>(R.id.spinnerType)
        spinnerType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("SOCKS5", "HTTP")
        )

        view.findViewById<MaterialButton>(R.id.btnSaveConfig).setOnClickListener {
            createDoc.launch("${collect().name.ifBlank { "config" }}.WerareProxy")
        }
        view.findViewById<MaterialButton>(R.id.btnImportConfig).setOnClickListener {
            openDoc.launch(arrayOf("*/*"))
        }
        view.findViewById<MaterialButton>(R.id.btnShareConfig).setOnClickListener {
            startActivity(ConfigManager.shareIntent(requireContext(), collect()))
        }
    }

    private fun field(id: Int) =
        requireView().findViewById<TextInputEditText>(id)

    private fun collect() = WerareConfig(
        name = field(R.id.edtName).text?.toString()?.trim() ?: "",
        host = field(R.id.edtHost).text?.toString()?.trim() ?: "",
        port = field(R.id.edtPort).text?.toString()?.toIntOrNull() ?: 1080,
        type = requireView().findViewById<Spinner>(R.id.spinnerType).selectedItem?.toString() ?: "SOCKS5",
        user = field(R.id.edtUser).text?.toString()?.trim() ?: "",
        pass = field(R.id.edtPass).text?.toString() ?: ""
    )

    private fun fill(c: WerareConfig) {
        field(R.id.edtName).setText(c.name)
        field(R.id.edtHost).setText(c.host)
        field(R.id.edtPort).setText(c.port.toString())
        field(R.id.edtUser).setText(c.user)
        field(R.id.edtPass).setText(c.pass)
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
