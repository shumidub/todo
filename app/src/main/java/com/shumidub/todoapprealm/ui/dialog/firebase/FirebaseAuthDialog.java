package com.shumidub.todoapprealm.ui.dialog.firebase;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.shumidub.todoapprealm.R;
import com.shumidub.todoapprealm.ui.activity.main.MainActivity;

/**
 * Email/password sign-in & registration backed by Firebase Auth. On success it
 * dismisses and notifies {@link OnAuth}; the caller then proceeds (e.g. uploads).
 */
public class FirebaseAuthDialog extends DialogFragment {

    public interface OnAuth { void onSignedIn(); }

    private OnAuth onAuth;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private View message;

    public void setOnAuth(OnAuth onAuth) { this.onAuth = onAuth; }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = android.view.LayoutInflater
                .from(((MainActivity) requireActivity()).dialogContext())
                .inflate(R.layout.dialog_firebase_auth, null);
        etEmail = view.findViewById(R.id.fb_email);
        etPassword = view.findViewById(R.id.fb_password);
        message = view.findViewById(R.id.fb_message);

        AlertDialog.Builder builder = ((MainActivity) requireActivity()).dialogBuilder();
        builder.setTitle("Sign in to Firebase")
                .setView(view)
                .setPositiveButton("Sign in", null)   // overridden in onStart (no auto-dismiss)
                .setNeutralButton("Register", null)
                .setNegativeButton("Cancel", (d, w) -> d.cancel());
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d == null) return;
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> authenticate(false));
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> authenticate(true));
    }

    private void authenticate(boolean register) {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString();
        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Enter email and password");
            return;
        }
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters");
            return;
        }
        setButtonsEnabled(false);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        com.google.android.gms.tasks.Task<?> task = register
                ? auth.createUserWithEmailAndPassword(email, password)
                : auth.signInWithEmailAndPassword(email, password);
        task.addOnCompleteListener(t -> {
            if (!isAdded()) return;
            if (t.isSuccessful()) {
                if (onAuth != null) onAuth.onSignedIn();
                dismissAllowingStateLoss();
            } else {
                setButtonsEnabled(true);
                String msg = t.getException() == null ? "Authentication failed"
                        : t.getException().getMessage();
                showMessage(msg);
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        AlertDialog d = (AlertDialog) getDialog();
        if (d == null) return;
        d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(enabled);
    }

    private void showMessage(String text) {
        if (message instanceof android.widget.TextView) {
            ((android.widget.TextView) message).setText(text);
            message.setVisibility(View.VISIBLE);
        }
    }
}
