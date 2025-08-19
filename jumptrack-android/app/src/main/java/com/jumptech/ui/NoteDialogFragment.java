package com.jumptech.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.jumptech.jumppod.R;
import com.jumptech.tracklib.data.Signature;
import com.jumptech.tracklib.repository.SignatureRepository;

import org.apache.commons.lang.StringUtils;

public class NoteDialogFragment extends DialogFragment {

	public static final String FRAGMENT_NAME = NoteDialogFragment.class.getSimpleName();
	private static final String SITE_NAME = "siteName";
	private static final String DELIVERY_NAME = "deliveryName";

	private Button btnSave;
	private OnNoteSaveListener listener;

	public static NoteDialogFragment init(String siteName, String deliveryName) {
		NoteDialogFragment fragment = new NoteDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(SITE_NAME, siteName);
		bundle.putString(DELIVERY_NAME, deliveryName);
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.note_activity, container,
				false);
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		((TextView)rootView.findViewById(R.id.siteName)).setText(getArguments().getString(SITE_NAME));

		String deliveryName = getArguments().getString(DELIVERY_NAME);
		((TextView)rootView.findViewById(R.id.deliveryName)).setText(deliveryName);
		rootView.findViewById(R.id.deliveryLayout).setVisibility(StringUtils.trimToNull(deliveryName) != null ? View.VISIBLE : View.GONE);

		Signature signature = SignatureRepository.newSignature(getContext());
		final EditText etNote = (EditText)rootView.findViewById(R.id.stopLevelNoteText);
		etNote.append(StringUtils.trimToEmpty(signature._note));

		btnSave = (Button) rootView.findViewById(R.id.stopLevelNoteSaveButton);
		btnSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String note = StringUtils.trimToNull(etNote.getText().toString());
				SignatureRepository.updateNote(getContext(), Signature.NEW, note);
				if (listener != null) {
					listener.onSaveNote(note);
				}
				dismiss();
			}
		});
		rootView.findViewById(R.id.stopLevelNoteCancelButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		return rootView;
	}

	@Override
	public void onDestroyView() {
		Dialog dialog = getDialog();
		// handles https://code.google.com/p/android/issues/detail?id=17423
		if (dialog != null && getRetainInstance()) {
			dialog.setDismissMessage(null);
		}
		super.onDestroyView();
	}
	
	public void onCreate(Bundle savedInstanceState) {
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	public void setOnNoteSaveListener(OnNoteSaveListener listener) {
		this.listener = listener;
	}

	public interface OnNoteSaveListener {
		void onSaveNote(String note);
	}
}
