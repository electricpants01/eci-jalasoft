package eci.technician.custom

import android.content.Context

import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSpinner

/**
 * This custom spinner is just to call the listener all the time even when
 * the user select the item selected, this helps us to create a hint for the UI
*/
class CancelCodeSpinner : AppCompatSpinner {

    constructor(context: Context):super(context)
    constructor(context: Context, attrs: AttributeSet):super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int):super(context, attrs, defStyle)

    override fun setSelection(position: Int) {
        val sameSelected = position == selectedItemPosition;
        super.setSelection(position);
        if (sameSelected) {
            // Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
            onItemSelectedListener?.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
        }
    }

    override fun setSelection(position: Int, animate: Boolean) {
        val sameSelected = position == selectedItemPosition;
        super.setSelection(position, animate);
        if (sameSelected) {
            // Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
            onItemSelectedListener?.onItemSelected(this, getSelectedView(), position, getSelectedItemId());
        }
    }

}