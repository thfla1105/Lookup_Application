package LookBook.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.JsonArray;
import com.squareup.moshi.Json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.R;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.Inflater;

import Cookie.SaveSharedPreference;
import ImageSelect.SelectActivity;
import Login_Main.activity.LoginActivity;
import Login_Main.activity.MainActivity;
import LookBook.GPSTracker;
import LookBook.LookBookResultData.LookBookResultData;
import LookBook.LookBookResultData.LookBookResultResponse;
import LookBook.currentWeatherData.CurrentBodyData;
import LookBook.currentWeatherData.CurrentItem;
import LookBook.currentWeatherData.CurrentItemsData;
import LookBook.currentWeatherData.CurrentResponseData;
import LookBook.currentWeatherData.CurrentWeatherData;
import LookBook.LookBookData.CoordiData;
import LookBook.LookBookData.CoordiFiveData;
import LookBook.weatherData.BodyData;
import LookBook.weatherData.Item;
import LookBook.weatherData.ItemsData;
import LookBook.weatherData.ResponseData;
import LookBook.weatherData.WeatherData;
import LookBook.network.RetrofitWeather;
import LookBook.network.ServiceApi_Weather;
import network.RetrofitClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import network.ServiceApi;
import LookBook.LookBookData.LookBookData;
import LookBook.LookBookData.LookBookResponse;
import styleList.noticeActivity2;

public class LookBookActivity extends AppCompatActivity {

