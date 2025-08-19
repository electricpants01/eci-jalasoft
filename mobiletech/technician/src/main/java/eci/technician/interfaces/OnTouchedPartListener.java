package eci.technician.interfaces;

import eci.technician.models.data.UsedPart;

public interface OnTouchedPartListener {
    void onDeletedUsedItem(UsedPart partToDelete, int position);

    void onLongPressedItem(UsedPart selectedPart);
}
