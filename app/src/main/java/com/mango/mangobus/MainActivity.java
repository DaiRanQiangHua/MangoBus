package com.mango.mangobus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.mango.anotation.BindView;
import com.mango.knife.MangoKnife;
import com.mango.mbus.Subscribe;
import com.mango.mbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.intent)
    public TextView intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MangoKnife.bind(this);
        intent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,OtherActivity.class));
            }
        });
    }

    @Deprecated
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void subscribeEvent(Event event){
        Log.e(TAG,"subscribeEvent");
        intent.setText(event.getMsg()+"-"+Thread.currentThread().getName());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
