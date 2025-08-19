package eci.technician.activities

import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import eci.technician.R
import eci.technician.databinding.ActivityTimeCardsCalendarBinding
import eci.technician.fragments.TimeCardsFragment
import eci.technician.helpers.DateTimeHelper.toCalendar
import eci.technician.helpers.DateTimeHelper.toTimeCardFormat
import eci.technician.repository.TechnicianTimeRepository
import eci.technician.viewmodels.TimeCardsFragmentViewModel
import eci.technician.weekview.weekviewp.MonthLoader
import eci.technician.weekview.weekviewp.WeekView
import eci.technician.weekview.weekviewp.WeekViewEvent
import java.util.*

class TimeCardsCalendarActivity : AppCompatActivity(), WeekView.EventClickListener,
    MonthLoader.MonthChangeListener, WeekView.EventLongPressListener,
    WeekView.EmptyViewLongPressListener, WeekView.ScrollListener {

    private val viewModel: TimeCardsFragmentViewModel by viewModels()
    private var eventList: MutableList<WeekViewEvent> = mutableListOf()
    private lateinit var bundleTitle: String
    private var timeCardDateInMillis: Long = 0
    private lateinit var binding: ActivityTimeCardsCalendarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeCardsCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bundleTitle = intent?.getStringExtra(TimeCardsFragment.SHIFT_TITLE_KEY) ?: ""
        timeCardDateInMillis = intent?.getLongExtra(TimeCardsFragment.SHIFT_DATE_KEY, 0) ?: 0
        setSupportActionBar(binding.appbarIncluded.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setBasicWeekView()

        title = bundleTitle

        if (timeCardDateInMillis > 0) {
            val date = Date(timeCardDateInMillis)
            viewModel.fetchTimeCards(date.toTimeCardFormat())
            binding.weekView.goToDate(date.toCalendar())
        }

    }

    private fun setBasicWeekView() {
        binding.weekView.setshadow(binding.myshadow)
        binding.weekView.setfont(ResourcesCompat.getFont(this, R.font.googlesans_regular), 0)
        binding.weekView.setfont(ResourcesCompat.getFont(this, R.font.googlesansmed), 1)
        binding.weekView.setOnEventClickListener(this)
        binding.weekView.eventLongPressListener = this
        binding.weekView.emptyViewLongPressListener = this
        binding.weekView.scrollListener = this
        binding.weekView.numberOfVisibleDays = 1
        binding.weekView.textSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
                .toInt()
        binding.weekView.eventTextSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
                .toInt()
        binding.weekView.allDayEventHeight =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 26f, resources.displayMetrics)
                .toInt()
        binding.weekView.monthChangeListener = this
    }

    override fun onEventClick(event: WeekViewEvent?, eventRect: RectF?) {
        event?.let {
            Toast.makeText(this, it.name, Toast.LENGTH_LONG).show()
        }
    }

    override fun onMonthChange(newYear: Int, newMonth: Int): MutableList<out WeekViewEvent> {
        return eventList.filter { it.startTime.get(Calendar.MONTH) == newMonth - 1 }.toMutableList()
    }

    override fun onEventLongPress(event: WeekViewEvent?, eventRect: RectF?) {
        event?.let {
            Toast.makeText(this, it.name, Toast.LENGTH_LONG).show()
        }
    }

    override fun onEmptyViewLongPress(time: Calendar?) {
        // do nothing
    }

    override fun onFirstVisibleDayChanged(
        newFirstVisibleDay: Calendar?,
        oldFirstVisibleDay: Calendar?
    ) {
        // do nothing
    }

    override fun onResume() {
        super.onResume()
        setObservers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setObservers() {
        viewModel.timeCardList.observe(this) {
            val timeCardList = it ?: mutableListOf()
            val weekViewEventList =
                TechnicianTimeRepository.createWeekEventListFromTimeCards(timeCardList, baseContext)
            eventList = weekViewEventList
            binding.weekView.notifyDatasetChanged()
        }
    }
}