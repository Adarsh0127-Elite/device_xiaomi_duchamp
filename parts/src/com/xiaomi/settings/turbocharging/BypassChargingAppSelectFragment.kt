/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.turbocharging

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.xiaomi.settings.R

class BypassChargingAppSelectFragment : PreferenceFragmentCompat() {

    private var searchBar: EditText? = null
    private var category: PreferenceCategory? = null
    private var allApps: List<ApplicationInfo> = emptyList()
    private var pm: PackageManager? = null
    private var autoApps: MutableSet<String> = mutableSetOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        val context = requireContext()

        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        val bgColor = if (typedValue.resourceId != 0) {
            context.getColor(typedValue.resourceId)
        } else {
            typedValue.data
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
        }
        root?.setBackgroundColor(bgColor)

        searchBar = EditText(context).apply {
            id = View.generateViewId()
            hint = context.getString(R.string.bypass_custom_apps_search_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setBackgroundResource(R.drawable.bg_search_rounded)
            setPadding(24, 24, 24, 24)
            setTextColor(context.getColor(R.color.app_name_text_selector))
            setHintTextColor(context.getColor(R.color.app_package_text_selector))
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margin = (context.resources.displayMetrics.density * 16).toInt()
        params.setMargins(margin, margin, margin, (margin / 2))
        layout.addView(searchBar, params)

        val listParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        root?.let { layout.addView(it, listParams) }

        searchBar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                populateAppList(s?.toString()?.trim()?.lowercase().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        return layout
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        preferenceScreen = preferenceManager.createPreferenceScreen(context)

        category = PreferenceCategory(context).apply {
            title = context.getString(R.string.bypass_custom_apps_title)
        }
        preferenceScreen.addPreference(category!!)

        pm = context.packageManager
        val packageManager = pm ?: return
        val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        allApps = installed.filter { app ->
            if (app.packageName == context.packageName) return@filter false
            val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val hasLauncher = packageManager.getLaunchIntentForPackage(app.packageName) != null
            !isSystemApp || hasLauncher
        }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        autoApps = prefs.getStringSet(ChargingUtils.PREF_BYPASS_AUTO_APPS, emptySet())
            ?.toMutableSet() ?: mutableSetOf()

        populateAppList("")
    }

    private fun populateAppList(filter: String) {
        val cat = category ?: return
        val context = context ?: return
        val packageManager = pm ?: return

        cat.removeAll()

        for (app in allApps) {
            val label = app.loadLabel(packageManager).toString()
            val pkg = app.packageName

            if (filter.isNotEmpty() &&
                !label.lowercase().contains(filter) &&
                !pkg.lowercase().contains(filter)
            ) {
                continue
            }

            val pref = SwitchPreferenceCompat(context).apply {
                title = label
                summary = pkg
                key = "bypass_app_$pkg"
                isChecked = autoApps.contains(pkg)
                try {
                    icon = app.loadIcon(packageManager)
                } catch (_: Exception) {}

                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    val isChecked = newValue as Boolean
                    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val currentSet = prefs.getStringSet(ChargingUtils.PREF_BYPASS_AUTO_APPS, emptySet())
                        ?.toMutableSet() ?: mutableSetOf()

                    if (isChecked) {
                        currentSet.add(pkg)
                        autoApps.add(pkg)
                    } else {
                        currentSet.remove(pkg)
                        autoApps.remove(pkg)
                    }

                    prefs.edit().putStringSet(ChargingUtils.PREF_BYPASS_AUTO_APPS, HashSet(currentSet)).apply()
                    ChargingUtils.notifyStateChanged(context)
                    true
                }
            }
            cat.addPreference(pref)
        }
    }
}
