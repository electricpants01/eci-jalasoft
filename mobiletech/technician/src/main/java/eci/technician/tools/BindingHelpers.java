package eci.technician.tools;

import androidx.databinding.BindingAdapter;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import eci.technician.helpers.AppAuth;

public class BindingHelpers {
    @BindingAdapter({"bind:userImageUrl"})
    public static void loadImageByUserIdent(ImageView imageView, String userIdent) {
        if (userIdent != null && !userIdent.isEmpty()) {
            String url = AppAuth.getInstance().getServerAddress() + "/Common/GetUserImage?userId=" + userIdent;
            Picasso.with(imageView.getContext())
                    .load(url)
                    .transform(new CircleTransformation())
                    .into(imageView);
        } else {
            imageView.setImageDrawable(null);
        }
    }

    @BindingAdapter("bind:layout_alignParentEnd")
    public static void setAlignParentEnd(View view, boolean alignParentEnd) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();

        if (alignParentEnd) {
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_START);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            }
        } else {
            layoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        }

        view.setLayoutParams(layoutParams);
    }
}