    private GPSTracker gpsTracker;
    private static final int GPS_ENABLE_REQUEST_CODE=2001;
    private static final int PERMISSIONS_REQUEST_CODE=100;
    String[] REQUIRED_PERMISSIONS={Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    Context context;
    int nx;
    int ny;
    String address;
    String g_lowTemp="0"; //????????????
    String g_highTemp="0"; //????????????
    String g_rainfall="0"; //????????????
    //String g_temp="0"; //????????????
    //String g_skyState="0"; //????????????
    String g_fcstTime="0"; //????????????

    int g_tempConvert=0;  //???????????????
    String g_fcstTime2="0"; //???????????????
    String g_currentTemp="0"; //???????????????
    String g_currentState="0"; //????????????(PTY) ?????? : ??????(0), ???(1), ???/???(2), ???(3), ?????????(4), ?????????(5), ?????????/?????????(6), ?????????(7)
    //????????? ???/?????? ?????? ?????? ?????? ?????? ?????? ?????? (????????????)

    //????????????
    TextView textView_address;
    TextView textView_lowTemp;
    TextView textView_highTemp;
    TextView textView_rainfall;
    //TextView textView_temp;
    //TextView textView_skyState;

    //???????????????
    TextView textView_currentTemp;
    TextView textView_currentState;
    ImageView imageView_currentState;
    Drawable weather_sun;
    Drawable weather_cloud;
    Drawable weather_rain;
    Drawable weather_snow;
    Drawable weather_snow_and_rain;
    Drawable weather_shower;

    //ACC radio
    //private RadioGroup mAccGroupView;
    //private RadioButton mAccButtonView;
    //String accResult;

    //???????????? chip
    ChipGroup mPurposeChipGroup;
    Chip mPurposeChip;
    int purposeResult=-1;

    //Acc chip
    ChipGroup mAccChipGroup;
    Chip mAccChip;
    String accResult="-1";


    private ServiceApi_Weather service_weather;
    private ServiceApi service_lookup;

    public static int TO_GRID = 0;
    public static int TO_GPS = 1;

    private Handler handler = new Handler();

    ArrayList<CoordiFiveData> coordiFiveDataList=new ArrayList<>();
    String id;
    ArrayList<CoordiFiveData> urlsList=new ArrayList<>(); //?????? ?????? ????????? ??? url???

    ProgressDialog serverDialog; //?????? progress bar

    /*
    final String [] purpose
            = new String[] {"??????","??????/??????","???????????????","?????? ??????/?????????","??????","??????"};

    final String [] acc
            = new String[] {"???????????? ??????O","???????????? ??????X"};
     */
    private long backPressedTime = 0;
    @Override
    public void onBackPressed() {
        // 2?????? ?????? ???????????? ??? ??????
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            finishAffinity();
        }

        // ?????? ?????? ?????????
        Toast.makeText(this, "?????? ??? ???????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show();
        backPressedTime = System.currentTimeMillis();
        finish();

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.select_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent=new Intent();
        switch (item.getItemId()){
            case R.id.action_go_main:
                intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lookbook_activity);
        service_weather = RetrofitWeather.getClient().create(ServiceApi_Weather.class);
        service_lookup= RetrofitClient.getClient().create(ServiceApi.class);

        id= (SaveSharedPreference.getString(getApplicationContext(), "ID"));

        serverDialog = new ProgressDialog(LookBookActivity.this);
        serverDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); //progress bar??? ????????? ??????
        serverDialog.setMessage("????????? ??????????????????");


        weather_sun=getApplicationContext().getResources().getDrawable(R.drawable.weather_sun);
        weather_cloud=getApplicationContext().getResources().getDrawable(R.drawable.weather_cloudy);
        weather_rain=getApplicationContext().getResources().getDrawable(R.drawable.weather_rain);
        weather_snow=getApplicationContext().getResources().getDrawable(R.drawable.weather_snow);
        weather_snow_and_rain=getApplicationContext().getResources().getDrawable(R.drawable.weather_snow_and_rain);
        weather_shower=getApplicationContext().getResources().getDrawable(R.drawable.weather_shower);


        if(!checkLocationServicesStatus()){
            showDialogForLocationServiceSetting();
        }
        else{
            checkRunTimePermission();
        }


        /*
        mPurpose = (Button) findViewById(R.id.purpose_btn);
        mPurpose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ListPurposeClick(v);
            }
        });
         */

        /*
        mAcc = (Button) findViewById(R.id.acc_btn);
        mAcc.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ListAccClick(v);
            }
        });

        mAccGroupView = (RadioGroup) findViewById(R.id.acc_group);

        //?????? ???????????? radiobutton
        mAccGroupView.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                mAccButtonView=(RadioButton) findViewById(i);
                Toast.makeText(LookBookActivity.this, mAccButtonView.getText(), Toast.LENGTH_SHORT).show();
                accResult=mAccButtonView.getText().toString();
            }
        });
         */

        //???????????? choice chip
        mPurposeChipGroup= findViewById(R.id.chipgroup);
        mPurposeChipGroup.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, @IdRes int checkedId){
                mPurposeChip=findViewById(checkedId);
                // Toast.makeText(LookBookActivity.this, mPurposeChip.getText(), Toast.LENGTH_SHORT).show();
                if(mPurposeChip.getText().equals("?????? ??????/?????????")){
                    purposeResult=1;
                }
                else if(mPurposeChip.getText().equals("??????")){
                    purposeResult=2;
                }
                else if(mPurposeChip.getText().equals("??????")){
                    purposeResult=3;
                }
                else if(mPurposeChip.getText().equals("??????/??????")){
                    purposeResult=4;
                }
                else if(mPurposeChip.getText().equals("???????????????")){
                    purposeResult=5;
                }
                else if(mPurposeChip.getText().equals("??????")){
                    purposeResult=6;
                }
                Log.e("PURPOSERESULT", String.valueOf(purposeResult));
            }
        });

        //Acc choice chip
        mAccChipGroup= findViewById(R.id.chipgroup2);
        mAccChipGroup.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, @IdRes int checkedId){
                mAccChip=findViewById(checkedId);
                //Toast.makeText(LookBookActivity.this, mAccChip.getText(), Toast.LENGTH_SHORT).show();
                if(mAccChip.getText().equals("?????? ??????")){
                    accResult="beanie";
                }
                else if(mAccChip.getText().equals("????????? ??????")){
                    accResult="scarf";
                }
                else if(mAccChip.getText().equals("????????? ??????")){
                    accResult="cap";
                }
                else if(mAccChip.getText().equals("?????? ??????")){
                    accResult="x";
                }
                Log.e("ACCRESULT", String.valueOf(accResult));
            }
        });

        textView_address=(TextView) findViewById(R.id.addressView);
        textView_lowTemp=(TextView) findViewById(R.id.lowTempView);
        textView_highTemp=(TextView) findViewById(R.id.highTempView);
        textView_rainfall=(TextView) findViewById(R.id.rainfallView);
        //textView_temp=(TextView) findViewById(R.id.tempView);
        //textView_skyState=(TextView) findViewById(R.id.skyStateView);

        textView_currentTemp=(TextView) findViewById(R.id.currentTempView);
        textView_currentState=(TextView) findViewById(R.id.currentStateView);
        imageView_currentState=(ImageView) findViewById(R.id.currentStateImageView);
        gpsTracker = new GPSTracker(LookBookActivity.this);
        double latitude = gpsTracker.getLatitude(); // ??????
        double longitude = gpsTracker.getLongitude(); //??????

        address = getCurrentAddress(latitude, longitude); //???????????? ????????? ????????? ~~

        textView_address.setText(address); //?????? ???????????? ????????????

        //Toast.makeText(LookBookActivity.this, "?????? ??????\n??????"+latitude
        //   +"\n??????"+longitude, Toast.LENGTH_LONG).show();

        //?????? ???????????? ??????, ?????? ????????? id, purpose, currentTemp??? ????????? ???????????? ??????????????? ????????? ???
        Button lookbook_btn=(Button) findViewById(R.id.lookbook_btn); //?????? ?????? ??????
        lookbook_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!(purposeResult==-1) && !accResult.equals("-1")){
                    Log.e("startLookBook", id + " / " + purposeResult + " / " + g_tempConvert);
                    serverDialog.show();
                    startLookBook(new LookBookData(id , purposeResult, g_tempConvert));
                    //Intent intent = new Intent(getApplicationContext(), MergeActivity3.class);
                    //startActivity(intent);
                }
                else
                    Toast.makeText(LookBookActivity.this, "????????? ?????? ????????????!", Toast.LENGTH_SHORT).show();
                //ListPurposeClick(v);
            }
        });


        LatXLngY tmp = convertGRID_GPS(TO_GRID, latitude, longitude); //??????, ????????? ????????? api??? ?????? ???????????? ???

        nx=(int)tmp.x;
        ny=(int)tmp.y;
        startWeather(nx, ny); //?????? api??? ????????? ??????, ????????????
        //startCurrentWeather(nx, ny); //?????? api??? ????????? ??????, ???????????????
        startCounting();

        Log.e(">>", "x = " + tmp.x + ", y = " + tmp.y);
    }

