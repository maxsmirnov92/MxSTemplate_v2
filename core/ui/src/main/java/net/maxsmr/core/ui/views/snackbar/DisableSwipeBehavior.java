package net.maxsmr.core.ui.views.snackbar;

import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.behavior.SwipeDismissBehavior;
import android.view.View;

public class DisableSwipeBehavior extends SwipeDismissBehavior<Snackbar.SnackbarLayout> {
    @Override
    public boolean canSwipeDismissView(@NonNull View view) {
        return false;
    }
}
