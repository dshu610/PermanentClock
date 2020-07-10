package com.dshu610.permanentclock

import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // add reset settings button and alert dialog
            val resetButton: Preference? = findPreference("reset")
            resetButton?.setOnPreferenceClickListener {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setMessage(R.string.reset_message)
                    .setPositiveButton(
                        R.string.reset_yes_btn
                    ) { dialog, which ->
                        PreferenceManager.getDefaultSharedPreferences(activity).edit()
                            .clear().commit()
                        PreferenceManager.setDefaultValues(
                            activity?.baseContext,
                            R.xml.root_preferences,
                            true
                        )

                        // refresh settings screen
                        preferenceScreen.removeAll()
                        setPreferencesFromResource(R.xml.root_preferences, rootKey)
                    }
                    .setNegativeButton(
                        R.string.reset_no_btn
                    ) { dialog, which -> }
                builder.create()
                builder.show()
                true
            }


        }
    }
}