package com.werare.proxy

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxyFragment : Fragment(R.layout.fragment_proxy) {

    private lateinit var prefs: Prefs
    private val proxies = mutableListOf<ProxyServer>()
    private var selectedAppPackage = ""

    private val pickList = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { loadProxies(it) }
    }

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            requireContext().startService(Intent(requireContext(), WerareVpnService::class.java))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        view.findViewById<View>(R.id.btnLoadList).setOnClickListener {
            pickList.launch(arrayOf("text/plain", "text/*", "application/octet-stream", "*/*"))
        }
        view.findViewById<View>(R.id.btnStartProxy).setOnClickListener { startFlow() }

        loadProxiesFromAssets()
        loadApps(view)
    }

    private fun loadProxiesFromAssets() {
        val loaded = runCatching {
            requireContext().assets.open("proxies.txt").bufferedReader().readLines()
                .mapNotNull { ProxyServer.parse(it) }
        }.getOrDefault(emptyList())
        proxies.clear()
        proxies.addAll(loaded)
        fillProxySpinner()
    }

    private fun loadProxies(uri: Uri) {
        val loaded = runCatching {
            requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readLines()
            }?.mapNotNull { ProxyServer.parse(it) } ?: emptyList()
        }.getOrDefault(emptyList())

        if (loaded.isEmpty()) {
            toast("Не найдено ни одного прокси (формат host:port или host:port:user:pass)")
            return
        }
        proxies.clear()
        proxies.addAll(loaded)
        fillProxySpinner()
        toast("Загружено прокси: ${loaded.size}")
    }

    private fun fillProxySpinner() {
        val spinner = requireView().findViewById<Spinner>(R.id.spinnerProxy)
        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            proxies.map { it.toString() }
        )
    }

    private fun loadApps(view: View) {
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = requireContext().packageManager
            val items = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { pm.getApplicationLabel(it).toString() to it.packageName }
                .sortedBy { it.first }

            withContext(Dispatchers.Main) {
                val spinner = view.findViewById<Spinner>(R.id.spinnerApp)
                val names = mutableListOf("Все приложения")
                names.addAll(items.map { it.first })
                spinner.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        selectedAppPackage = if (pos == 0) "" else items[pos - 1].second
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun startFlow() {
        val ctx = requireContext()
        if (proxies.isEmpty()) {
            toast("Сначала загрузите список прокси (.txt)")
            return
        }
        val spinner = requireView().findViewById<Spinner>(R.id.spinnerProxy)
        val chosen = proxies.getOrElse(spinner.selectedItemPosition) { proxies.first() }
        prefs.lastProxy = chosen.toString()
        prefs.targetApp = selectedAppPackage

        if (prefs.showOverlayButton && !Settings.canDrawOverlays(ctx)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${ctx.packageName}")))
            toast("Выдайте разрешение поверх других окон")
            return
        }

        ContextCompat.startForegroundService(ctx, Intent(ctx, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_TOGGLE, true)
        })

        val prepare = VpnService.prepare(ctx)
        if (prepare == null) {
            ctx.startService(Intent(ctx, WerareVpnService::class.java))
        } else {
            vpnPermission.launch(prepare)
        }
        toast("WerareProxy запущен")
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
