package com.dshu610.permanentclock

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


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
            configPhotoPicker(rootKey)

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

        fun configPhotoPicker(rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val photoPickerActivity = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { result: MutableList<Uri>? ->
                with(preferenceManager.sharedPreferences.edit()){
                    putStringSet("photos", result?.map {
                        activity?.contentResolver?.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        it.toString()
                    }?.toMutableSet())
                    commit()
                }
            }

            // add reset settings button and alert dialog
            val photoButton: Preference? = findPreference("photos")
            photoButton?.setOnPreferenceClickListener {
//                val intent = Intent()
//                intent.type = "image/*"
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//                intent.action = Intent.ACTION_GET_CONTENT
//                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
                photoPickerActivity.launch(arrayOf("image/*"))
                true
            }
        }
    }
}