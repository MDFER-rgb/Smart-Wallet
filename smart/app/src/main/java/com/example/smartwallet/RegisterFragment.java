package com.example.smartwallet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private static final int PICK_IMAGE = 100;

    private ImageView imgProfile;
    private Uri selectedImageUri;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public RegisterFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imgProfile = view.findViewById(R.id.imgProfile);

        EditText edtFirst = view.findViewById(R.id.edtFirst);
        EditText edtLast = view.findViewById(R.id.edtLast);
        EditText edtEmail = view.findViewById(R.id.edtEmail);
        EditText edtPassword = view.findViewById(R.id.edtPassword);
        EditText edtConfirm = view.findViewById(R.id.edtConfirmPassword);
        EditText edtPhone = view.findViewById(R.id.edtPhone);
        EditText edtAddress = view.findViewById(R.id.edtAddress);
        EditText edtInitialBalance = view.findViewById(R.id.edtInitialBalance);
        EditText edtMonthlyBudget = view.findViewById(R.id.edtMonthlyBudget);
        Button btnRegister = view.findViewById(R.id.btnRegister);

        imgProfile.setOnClickListener(v -> selectProfileImage());

        btnRegister.setOnClickListener(v -> {

            String first = edtFirst.getText().toString().trim();
            String last = edtLast.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString();
            String confirm = edtConfirm.getText().toString();
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(requireContext(), "Email & password required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirm)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            double initialBalance = 0, monthlyBudget = 0;

            try { initialBalance = Double.parseDouble(edtInitialBalance.getText().toString().trim()); }
            catch (Exception ignored) {}

            try { monthlyBudget = Double.parseDouble(edtMonthlyBudget.getText().toString().trim()); }
            catch (Exception ignored) {}

            double finalInitialBalance = initialBalance;
            double finalMonthlyBudget = monthlyBudget;

            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(authResult -> {

                        String uid = auth.getCurrentUser().getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("firstName", first);
                        user.put("lastName", last);
                        user.put("email", email);
                        user.put("phone", phone);
                        user.put("address", address);

                        if (selectedImageUri != null)
                            user.put("profileImage", selectedImageUri.toString());

                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(a -> {

                                    Map<String, Object> wallet = new HashMap<>();
                                    wallet.put("totalBalance", finalInitialBalance);
                                    wallet.put("maxBalance", finalInitialBalance);

                                    db.collection("wallet").document(uid).set(wallet)
                                            .addOnSuccessListener(w -> {

                                                Map<String, Object> budget = new HashMap<>();
                                                budget.put("monthlyBudget", finalMonthlyBudget);

                                                db.collection("budget").document(uid).set(budget)
                                                        .addOnSuccessListener(b -> {

                                                            db.collection("subscriptions")
                                                                    .document(uid)
                                                                    .collection("items")
                                                                    .document("init")
                                                                    .set(Collections.singletonMap("init", true))
                                                                    .addOnSuccessListener(s -> {

                                                                        if (isAdded()) {
                                                                            requireActivity().getSupportFragmentManager()
                                                                                    .beginTransaction()
                                                                                    .replace(R.id.fragment_container, new DashboardFragment())
                                                                                    .commit();
                                                                        }

                                                                    });
                                                        });
                                            });
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Register failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        });
    }

    private void selectProfileImage() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");
        startActivityForResult(pick, PICK_IMAGE);
    }

    // ----------------------------
    // FORCE IMAGE INTO PERFECT CIRCLE
    // ----------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {

            selectedImageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        requireActivity().getContentResolver(),
                        selectedImageUri
                );

                Bitmap circleBitmap = getCircularBitmap(bitmap);

                imgProfile.setImageBitmap(circleBitmap);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ----------------------------
    // MAKE BITMAP CIRCULAR
    // ----------------------------
    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);

        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);

        return output;
    }
}
