package com.jumptech.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.jumptech.jumppod.R;

public class FeedbackActivity extends Activity {
	String feedBackTxt = "";
	String feedBackEmail = "";
	String rating = "5";
	EditText emailAddrEditText = null, feedBackEditText = null;
	boolean bol1, bol2, bol3, bol4, bol5;
	boolean boolArr[] = { bol1, bol2, bol3, bol4, bol5 };
	Button b1, b2, b3, b4, b5;
	Button buttonArr[];

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.feedback_activity);
		b1 = (Button)findViewById(R.id.feedback_btn1);
		b2 = (Button)findViewById(R.id.feedback_btn2);
		b3 = (Button)findViewById(R.id.feedback_btn3);
		b4 = (Button)findViewById(R.id.feedback_btn4);
		b5 = (Button)findViewById(R.id.feedback_btn5);
		emailAddrEditText = (EditText)findViewById(R.id.feedback_email);
		feedBackEditText = (EditText)findViewById(R.id.feedback_comment);

		Button sendMailButton = (Button)findViewById(R.id.feedBckButton);

		buttonArr = new Button[] { b1, b2, b3, b4, b5 };
		setSelection(4);

		b1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				rating = "1";
				setSelection(0);
			}
		});

		b2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				rating = "2";
				setSelection(1);
			}
		});

		b3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				rating = "3";
				setSelection(2);
			}
		});

		b4.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				rating = "4";
				setSelection(3);
			}
		});

		b5.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				rating = "5";
				setSelection(4);
			}
		});

		sendMailButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				feedBackTxt = feedBackEditText.getText().toString();
				feedBackEmail = emailAddrEditText.getText().toString();
				sendMail();
			}
		});

	}

	private void setSelection(int sel) {
		if(buttonArr != null) {
			for(int i = 0; i < buttonArr.length; i++) {
				if(i == sel) {
					buttonArr[i].setTextColor(Color.WHITE);
					if(i == 0) buttonArr[i].setBackgroundResource(R.drawable.button_left_sel);
					else if(i == 4) buttonArr[i].setBackgroundResource(R.drawable.button_right_sel);
					else buttonArr[i].setBackgroundResource(R.drawable.button_bkg_sel);
				}
				else {
					buttonArr[i].setTextColor(Color.BLACK);
					if(i == 0) buttonArr[i].setBackgroundResource(R.drawable.button_left);
					else if(i == 4) buttonArr[i].setBackgroundResource(R.drawable.button_right);
					else buttonArr[i].setBackgroundResource(R.drawable.button_bkg);
				}
			}
		}
	}

	public void sendMail() {
		final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("plain/text");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.feedbackEmail) });
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Android feedback");
		emailIntent.putExtra(Intent.EXTRA_TEXT, "Rate : " + rating + '\n' + feedBackTxt + '\n' + "email address : " + feedBackEmail + '\n');
		startActivity(Intent.createChooser(emailIntent, getString(R.string.feedbackSend)));
	}

}
