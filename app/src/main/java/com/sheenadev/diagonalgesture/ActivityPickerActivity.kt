package com.sheenadev.diagonalgesture

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.sheenadev.diagonalgesture.databinding.ActivityPickerBinding

class ActivityPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPickerBinding
    private lateinit var appList: MutableList<ResolveInfo>
    private lateinit var adapter: AppListAdapter

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appList = mutableListOf()
        setupUI()
        loadApps()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = AppListAdapter()
        binding.listView.adapter = adapter

        binding.listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val app = appList[position]
            showActivityPicker(app)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                loadApps()
                filterApps(newText ?: "")
                return true
            }
        })
    }

    private fun loadApps() {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val list = pm.queryIntentActivities(mainIntent, 0)
        appList.clear()

        val query = binding.searchView.query.toString()
        for (info in list) {
            val appInfo = info.activityInfo.applicationInfo
            val appName = pm.getApplicationLabel(appInfo).toString()
            
            if (query.isEmpty() || appName.contains(query, ignoreCase = true)) {
                appList.add(info)
            }
        }

        appList.sortBy { pm.getApplicationLabel(it.activityInfo.applicationInfo).toString().lowercase() }
        adapter.notifyDataSetChanged()
    }

    private fun filterApps(query: String) {
        if (query.isEmpty()) {
            loadApps()
        } else {
            val filtered = appList.filter {
                packageManager.getApplicationLabel(it.activityInfo.applicationInfo).toString()
                    .contains(query, ignoreCase = true)
            }
            (adapter as AppListAdapter).updateList(filtered)
        }
    }

    private fun showActivityPicker(app: ResolveInfo) {
        val activities = getExportableActivities(app.activityInfo.packageName)

        if (activities.size <= 1) {
            val mainActivity = activities.firstOrNull()
            if (mainActivity != null) {
                selectApp(app, mainActivity)
            } else {
                selectApp(app, app.activityInfo)
            }
        } else {
            val bottomSheet = ActivitySelectionBottomSheet.newInstance(
                packageName = app.activityInfo.packageName,
                appName = packageManager.getApplicationLabel(app.activityInfo.applicationInfo).toString(),
                appIcon = packageManager.getApplicationIcon(app.activityInfo.applicationInfo)
            )

            bottomSheet.onActivitySelected = { component ->
                val activityInfo = getActivityInfo(component)
                if (activityInfo != null) {
                    selectApp(app, activityInfo)
                }
            }

            bottomSheet.show(supportFragmentManager, "activity_picker")
        }
    }

    private fun getActivityInfo(component: ComponentName): android.content.pm.ActivityInfo? {
        return try {
            val packageInfo = packageManager.getPackageInfo(component.packageName, PackageManager.GET_ACTIVITIES)
            packageInfo.activities?.find { it.name == component.className }
        } catch (e: Exception) {
            null
        }
    }

    private fun getExportableActivities(packageName: String): List<android.content.pm.ActivityInfo> {
        val activities = mutableListOf<android.content.pm.ActivityInfo>()

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            packageInfo.activities?.forEach { activity ->
                if (activity.exported) {
                    activities.add(activity)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return activities.distinctBy { it.name }
    }

    private fun selectApp(launcherApp: ResolveInfo, activityInfo: android.content.pm.ActivityInfo) {
        val pm = packageManager
        val appName = pm.getApplicationLabel(launcherApp.activityInfo.applicationInfo).toString()
        val activityLabel = activityInfo.name.substringAfterLast('.')

        val intent = Intent().apply {
            putExtra(EXTRA_PACKAGE_NAME, launcherApp.activityInfo.packageName)
            putExtra(EXTRA_APP_NAME, appName)
            putExtra(EXTRA_ACTIVITY_NAME, activityInfo.name)
            putExtra(EXTRA_ACTIVITY_LABEL, activityLabel)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    inner class AppListAdapter : BaseAdapter() {
        private var items: List<ResolveInfo> = appList

        fun updateList(newItems: List<ResolveInfo>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): ResolveInfo = items[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_app_list, parent, false)
            val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
            val tvPackageName = view.findViewById<TextView>(R.id.tvPackageName)
            val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)

            val app = getItem(position)
            tvAppName.text = packageManager.getApplicationLabel(app.activityInfo.applicationInfo)
            tvPackageName.text = app.activityInfo.packageName
            ivIcon.setImageDrawable(packageManager.getApplicationIcon(app.activityInfo.applicationInfo))

            return view
        }

        override fun getItemId(position: Int): Long = position.toLong()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_ACTIVITY_NAME = "activity_name"
        const val EXTRA_ACTIVITY_LABEL = "activity_label"
    }
}
