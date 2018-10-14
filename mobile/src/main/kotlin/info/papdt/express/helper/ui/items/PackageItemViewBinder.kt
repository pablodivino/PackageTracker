package info.papdt.express.helper.ui.items

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.RecyclerView
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView
import info.papdt.express.helper.R
import info.papdt.express.helper.model.Kuaidi100Package
import info.papdt.express.helper.model.MaterialIcon
import info.papdt.express.helper.support.ColorGenerator
import info.papdt.express.helper.support.Spanny
import info.papdt.express.helper.support.isFontProviderEnabled
import info.papdt.express.helper.ui.DetailsActivity
import me.drakeet.multitype.ItemViewBinder

object PackageItemViewBinder
    : ItemViewBinder<Kuaidi100Package, PackageItemViewBinder.ViewHolder>() {

    private var statusTitleColor: Int = 0
    private var statusSubtextColor = -1
    private var STATUS_STRING_ARRAY: Array<String>? = null

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        if (STATUS_STRING_ARRAY == null) {
            STATUS_STRING_ARRAY = parent.context.resources
                    .getStringArray(R.array.item_status_description)
        }
        if (statusSubtextColor == -1) {
            statusTitleColor = ContextCompat.getColor(parent.context,
                    R.color.package_list_status_title_color)
            statusSubtextColor = ContextCompat.getColor(parent.context,
                    R.color.package_list_status_subtext_color)
        }

        return ViewHolder(inflater.inflate(R.layout.item_home_package, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Kuaidi100Package) {
        holder.itemData = item

        if (item.name?.isNotEmpty() == true) {
            holder.titleText.text = item.name
        } else {
            holder.titleText.text = item.companyChineseName
        }
        if (item.data != null && item.data!!.size > 0) {
            val status = item.data!![0]
            val spanny = Spanny(STATUS_STRING_ARRAY!![item.getState()], ForegroundColorSpan(statusTitleColor))
                    .append(" - " + status.context, ForegroundColorSpan(statusSubtextColor))
            holder.descText.text = spanny
            holder.timeText.text = status.ftime
            holder.timeText.visibility = View.VISIBLE
        } else {
            /** Set placeholder when cannot get data  */
            holder.descText.setText(R.string.item_text_cannot_get_package_status)
            holder.timeText.visibility = View.GONE
        }

        /** Set bold text when unread  */
        holder.descText.paint.isFakeBoldText = item.unreadNew
        holder.titleText.paint.isFakeBoldText = item.unreadNew || !isFontProviderEnabled

        /** Set CircleImageView  */
        if (item.iconCode?.isNotEmpty() == true) {
            holder.bigCharView.typeface = MaterialIcon.iconTypeface
            holder.bigCharView.text = item.iconCode
        } else {
            holder.bigCharView.typeface = Typeface.DEFAULT
            if (item.name?.isNotEmpty() == true) {
                holder.bigCharView.text = item.name!!.substring(0, 1).toUpperCase()
            } else if (item.companyChineseName?.isNotEmpty() == true) {
                holder.bigCharView.text = item.companyChineseName!!.substring(0, 1).toUpperCase()
            }
        }
        if (item.name?.isNotEmpty() == true) {
            holder.logoView.setImageDrawable(
                    ColorDrawable(ColorGenerator.MATERIAL.getColor(item.name!!)))
        } else if (item.companyChineseName?.isNotEmpty() == true) {
            holder.logoView.setImageDrawable(
                    ColorDrawable(ColorGenerator.MATERIAL.getColor(item.companyChineseName!!)))
        }
    }

    class ViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView), View.OnCreateContextMenuListener {

        internal val logoView: CircleImageView = itemView.findViewById(R.id.iv_logo)
        internal val titleText: AppCompatTextView = itemView.findViewById(R.id.tv_title)
        internal val descText: AppCompatTextView = itemView.findViewById(R.id.tv_other)
        internal val timeText: AppCompatTextView = itemView.findViewById(R.id.tv_time)
        internal val bigCharView: TextView = itemView.findViewById(R.id.tv_first_char)

        val containerView: View = itemView.findViewById(R.id.item_container)

        var itemData: Kuaidi100Package? = null

        init {
            containerView.setOnCreateContextMenuListener(this)
            containerView.setOnClickListener {
                DetailsActivity.launch(it.context as Activity, itemData!!)
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu,
                                         v: View, info: ContextMenu.ContextMenuInfo?) {
            // (parentActivity as? MainActivity)?.onContextMenuCreate(itemData!!)
            menu.setHeaderTitle(itemData!!.name)
            if (!itemData!!.unreadNew) {
                menu.add(Menu.NONE, R.id.action_set_unread, 0, R.string.action_set_unread)
            }
            menu.add(Menu.NONE, R.id.action_share, 0, R.string.action_share)
            menu.add(Menu.NONE, R.id.action_delete, 0, R.string.action_remove)
        }
    }

}