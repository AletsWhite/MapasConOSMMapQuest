package net.ivanvega.mapasconosmdroida;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Activity_RutaConfigurable extends AppCompatActivity {

    EditText latitud1, longitud1, latitud2, longitud2;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ruta_configurable);

        latitud1 =findViewById(R.id.latitud1);
        longitud1 =findViewById(R.id.longitud1);
        latitud2 =findViewById(R.id.latitud2);
        longitud2 =findViewById(R.id.longitud2);

        btn=findViewById(R.id.button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("lat1", latitud1.getText().toString());
                intent.putExtra("lat2", latitud2.getText().toString());
                intent.putExtra("long1", longitud1.getText().toString());
                intent.putExtra("long2", longitud2.getText().toString());
                startActivity(intent);


            }
        });


    }
}