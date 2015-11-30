package com.topwise.sw.doublemm;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity {
    private final static String TAG_LOG = MainActivity.class.getName();
    Context mmContext = null;
    private static final String MAIN_DIR = "/sdcard/tencentmm";
    private static final String APP_DIR = MAIN_DIR + "/" + "app";
    private static final String APK_PATH = APP_DIR + "/com.tencent.mm-1/base.apk";
    private static final String APK_LIB_PATH = APP_DIR + "/com.tencent.mm-1/lib";
    private static final String DEX_PATH = MAIN_DIR + "/dalvik-cache/arm/data@app@com.tencent.mm-1@base.apk@classes.dex";
    private static final String



    Runnable getWeChatTask = new Runnable() {
        @Override
        public void run() {
            Context context = MainActivity.this;
            PackageManager pm = context.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> rInfos = pm.queryIntentActivities(intent, 0);

            ResolveInfo mmInfo = null;
            for (int i = 0; i < rInfos.size(); i++) {
                ResolveInfo rInfo = rInfos.get(i);
                if (TextUtils.equals(rInfo.activityInfo.packageName, "com.tencent.mm")) {
                    mmInfo = rInfo;
                    //return;
                }
            }

            if (mmInfo == null) {
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putParcelable("mm", mmInfo);
            Message msg = new Message();
            msg.what = 1;
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    };

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Bundle bundle = msg.getData();
                ResolveInfo mmInfo = bundle.getParcelable("mm");
                if (mmInfo != null) {
                    ApplicationInfo aInfo = mmInfo.activityInfo.applicationInfo;
                    try {
                        mmContext = MainActivity.this.createPackageContext(aInfo.packageName,
                                Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    DexClassLoader mmDexClassLoader = new DexClassLoader(APK_PATH, DEX_PATH, APK_LIB_PATH, mmContext.getClassLoader());
                    PathClassLoader mmPathClassLoader = new PathClassLoader(APK_PATH, mmDexClassLoader);

                    try {
                        mmPathClassLoader.loadClass("com.tencent.mm.ui.LauncherUI");
                        Class<?> aClass = mmPathClassLoader.loadClass("com.tencent.mm.app.MMApplication");
                        Application application = (Application) aClass.newInstance();
                        ComponentName name = new ComponentName(mmInfo.activityInfo.applicationInfo.packageName, mmInfo.activityInfo.name);
                        Intent intent = Intent.makeMainActivity(name);
                        //startActivity(intent);
                        application.startActivity(intent);


                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(getWeChatTask).start();
            }
        });

        //getSkinResourcesId("mm.tencent.mm");

    }



    /**
     * 取得对应包的所有资源的ID
     * 存在MAP中
     * @param packageName
     * @return
     */
    private Map<String,Map<String, Object>> getSkinResourcesId(String packageName)
    {
        Map<String, Object> temp =  null;
        Map<String,Map<String, Object>> resMap =new HashMap<String,Map<String,Object>>();
        try {
            //取得皮肤包中的R文件
            Class<?> rClass = mmContext.getClassLoader().loadClass(packageName+".ui.LauncherUI");
            //取得记录各种资源的ID的类
            Class<?>[] resClass =rClass.getClasses();
            String className,resourceName;
            int resourceId=0;
            for(int i=0;i<resClass.length;i++)
            {
                className = resClass[i].getName();
                //取得该类的资源
                Field field[] = resClass[i].getFields();
                for(int j =0;j < field.length; j++)
                {
                    resourceName = field[j].getName();
                    try {
                        resourceId = field[j].getInt(resourceName);
                    } catch (IllegalArgumentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if(resourceName!=null && !resourceName.equals(""))
                    {
                        temp =new HashMap<String, Object>();
                        temp.put(resourceName, resourceId);
                        Log.i("DDDDD", "className:" + className + "  resourceName:" + resourceName + "  " +
                                "resourceId:" + Integer.toHexString(resourceId));
                    }
                }
                //由于内部类的关系className应该是com.skin.R$layout的形式
                //截掉前面的包名和.R$以方便使用
                className = className.substring(packageName.length()+3);
                resMap.put(className, temp);
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resMap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
