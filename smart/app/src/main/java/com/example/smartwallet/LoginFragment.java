package com.example.smartwallet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private FirebaseAuth auth;
    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnGoRegister;

    public LoginFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout that contains edtEmail, edtPassword, btnLogin, btnGoRegister
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // find views and guard against null
        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnGoRegister = view.findViewById(R.id.btnGoRegister);

        if (edtEmail == null || edtPassword == null || btnLogin == null || btnGoRegister == null) {
            Toast.makeText(requireContext(), "Login layout is missing required views. Check IDs.", Toast.LENGTH_LONG).show();
            return;
        }

        btnGoRegister.setOnClickListener(v -> {
            if (getActivity() == null) return;
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "Email and password required", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        // small delay ensures user object is available to DashboardFragment
                        view.postDelayed(() -> {
                            if (getActivity() == null) return;
                            getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, new DashboardFragment())
                                    .commitAllowingStateLoss();
                        }, 250);
                    })
                    .addOnFailureListener(e -> {
                        btnLogin.setEnabled(true);
                        Toast.makeText(requireContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}
