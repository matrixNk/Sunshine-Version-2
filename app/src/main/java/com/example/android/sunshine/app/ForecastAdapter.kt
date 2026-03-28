package com.example.android.sunshine.app

import android.content.Context
import android.database.Cursor
import android.support.v4.widget.CursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * [ForecastAdapter] exposes a list of weather forecasts
 * from a [Cursor] to a [android.widget.ListView].
 */
class ForecastAdapter(context: Context, c: Cursor?, flags: Int) : CursorAdapter(context, c, flags) {

    companion object {
        private const val VIEW_TYPE_COUNT = 2
        private const val VIEW_TYPE_TODAY = 0
        private const val VIEW_TYPE_FUTURE_DAY = 1
    }

    private var mUseTodayLayout = true

    /**
     * Cache of the children views for a forecast list item.
     */
    class ViewHolder(view: View) {
        val iconView: ImageView = view.findViewById(R.id.list_item_icon) as ImageView
        val dateView: TextView = view.findViewById(R.id.list_item_date_textview) as TextView
        val descriptionView: TextView = view.findViewById(R.id.list_item_forecast_textview) as TextView
        val highTempView: TextView = view.findViewById(R.id.list_item_high_textview) as TextView
        val lowTempView: TextView = view.findViewById(R.id.list_item_low_textview) as TextView
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val layoutId = when (getItemViewType(cursor.position)) {
            VIEW_TYPE_TODAY -> R.layout.list_item_forecast_today
            else -> R.layout.list_item_forecast
        }
        val view = LayoutInflater.from(context).inflate(layoutId, parent, false)
        view.tag = ViewHolder(view)
        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val viewHolder = view.tag as ViewHolder

        when (getItemViewType(cursor.position)) {
            VIEW_TYPE_TODAY -> viewHolder.iconView.setImageResource(
                Utility.getArtResourceForWeatherCondition(
                    cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID)
                )
            )
            VIEW_TYPE_FUTURE_DAY -> viewHolder.iconView.setImageResource(
                Utility.getIconResourceForWeatherCondition(
                    cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID)
                )
            )
        }

        val dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE)
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis))

        val description = cursor.getString(ForecastFragment.COL_WEATHER_DESC)
        viewHolder.descriptionView.setText(description)
        viewHolder.iconView.contentDescription = description

        val high = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP)
        viewHolder.highTempView.setText(Utility.formatTemperature(context, high))

        val low = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP)
        viewHolder.lowTempView.setText(Utility.formatTemperature(context, low))
    }

    fun setUseTodayLayout(useTodayLayout: Boolean) {
        mUseTodayLayout = useTodayLayout
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0 && mUseTodayLayout) VIEW_TYPE_TODAY else VIEW_TYPE_FUTURE_DAY

    override fun getViewTypeCount(): Int = VIEW_TYPE_COUNT
}
