package com.mango.mangobus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.mango.anotation.BindView;
import com.mango.knife.MangoKnife;
import com.mango.mbus.MangoBus;

/**
 * Author: Mangoer
 * Time: 2019/5/18 15:48
 * Version:
 * Desc: TODO()
 */
public class OtherActivity extends AppCompatActivity {

    @BindView(R.id.push)
    Button push;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_other);
        MangoKnife.bind(this);
        push.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MangoBus.getInstance().post(new Event("OtherActivity"));
            }
        });
    }
}
