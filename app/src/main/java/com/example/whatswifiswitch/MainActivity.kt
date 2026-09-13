package com.example.whatswifiswitch

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatswifiswitch.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NetworkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = NetworkAdapter(
            onEdit = { network -> showNetworkDialog(network) },
            onDelete = { network -> confirmDelete(network) }
        )
        binding.networksList.layoutManager = LinearLayoutManager(this)
        binding.networksList.adapter = adapter

        binding.addNetworkButton.setOnClickListener { showNetworkDialog(null) }
        binding.enableAccessibilityButton.setOnClickListener { openAccessibilitySettings() }
        binding.saveTargetContactButton.setOnClickListener { saveTargetContact() }
    }

    private fun saveTargetContact() {
        val value = binding.targetContactInput.text.toString().trim()
        getSharedPreferences(Prefs.NAME, MODE_PRIVATE).edit()
            .putString(Prefs.TARGET_CONTACT_KEY, value)
            .apply()
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        binding.targetContactInput.setText(
            getSharedPreferences(Prefs.NAME, MODE_PRIVATE).getString(Prefs.TARGET_CONTACT_KEY, "")
        )
    }

    private fun refreshList() {
        val networks = NetworkStore.getAll(this)
        adapter.submitList(networks)
        binding.emptyText.visibility = if (networks.isEmpty()) View.VISIBLE else View.GONE
        binding.networksList.visibility = if (networks.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showNetworkDialog(existing: WifiNetwork?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_network, null)
        val ssidInput = dialogView.findViewById<EditText>(R.id.dialogSsidInput)
        val passwordInput = dialogView.findViewById<EditText>(R.id.dialogPasswordInput)

        if (existing != null) {
            ssidInput.setText(existing.ssid)
            passwordInput.setText(existing.password)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) R.string.add_network else R.string.edit_network)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val ssid = ssidInput.text.toString().trim()
                val password = passwordInput.text.toString()

                if (TextUtils.isEmpty(ssid) || TextUtils.isEmpty(password)) {
                    Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (existing == null) {
                    NetworkStore.add(this, ssid, password)
                } else {
                    NetworkStore.update(this, existing.id, ssid, password)
                }
                refreshList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(network: WifiNetwork) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_network_title)
            .setMessage(getString(R.string.delete_network_message, network.ssid))
            .setPositiveButton(R.string.delete) { _, _ ->
                NetworkStore.delete(this, network.id)
                refreshList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, R.string.find_service_hint, Toast.LENGTH_LONG).show()
    }
}