/*
    public void ListPurposeClick(View view) {
        new AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Light_Dialog_Alert).setTitle("??????").setItems(purpose, new DialogInterface.OnClickListener()
        {
            @Override public void onClick(DialogInterface dialog, int which)
            {
                //Intent intent = new Intent(getApplicationContext(), MakeLookBook.class);
                //startActivity(intent);
                Toast.makeText(LookBookActivity.this, "words : " + purpose[which], Toast.LENGTH_LONG).show();
            } }).setNeutralButton("??????", null).show();
    }
 */


    //?????? ?????? 30????????? update
    private void startCounting() {
        handler.post(run);
    }
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            startCurrentWeather(nx, ny); //????????? ??????
            handler.postDelayed(this, 1800000);
        }
    };

    //????????? ?????? index
    public void tempConvert(){
        if(Double.parseDouble(g_currentTemp)<5){
            g_tempConvert=7;
        }
        else if(Double.parseDouble(g_currentTemp)>=5 && Double.parseDouble(g_currentTemp)<9){
            g_tempConvert=6;
        }
        else if(Double.parseDouble(g_currentTemp)>=9 && Double.parseDouble(g_currentTemp)<12){
            g_tempConvert=5;
        }
        else if(Double.parseDouble(g_currentTemp)>=12 && Double.parseDouble(g_currentTemp)<16){
            g_tempConvert=4;
        }
        else if(Double.parseDouble(g_currentTemp)>=16 && Double.parseDouble(g_currentTemp)<19){
            g_tempConvert=3;
        }
        else if(Double.parseDouble(g_currentTemp)>=19 && Double.parseDouble(g_currentTemp)<22){
            g_tempConvert=2;
        }
        else if(Double.parseDouble(g_currentTemp)>=22 && Double.parseDouble(g_currentTemp)<26){
            g_tempConvert=1;
        }
        else if(Double.parseDouble(g_currentTemp)>=26){
            g_tempConvert=0;
        }
        /*
        if(Double.parseDouble(g_currentTemp)<=4){
            g_tempConvert=7;
        }
        else if(Double.parseDouble(g_currentTemp)>=5 && Double.parseDouble(g_currentTemp)<=8){
            g_tempConvert=6;
        }
        else if(Double.parseDouble(g_currentTemp)>=9 && Double.parseDouble(g_currentTemp)<=11){
            g_tempConvert=5;
        }
        else if(Double.parseDouble(g_currentTemp)>=12 && Double.parseDouble(g_currentTemp)<=16){
            g_tempConvert=4;
        }
        else if(Double.parseDouble(g_currentTemp)>=17 && Double.parseDouble(g_currentTemp)<=19){
            g_tempConvert=3;
        }
        else if(Double.parseDouble(g_currentTemp)>=20 && Double.parseDouble(g_currentTemp)<=22){
            g_tempConvert=2;
        }
        else if(Double.parseDouble(g_currentTemp)>=23 && Double.parseDouble(g_currentTemp)<=27){
            g_tempConvert=1;
        }
        else if(Double.parseDouble(g_currentTemp)>=28){
            g_tempConvert=0;
        }
         */
    }

    public void startLookBook(LookBookData data) {
        service_lookup.getCoordiList(data).enqueue(new Callback<LookBookResponse>() {
            @Override
            public void onResponse(Call<LookBookResponse> call, Response<LookBookResponse> response) {
                LookBookResponse result = response.body();
                //Toast.makeText(LookBookActivity.this, result.toString(), Toast.LENGTH_SHORT).show();
                //showProgress(false);

                if (response.isSuccessful()) {
                    result = response.body();
                    if(result!=null) {
                        Log.e("?????? StyleList api1", response.toString());
                        List<CoordiData> coordiDataList = result.getStyleList();
                        Log.e("?????? StyleList api2", coordiDataList.toString());

                        for (int i = 0; i < coordiDataList.size(); i++) {
                            CoordiData coordiData = coordiDataList.get(i);

                            int idnum = coordiData.getIdnum();
                            String styles = coordiData.getStyles();
                            String dress = coordiData.getDress();
                            String top = coordiData.getTop();
                            String bottom = coordiData.getBottom();
                            String outwear = coordiData.getOutwear();

                            int temp = coordiData.getTemp();
                            int weight = coordiData.getWeight();
                            int count = coordiData.getCount();
                            String coordi_literal = coordiData.getCoordi_literal();

                            Log.e("stylist-LookBookAct", idnum + " / " + styles + " / " + temp + " / " + weight + " / " + count);
                            Log.e("stylist-LookBookAct2", top + " / " + bottom + " / " + dress + " / " + outwear + " / " + coordi_literal);
                            coordiFiveDataList.add(new CoordiFiveData(top, bottom, outwear, dress, accResult));
                        }

                        Log.e("coordiFiveDataList0", coordiFiveDataList.get(0).getTop() + coordiFiveDataList.get(0).getBottom());
                        Log.e("coordiFiveDataList1", coordiFiveDataList.get(1).getTop() + coordiFiveDataList.get(1).getBottom());

                        for (int i = 0; i < coordiFiveDataList.size(); i++) {
                            CoordiFiveData coordiFiveData = coordiFiveDataList.get(i);
                            String top = coordiFiveData.getTop();
                            String bottom = coordiFiveData.getBottom();
                            String outer = coordiFiveData.getOuter();
                            String dress = coordiFiveData.getDress();
                            String acc = coordiFiveData.getAcc();
                            startGetUrls(new LookBookResultData(id, top, bottom, outer, dress, acc)); //????????? category?????? ??????
                        }
                    }

                    if(serverDialog !=null){ //progress bar ??????
                        serverDialog.dismiss();
                    }
                    //Intent intent=new Intent(getApplicationContext(), LookBookResultActivity.class);

                    //intent.putExtra("coordiFiveDataList", coordiFiveDataList);
                    //startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call<LookBookResponse> call, Throwable t) {
                Toast.makeText(LookBookActivity.this, "?????? ?????? ?????? ??????", Toast.LENGTH_SHORT).show();
                Log.e("?????? ?????? ?????? ??????", t.getMessage());
                //showProgress(false);
            }
        });
    }

    public void startGetUrls(LookBookResultData data){
        service_lookup.getUrlsList(data).enqueue(new Callback<LookBookResultResponse>() {
            @Override
            public void onResponse(Call<LookBookResultResponse> call, Response<LookBookResultResponse> response) {
                LookBookResultResponse result = response.body();
                //Toast.makeText(LookBookActivity.this, result.toString(), Toast.LENGTH_SHORT).show();
                //showProgress(false);
                if(response.isSuccessful()) {
                    Log.e("?????? StyleList api1", response.toString());
                    Log.e("Url-top", result.getTop());
                    Log.e("Url-bottom", result.getBottom());
                    Log.e("Url-outer", result.getOuter());
                    Log.e("Url-dress", result.getDress());
                    Log.e("Url-acc", result.getAcc());

                    if (result.getTop().equals("1")) {
                        Toast.makeText(LookBookActivity.this, "????????? ?????? ???????????? ????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LookBookActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    if (result.getBottom().equals("1")) {
                        Toast.makeText(LookBookActivity.this, "????????? ?????? ???????????? ????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LookBookActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    if (result.getOuter().equals("1")) {
                        Toast.makeText(LookBookActivity.this, "????????? ?????? ???????????? ????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LookBookActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    if (result.getDress().equals("1")) {
                        Toast.makeText(LookBookActivity.this, "????????? ?????? ???????????? ????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LookBookActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    if (result.getAcc().equals("1")) {
                        Toast.makeText(LookBookActivity.this, "????????? ?????? ???????????? ????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LookBookActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    urlsList.add(new CoordiFiveData(result.getTop(), result.getBottom(), result.getOuter(), result.getDress(), result.getAcc())); //????????? url???

                    if(urlsList.size()==2){
                        Intent intent=new Intent(getApplicationContext(), LookBookResultActivity.class);

                        intent.putExtra("urlsList", urlsList);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onFailure(Call<LookBookResultResponse> call, Throwable t) {
                Toast.makeText(LookBookActivity.this, "?????? ?????? ?????? ??????", Toast.LENGTH_SHORT).show();
                Log.e("?????? ?????? ?????? ??????", t.getMessage());
                //showProgress(false);
            }
        });
    }


    //@Override
    public void onRequestPermissionResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grandResults){
        if(permsRequestCode==PERMISSIONS_REQUEST_CODE && grandResults.length==REQUIRED_PERMISSIONS.length){
            boolean check_result=true;

            for(int result : grandResults){
                if(result != PackageManager.PERMISSION_GRANTED){
                    check_result=false;
                    break;
                }
            }

            if(check_result){
                ;
            }
            else{
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Toast.makeText(LookBookActivity.this, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                else{
                    Toast.makeText(LookBookActivity.this, "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ??????????????????",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    void checkRunTimePermission(){
        int hasFineLocationPermission= ContextCompat.checkSelfPermission(LookBookActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission=ContextCompat.checkSelfPermission(LookBookActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED
                && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
        }
        else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(LookBookActivity.this,
                    REQUIRED_PERMISSIONS[0])){
                Toast.makeText(LookBookActivity.this, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(LookBookActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
            else{
                ActivityCompat.requestPermissions(LookBookActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
    }



    //address ????????????(???????????? ??????????????? ????????? ?????????)
    public String getCurrentAddress( double latitude, double longitude) {
        //????????????... GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    100);
        }
        catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????", Toast.LENGTH_LONG).show();
            // showDialogForLocationServiceSetting();
            return "???????????? ????????? ????????????";
        }
        catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????", Toast.LENGTH_LONG).show();
            // showDialogForLocationServiceSetting();
            return "????????? GPS ??????";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????", Toast.LENGTH_LONG).show();
            //  showDialogForLocationServiceSetting();
            return "?????? ?????????";
        }

        Address address = addresses.get(0);
        //Log.d(address.getAdminArea() + " / " + address.getLocality() + " / " + address.getThoroughfare());
        //return address.getAddressLine(0).toString()+"\n";
        //return address.getAdminArea() + " / " + address.getLocality() + " / " + address.getThoroughfare();
        //return address.getCountryName()+ " / "+address.getAdminArea()+ " / "+ address.getSubLocality() + " / " + address.getThoroughfare();
        //return address.getAdminArea()+ " "+ address.getSubLocality() + " " + address.getThoroughfare();
        return address.getAdminArea()+ " "+ address.getSubLocality();
    }


    //??????????????? GPS ???????????? ?????? ????????????
    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LookBookActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n" + "?????? ????????? ???????????????????");
        builder.setCancelable(true); builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });

        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        builder.create().show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS ????????? ?????????");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }



    public boolean checkLocationServicesStatus(){
        LocationManager locationManager=(LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }



    //??????, ????????? ????????? api??? ?????? ???????????? ???
    class LatXLngY
    {
        public double lat;
        public double lng;

        public double x;
        public double y;

    }

    //??????, ????????? ????????? api??? ?????? ???????????? ???
    private LatXLngY convertGRID_GPS(int mode, double lat_X, double lng_Y )
    {
        double RE = 6371.00877; // ?????? ??????(km)
        double GRID = 5.0; // ?????? ??????(km)
        double SLAT1 = 30.0; // ?????? ??????1(degree)
        double SLAT2 = 60.0; // ?????? ??????2(degree)
        double OLON = 126.0; // ????????? ??????(degree)
        double OLAT = 38.0; // ????????? ??????(degree)
        double XO = 43; // ????????? X??????(GRID)
        double YO = 136; // ???1?????? Y??????(GRID)

        //
        // LCC DFS ???????????? ( code : "TO_GRID"(?????????->??????, lat_X:??????,  lng_Y:??????), "TO_GPS"(??????->?????????,  lat_X:x, lng_Y:y) )
        //


        double DEGRAD = Math.PI / 180.0;
        double RADDEG = 180.0 / Math.PI;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);
        LatXLngY rs = new LatXLngY();

        if (mode == TO_GRID) {
            rs.lat = lat_X;
            rs.lng = lng_Y;
            double ra = Math.tan(Math.PI * 0.25 + (lat_X) * DEGRAD * 0.5);
            ra = re * sf / Math.pow(ra, sn);
            double theta = lng_Y * DEGRAD - olon;
            if (theta > Math.PI) theta -= 2.0 * Math.PI;
            if (theta < -Math.PI) theta += 2.0 * Math.PI;
            theta *= sn;
            rs.x = Math.floor(ra * Math.sin(theta) + XO + 0.5);
            rs.y = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        }
        else {
            rs.x = lat_X;
            rs.y = lng_Y;
            double xn = lat_X - XO;
            double yn = ro - lng_Y + YO;
            double ra = Math.sqrt(xn * xn + yn * yn);
            if (sn < 0.0) {
                ra = -ra;
            }
            double alat = Math.pow((re * sf / ra), (1.0 / sn));
            alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;

            double theta = 0.0;
            if (Math.abs(xn) <= 0.0) {
                theta = 0.0;
            }
            else {
                if (Math.abs(yn) <= 0.0) {
                    theta = Math.PI * 0.5;
                    if (xn < 0.0) {
                        theta = -theta;
                    }
                }
                else theta = Math.atan2(xn, yn);
            }
            double alon = theta / sn + olon;
            rs.lat = alat * RADDEG;
            rs.lng = alon * RADDEG;
        }
        return rs;
    }

    //?????? ?????? api
    public void startWeather(int nx, int ny){
        Date todayDate = new Date(); //?????? ??????
        Date baseDateNotString = new Date(todayDate.getTime()+(1000*60*60*24*-1)); //?????? ??????, ?????? ?????? ????????? ?????? ?????????
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String dates=format.format(todayDate); //?????? ?????? yyyyMMdd?????? String
        //System.out.println(format.format(todayDate)); // 20210220

        SimpleDateFormat format2=new SimpleDateFormat("HH"); //??? ???
        SimpleDateFormat format3=new SimpleDateFormat("mm");// ??? ???
        String hours=format2.format(todayDate); //???????????? String, (ex 1, 23)
        String minutes=format3.format(todayDate); //???????????? String (ex 28)
        int hour=Integer.parseInt(hours); //???????????? ????????? ??????
        int min=Integer.parseInt(minutes); //???????????? ????????? ??????

        //???????????????(fcstTime) ????????????
        if(hour>=0 && hour<=2) {
            g_fcstTime="0000";
        }
        if(hour>=3 && hour<=5) {
            g_fcstTime="0300";
        }
        else if(hour>=6 && hour<=8) {
            g_fcstTime="0600";
        }
        else if(hour>=9 && hour<=11) {
            g_fcstTime="0900";
        }
        else if(hour>=12 && hour<=14) {
            g_fcstTime="1200";
        }
        else if(hour>=15 && hour<=17) {
            g_fcstTime="1500";
        }
        else if(hour>=18 && hour<=20) {
            g_fcstTime="1800";
        }
        else if(hour>=21 && hour<=23) {
            g_fcstTime="2100";
        }

        //String serviceKey = "*";
        String pageNum="1";
        String s_nx = String.valueOf(nx);	//?????? ?????????
        String s_ny = String.valueOf(ny);	//?????? ?????????
        String type = "JSON"; //?????? xml, json ?????? ..
        String baseDate=format.format(baseDateNotString); //???????????? ?????? ?????? (ex 20210219)-?????? ????????? ?????????
        String baseTime="2000"; //???????????? ?????? ?????? (ex 2300)
        String numOfRows = "153"; //??? ????????? ?????? ???

        //2000 ???????????? ?????? 8??? ????????? ???????????? ????????? ?????? ????????????
        if(hour>=20) {
            baseTime="2300";
        }
        if(hour>=23) {
            baseDate=format.format(todayDate); //??????????????? ??????
            baseTime="0200";
        }

        service_weather.getWeather(pageNum, numOfRows, type, baseDate, baseTime, s_nx, s_ny).enqueue(new Callback<WeatherData>() {
            @Override
            public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                WeatherData result = response.body();
                if(response.body() != null) {
                    result = response.body();
                }
                else{
                    Log.e("??????", "?????? ????????????.");
                }
                // Toast.makeText(LoginActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                //Toast.makeText(getApplicationContext(), result.getMessage(), Toast.LENGTH_SHORT).show();

                if(response.isSuccessful()){
                    Log.e("?????? ???????????? api1", response.toString());
                    ResponseData responseData=result.getResponse();
                    Log.e("?????? ???????????? api2", responseData.toString());
                    BodyData bodyData=responseData.getBody();
                    Log.e("?????? ???????????? api3", bodyData.toString());
                    ItemsData itemsData=bodyData.getItems();
                    Log.e("?????? ???????????? api4", itemsData.toString());
                    Item[] itemList=itemsData.getItem();
                    Log.e("?????? ???????????? api5", itemList.toString());
                    /*
                    for(Item item:itemList){
                        result_list.add(item);
                    }
                     */

                    for(int i = 0 ; i < itemList.length; i++) {
                        Item weather = itemList[i];
                        String category=weather.getCategory();
                        String fcstDate=weather.getFcstDate();
                        String fcstTime=weather.getFcstTime();
                        String fcstValue=weather.getFcstValue();

                        if(dates.equals(fcstDate)) {//?????? ???????????? ????????????
                            if(category.equals("TMN")) {
                                //info = "?????? ????????????";
                                // DataValue = DataValue+" %";
                                g_lowTemp=fcstValue;
                                //System.out.println(info + ": " + lowTemp);
                            }
                            if(category.equals("TMX")) {
                                //info = "??? ????????????";
                                // DataValue = DataValue+" %";
                                g_highTemp=fcstValue;
                                //System.out.println(info + ": " + highTemp);
                            }
                            if(g_fcstTime.equals(fcstTime)) { //?????? ???????????? ???????????? ???
                                if (category.equals("POP")) {
                                    //System.out.println("?????????: "+(String)fcstTime);
                                    //g_fcstTime=(String)fcstTime;
                                    //info = "????????????";
                                    // DataValue = DataValue+" %";
                                    g_rainfall=fcstValue;
                                    //System.out.println(info + ": " + rainfall);
                                }
                                /*
                                if (category.equals("T3H")) {
                                    //info = "3????????????";
                                    //DataValue = DataValue + " ???";
                                    g_temp=fcstValue;
                                    //System.out.println(info + ": " + temp);
                                }
                                if(category.equals("SKY")) {
                                    //info = "????????????";
                                    if(fcstValue.equals("1")) {
                                        //skyState = "??????";
                                        g_skyState="1";
                                    }else if(fcstValue.equals("2")) {
                                        //skyState = "???";
                                        g_skyState="2";
                                    }else if(fcstValue.equals("3")) {
                                        //skyState = "????????????";
                                        g_skyState="3";
                                    }else if(fcstValue.equals("4")) {
                                        //skyState = "??????";
                                        g_skyState="4";
                                    }
                                    //System.out.println(info + ": " + skyState);
                                }
                                 */
                            }
                        }
                    } //for??? ???

                    textView_lowTemp.setText("?????? : "+g_lowTemp+"??");
                    textView_highTemp.setText("?????? : "+g_highTemp+"?? / ");
                    textView_rainfall.setText("????????????: "+g_rainfall+"%");
                    /*
                    textView_temp.setText("?????? ??????: "+g_temp);
                    if(g_skyState.equals("1")){
                        textView_skyState.setText("?????? ??????: ??????");
                    }
                    else if(g_skyState.equals("2")){
                        textView_skyState.setText("?????? ??????: ???");
                    }
                    else if(g_skyState.equals("3")){
                        textView_skyState.setText("?????? ??????: ?????? ??????");
                    }
                    else if(g_skyState.equals("4")){
                        textView_skyState.setText("?????? ??????: ??????");
                    }
                     */

                }
                //showProgress(false);

            }

            @Override
            public void onFailure(Call<WeatherData> call, Throwable t) {
                Toast.makeText(LookBookActivity.this, "?????? api ?????? ??????", Toast.LENGTH_SHORT).show();
                Log.e("?????? api ?????? ??????", t.getMessage());
                //showProgress(false);
            }
        });

    }

    //????????? ?????? api
    public void startCurrentWeather(int nx, int ny){
        Date todayDate = new Date(); //?????? ??????
        //Date baseDateNotString = new Date(todayDate.getTime()+(1000*60*60*24*-1)); //?????? ??????, ?????? ?????? ????????? ?????? ?????????
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String dates=format.format(todayDate); //?????? ?????? yyyyMMdd?????? String
        //System.out.println(format.format(todayDate)); // 20210220

        SimpleDateFormat format2=new SimpleDateFormat("HH"); //??? ???
        SimpleDateFormat format3=new SimpleDateFormat("mm");// ??? ???
        String hours=format2.format(todayDate); //???????????? String, (ex 1, 23)
        String minutes=format3.format(todayDate); //???????????? String (ex 28)
        int hour=Integer.parseInt(hours); //???????????? ????????? ??????
        int min=Integer.parseInt(minutes); //???????????? ????????? ??????


        String serviceKey = "*";
        String pageNum="1";
        String s_nx = String.valueOf(nx);	//?????? ?????????
        String s_ny = String.valueOf(ny);	//?????? ?????????
        String type = "JSON"; //?????? xml, json ?????? ..
        String baseDate=format.format(todayDate); //???????????? ?????? ??????->?????? ??????!(??????????????? ??????)
        String baseTime; //???????????? ?????? ?????? (ex 2300)
        String numOfRows = "153"; //??? ????????? ?????? ???

        //??????????????? ????????????
        if(min<30 && hour !=0){
            hour--;
        }
        if(hour==0 && min<30){
            Date baseDateNotString = new Date(todayDate.getTime()+(1000*60*60*24*-1)); //?????? ??????
            baseDate=format.format(baseDateNotString);
            hour=23;
        }

        if(hour<9){
            g_fcstTime2="0"+hour+"00";
        }
        else{
            g_fcstTime2=hour+"00";
        }
        baseTime=g_fcstTime2;

        service_weather.getCurrentWeather(pageNum, numOfRows, type, baseDate, baseTime, s_nx, s_ny).enqueue(new Callback<CurrentWeatherData>() {
            @Override
            public void onResponse(Call<CurrentWeatherData> call, Response<CurrentWeatherData> response) {
                CurrentWeatherData result = response.body();
                if (response.body() != null) {
                    result = response.body();
                } else {
                    Log.e("??????", "?????? ????????????.");
                }
                // Toast.makeText(LoginActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                //Toast.makeText(getApplicationContext(), result.getMessage(), Toast.LENGTH_SHORT).show();

                if (response.isSuccessful()) {
                    Log.e("?????? ????????? ?????? api1", response.toString());
                    CurrentResponseData currentResponseData = result.getResponse();
                    Log.e("?????? ????????? ?????? api2", currentResponseData.toString());
                    CurrentBodyData currentBodyData = currentResponseData.getBody();
                    Log.e("?????? ????????? ?????? api3", currentBodyData.toString());
                    CurrentItemsData currentItemsData = currentBodyData.getItems();
                    Log.e("?????? ????????? ?????? api4", currentItemsData.toString());
                    CurrentItem[] currentItemList = currentItemsData.getItem();
                    /*
                    for(Item item:itemList){
                        result_list.add(item);
                    }
                     */

                    for (int i = 0; i < currentItemList.length; i++) {
                        CurrentItem weather = currentItemList[i];
                        String category = weather.getCategory();
                        String baseDate = weather.getBaseDate();
                        String obsrValue = weather.getObsrValue();

                        if (baseDate.equals(baseDate)) {//?????? ???????????? ????????????
                            if (category.equals("T1H")) {
                                //info = "?????? ????????????";
                                // DataValue = DataValue+" %";
                                g_currentTemp = obsrValue;
                                //System.out.println(info + ": " + lowTemp);
                            }
                            if (category.equals("PTY")) {
                                //info = "??? ????????????";
                                // DataValue = DataValue+" %";
                                g_currentState = obsrValue;
                                //System.out.println(info + ": " + highTemp);
                            }
                        }
                    } //for??? ???

                    textView_currentTemp.setText("?????? : "+g_currentTemp+"??");
                    if(g_currentState.equals("0")){
                        textView_currentState.setText("??????");
                        imageView_currentState.setImageDrawable(weather_sun);

                    }
                    if(g_currentState.equals("1")){
                        textView_currentState.setText("???");
                        imageView_currentState.setImageDrawable(weather_rain);
                    }
                    else if(g_currentState.equals("2")){
                        textView_currentState.setText("???/???");
                        imageView_currentState.setImageDrawable(weather_snow_and_rain);
                    }
                    else if(g_currentState.equals("3")){
                        textView_currentState.setText("???");
                        imageView_currentState.setImageDrawable(weather_snow);
                    }
                    else if(g_currentState.equals("4")){
                        textView_currentState.setText("?????????");
                        imageView_currentState.setImageDrawable(weather_shower);
                    }
                    else if(g_currentState.equals("5")){
                        textView_currentState.setText("?????????");
                        imageView_currentState.setImageDrawable(weather_shower);
                    }
                    else if(g_currentState.equals("6")){
                        textView_currentState.setText("?????????/?????????");
                        imageView_currentState.setImageDrawable(weather_snow_and_rain);
                    }
                    else if(g_currentState.equals("7")){
                        textView_currentState.setText("?????????");
                        imageView_currentState.setImageDrawable(weather_snow);
                    }

                    tempConvert();
                    //showProgress(false);
                }
            }

            @Override
            public void onFailure(Call<CurrentWeatherData> call, Throwable t) {
                Toast.makeText(LookBookActivity.this, "?????? api ?????? ??????", Toast.LENGTH_SHORT).show();
                Log.e("?????? api ?????? ??????", t.getMessage());
                //showProgress(false);
            }
        });

    }

    @Override
    public void onDestroy(){
        if(serverDialog !=null && serverDialog.isShowing()){
            serverDialog.dismiss();
        }
        super.onDestroy();
    }
}
