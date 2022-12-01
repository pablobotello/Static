package com.example.StaticsMusica;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    Button btnLogOut, playCloud, pauseCloud;
    RecyclerView recyclerView;
    TextView noMusicTextView, usuario;
    ArrayList<AudioModel> songsList = new ArrayList<>();
    MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    MediaPlayer cloudPlayer;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    String urlAudio;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        noMusicTextView = findViewById(R.id.no_songs_text);
        btnLogOut = findViewById(R.id.btnLogout);
        mAuth = FirebaseAuth.getInstance();
        btnLogOut.setOnClickListener(view -> {
            mediaPlayer.stop();
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        if (checkPermission() == false) {
            requestPermission();
            return;
        }

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);
        while (cursor.moveToNext()) {
            AudioModel songData = new AudioModel(cursor.getString(1), cursor.getString(0), cursor.getString(2));
            if (new File(songData.getPath()).exists())
                songsList.add(songData);
        }

        if (songsList.size() == 0) {
            noMusicTextView.setVisibility(View.VISIBLE);
        } else {
            //recyclerview
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new MusicListAdapter(songsList, getApplicationContext()));
        }
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Cancion");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                urlAudio = snapshot.getValue(String.class);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(MainActivity.this, "Error al obtener la URL del audio.", Toast.LENGTH_SHORT).show();
            }
        });
        playCloud = findViewById(R.id.playCloud);
        pauseCloud = findViewById(R.id.stopCloud);
        playCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                playCloudAudio(urlAudio);   
            }
        });
        pauseCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (cloudPlayer.isPlaying()) {

                    cloudPlayer.stop();
                    cloudPlayer.reset();
                    cloudPlayer.release();


                    Toast.makeText(MainActivity.this, "El audio ha sido pausado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "El audio no se ha reproducido", Toast.LENGTH_SHORT).show();
                }
            }
        });
        try {
            usuario = (TextView)findViewById(R.id.autor);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userEmail = user.getEmail();
                usuario.setText(userEmail);
            } else {
                usuario.setText("Ningun Usuario conectado");
            }
        }catch (Exception e){
            Toast.makeText(this, "Error con la autenticación" + e, Toast.LENGTH_SHORT).show();

        }

    }


    boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            return false;
        }
    }

    void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            Toast.makeText(MainActivity.this,"SE REQUIERE PERMISO DE LECTURA, PERMITA DESDE CONFIGURACIÓN",Toast.LENGTH_SHORT).show();
        }else
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},123);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(recyclerView!=null){
            recyclerView.setAdapter(new MusicListAdapter(songsList,getApplicationContext()));
        }
        FirebaseUser user = mAuth.getCurrentUser();
        Toast.makeText(MainActivity.this, "Sesion reanudada", Toast.LENGTH_SHORT).show();
            String userEmail = user.getEmail();
        if (user == null){
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null){
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }
    private void playCloudAudio(String urlAudio) {

        cloudPlayer = new MediaPlayer();

        cloudPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {

            cloudPlayer.setDataSource(urlAudio);
            cloudPlayer.prepare();
            cloudPlayer.start();


            Toast.makeText(this, "Escuchando canción...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {

            Toast.makeText(this, "No se pudo reproducir la cancion" + e, Toast.LENGTH_SHORT).show();
        }
    }
}