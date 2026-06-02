package com.sheenadev.diagonalgesture

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ActivitySelectionBottomSheet : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvAppName: TextView
    private lateinit var ivAppIcon: ImageView

    private var packageName: String = ""
    private var appName: String = ""
    private var appIcon: android.graphics.drawable.Drawable? = null

    var onActivitySelected: ((ComponentName) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_activity_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerActivities)
        tvAppName = view.findViewById(R.id.tvAppName)
        ivAppIcon = view.findViewById(R.id.ivAppIcon)

        tvAppName.text = appName
        ivAppIcon.setImageDrawable(appIcon)

        val activities = getExportableActivities(packageName)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ActivityAdapter(activities)
    }

    private fun getExportableActivities(packageName: String): List<ActivityInfo> {
        val activities = mutableListOf<ActivityInfo>()

        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
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

    inner class ActivityAdapter(
        private val activities: List<ActivityInfo>
    ) : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvActivityName: TextView = itemView.findViewById(R.id.tvActivityName)
            val tvActivityClass: TextView = itemView.findViewById(R.id.tvActivityClass)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_activity, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val activity = activities[position]
            val label = activity.name.substringAfterLast('.')
            holder.tvActivityName.text = label
            holder.tvActivityClass.text = activity.name

            holder.itemView.setOnClickListener {
                onActivitySelected?.invoke(ComponentName(packageName, activity.name))
                dismiss()
            }
        }

        override fun getItemCount(): Int = activities.size
    }

    companion object {
        fun newInstance(
            packageName: String,
            appName: String,
            appIcon: android.graphics.drawable.Drawable?
        ): ActivitySelectionBottomSheet {
            return ActivitySelectionBottomSheet().apply {
                this.packageName = packageName
                this.appName = appName
                this.appIcon = appIcon
            }
        }
    }
}
