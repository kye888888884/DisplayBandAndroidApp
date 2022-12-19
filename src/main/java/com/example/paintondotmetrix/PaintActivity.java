package com.example.paintondotmetrix;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Random;

public class PaintActivity extends Activity {
    PaintView paintView = null;
    Button btnClear = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);

        paintView = findViewById(R.id.paint_view);
        btnClear = findViewById(R.id.btn_clear);

        btnClear.setOnClickListener((v) -> {
            paintView.clearCanvas();
        });
    }
}
