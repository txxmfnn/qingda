package com.yanz.machine.shinva.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
        bnCancel.setOnClickListener(new HomeListener(this));
        //更新方法
        Button updateBtn = (Button) findViewById(R.id.bn_check);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateManager manager = new UpdateManager(LoginActivity.this);
                //检查软件更新
                try {
                    manager.checkUpdate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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
    public void login_main(View v){
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
                                System.out.println("放入全局变量中:"+bPerson.getCpsPassword());
                                myApplication.setUserInfo(bPerson);
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            }else {
                                Toast.makeText(LoginActivity.this,message,Toast.LENGTH_SHORT).show();
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
                    .setIcon(getResources().getDrawable(R.drawable.waring_icon,null))
                    .setTitle("警告")
                    .setMessage("用户名或密码不能为空")
                    .create().show();
        }
    }

   /* public void loginPro(View view){
        //获取点击空间的id
        int id = view.getId();
        if (R.id.bnLogin == id){
            //进行登陆操作，获取用户名/密码
            final String name = etName.getText().toString();
            final String password = etPass.getText().toString();
            //进行前台验证
            if (TextUtils.isEmpty(name)||TextUtils.isEmpty(password)){
                Toast.makeText(this, "用户名或密码不能为空", Toast.LENGTH_LONG).show();
            }else {
                System.out.println("---------请求发送至服务器------");
                //开启子线程
                new Thread() {
                    @Override
                    public void run() {
                        loginByPost(name, password);
                    }
                }.start();
            }
        }
    }*/

    /*public void loginByPost(String name,String password){
        try{
            String spec = "http://192.168.1.66:8080/graduation/user/appLogin";
            URL url = new URL(spec);
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setReadTimeout(5000);
            urlConnection.setConnectTimeout(5000);
            String data = "name="+ URLEncoder.encode(name, "UTF-8")+"&password="+URLEncoder.encode(password,"UTF-8");
            urlConnection.setRequestProperty("Connection","keep-alive");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Content-Length",
                    String.valueOf(data.getBytes().length));
            urlConnection
                    .setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0");

            urlConnection.setDoOutput(true); // 发送POST请求必须设置允许输出
            urlConnection.setDoInput(true); // 发送POST请求必须设置允许输入
            OutputStream os = urlConnection.getOutputStream();
            os.write(data.getBytes());
            os.flush();
            System.out.println(urlConnection.getResponseCode());
            if (urlConnection.getResponseCode()==200){
                InputStream is = urlConnection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                byte buffer[] = new byte[1024];
                while ((len = is.read(buffer))!=-1){
                    baos.write(buffer,0,len);
                }
                is.close();
                baos.close();
                final String result = new String(baos.toByteArray());
                System.out.println("****"+result);
                LoginActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvWelcome.setText(result);
                    }
                });
                if (result.equals("loginsuccess")){
                    //启动main activity
                    Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                    intent.putExtra("hello",result);
                    startActivity(intent);
                    //结束该Activity
                    finish();
                }
            }else{
                System.out.println("连接失败");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
*/
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