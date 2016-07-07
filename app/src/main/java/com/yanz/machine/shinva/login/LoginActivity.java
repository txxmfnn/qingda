package com.yanz.machine.shinva.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.yanz.machine.shinva.MainActivity;
import com.yanz.machine.shinva.R;
import com.yanz.machine.shinva.application.MyApplication;
import com.yanz.machine.shinva.entity.BPerson;
import com.yanz.machine.shinva.update.UpdateManager;
import com.yanz.machine.shinva.util.ClickUtil;
import com.yanz.machine.shinva.util.HttpUtil;
import com.yanz.machine.shinva.util.JsonUtil;
import com.yanz.machine.shinva.util.StrUtil;

import java.io.IOException;

import cz.msebera.android.httpclient.Header;

public class LoginActivity extends Activity {



    private MyApplication application;

    String uri = "/login/check";
    private boolean isExit;
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };
    //获取sp内信息
    public String readIPANDPortInfo(){
        SharedPreferences sp = getSharedPreferences("IPPORT",0);
        String ip = sp.getString("ip", "");
        String port = sp.getString("port","");
        if (StrUtil.isNotEmpty(ip)&&StrUtil.isNotEmpty(port)){
            String[] conf = new String[2];
            conf[0] = ip;
            conf[1] = port;
            return ip+":"+port;
        }
        return null;
    }
    //存储登陆状态
    public void saveIPANDPortInfo(String ip,String port){
        SharedPreferences sp = getSharedPreferences("IPPORT",0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("ip",ip);
        editor.putString("port",port);
        editor.commit();
    }

    //定义文本框
    EditText etName,etPass;
    //定义按钮
    Button bnLogin,bnCancel;
    //验证登陆
    TextView tvWelcome;
    //记住密码
    CheckBox checkBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //获取界面内两个编辑框
        etName = (EditText) findViewById(R.id.userEditText);
        etPass = (EditText) findViewById(R.id.pwdEditText);
        //获取按钮
        bnLogin = (Button) findViewById(R.id.sLogin);
        bnCancel = (Button) findViewById(R.id.bnCancel);
        //验证登陆textview
        tvWelcome = (TextView) findViewById(R.id.tv_welcome);
        //记住密码
        checkBox = (CheckBox) findViewById(R.id.save);
        //调用读取用户信息功能
        String msg = readUserInfo();
        if (msg!=null){
            String[] info = msg.split(":");
            String name = info[0];
            String password = info[1];
            Boolean stat = Boolean.valueOf(info[2]);
            etName.setText(name);
            etPass.setText(password);
            checkBox.setChecked(stat);
        }
        //绑定取消事件
        //bnCancel.setOnClickListener(new HomeListener(this));
        //自动检查更新
        Toast.makeText(LoginActivity.this,"正在检查更新...",Toast.LENGTH_SHORT).show();
        new Thread(){
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                try {
                    UpdateManager um= new UpdateManager(LoginActivity.this);
                    um.checkUpdate();}
                 catch (IOException e){
                    e.printStackTrace();
                }
                Looper.loop();
            }
        }.start();
        bnLogin.setOnClickListener(new ClickUtil() {
            @Override
            protected void onNoDoubleClick(View view) {
                String name = etName.getText().toString();
                String password = etPass.getText().toString();
                final Boolean stat = checkBox.isChecked();
                if (stat){
                    saveUserInfo(name,password,stat);
                }else{
                    removeUserInfo();
                }
                String url;
                if (StrUtil.isNotEmpty(name)&&StrUtil.isNotEmpty(password)){
                    AsyncHttpClient client = new AsyncHttpClient();
                    url = HttpUtil.BASE_URL+uri;
                    RequestParams params = new RequestParams();
                    params.put("cpsCode",name);
                    params.put("password",password);
                    client.post(url, params, new TextHttpResponseHandler() {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                            Toast.makeText(LoginActivity.this,"网络未连接，请重试",Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String message) {
                            if (statusCode == 200){
                                try {
                                    message = new String(message.getBytes("iso8859-1"),"UTF-8");
                                    if (message.contains("true@@")){
                                        String[] msg = message.split("@@");
                                        String result = msg[1];
                                        BPerson bPerson ;
                                        bPerson = (BPerson) JsonUtil.jsonToObject(result,BPerson.class);
                                        MyApplication myApplication = (MyApplication) getApplication();
                                        Log.e("meng","放入全局变量中:"+bPerson.getCpsPassword());
                                        System.out.println("放入全局变量中:"+bPerson.getCpsPassword());
                                        myApplication.setUserInfo(bPerson);
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                    }else {
                                        Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_SHORT).show();
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }else {
                                Toast.makeText(LoginActivity.this,"网络连接失败",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else if ("".equals(etName.getText().toString())||"".equals(etPass.getText().toString())){
                    new AlertDialog.Builder(LoginActivity.this)
                            .setIcon(getResources().getDrawable(R.drawable.waring_icon))
                            .setTitle("警告")
                            .setMessage("用户名或密码不能为空")
                            .create().show();
                }
            }
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            exit();
            return false;
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }
    public void exit(){
        if (!isExit){
            isExit= true;
            Toast.makeText(getApplicationContext(),"再按一次退出程序",Toast.LENGTH_SHORT).show();
            mHandler.sendEmptyMessageDelayed(0,2000);
        }else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            System.exit(0);
        }
    }
    //登陆点击事件
    /*public void login_main(View v){
        String name = etName.getText().toString();
        String password = etPass.getText().toString();
        final Boolean stat = checkBox.isChecked();
        if (stat){
            saveUserInfo(name,password,stat);
        }else{
            removeUserInfo();
        }
        String url;
        if (StrUtil.isNotEmpty(name)&&StrUtil.isNotEmpty(password)){
            AsyncHttpClient client = new AsyncHttpClient();
            url = HttpUtil.BASE_URL+uri;
            RequestParams params = new RequestParams();
            params.put("cpsCode",name);
            params.put("password",password);
            client.post(url, params, new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String message, Throwable throwable) {
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, String message) {
                    if (statusCode == 200){
                        try {
                            message = new String(message.getBytes("iso8859-1"),"UTF-8");
                            if (message.contains("true@@")){
                                String[] msg = message.split("@@");
                                String result = msg[1];
                                BPerson bPerson ;
                                bPerson = (BPerson) JsonUtil.jsonToObject(result,BPerson.class);
                                MyApplication myApplication = (MyApplication) getApplication();
                                Log.e("meng","放入全局变量中:"+bPerson.getCpsPassword());
                                System.out.println("放入全局变量中:"+bPerson.getCpsPassword());
                                myApplication.setUserInfo(bPerson);
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            }else {
                                Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_SHORT).show();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }else {
                        Toast.makeText(LoginActivity.this,"网络连接失败",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else if ("".equals(etName.getText().toString())||"".equals(etPass.getText().toString())){
            new AlertDialog.Builder(LoginActivity.this)
                    .setIcon(getResources().getDrawable(R.drawable.waring_icon))
                    .setTitle("警告")
                    .setMessage("用户名或密码不能为空")
                    .create().show();
        }
    }*/
    //读取用户信息
    public String readUserInfo(){
        SharedPreferences sp = getSharedPreferences("USERINFO",0);
        String name = sp.getString("name", "");
        String password = sp.getString("password","");
        Boolean stat = sp.getBoolean("stat",false);
        if (StrUtil.isNotEmpty(name)&&StrUtil.isNotEmpty(password)){
            return name+":"+password+":"+stat;
        }
        return null;
    }
    //存储用户信息
    public void saveUserInfo(String name,String password,Boolean stat){
        SharedPreferences sp = getSharedPreferences("USERINFO",0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("name",name);
        editor.putString("password",password);
        editor.putBoolean("stat", stat);
        System.out.println("stat:"+stat);
        editor.commit();
    }
    //移除用户信息
    public void removeUserInfo(){
        SharedPreferences sp = getSharedPreferences("USERINFO",0);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove("name");
        editor.remove("password");
        editor.remove("stat");
        editor.commit();
    }

}
