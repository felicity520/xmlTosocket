package ryd.gyy.xmltosocket;

import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    //服务端的IP是：192.168.1.5（这里用我的电脑当服务器）
    //手机当做是客户端，端口号可以任意设定，双方一致就OK
    private static final String HOST = "192.168.1.5";
    private static final int PORT = 12333;
    private OutputStream outStream;
    private Socket socket = null;
    private BufferedReader in = null;
    private PrintWriter out = null;


    //上传到服务器的线程
    private boolean exit0 = false;
    private socketThread msocketThread;


    //创建一个map来存储测试名称和结果，使用key value来保存测试项和测试结果
    Map<String, String> map = new TreeMap<String, String>();

    private SharedPreferencesHelper sharedPreferencesHelper;

    String[] names;

    private List<ryd> smsLists;
    ryd sms;

    public static final int TEST_RESULT_NONE = 0; //没有测试
    public static final int TEST_RESULT_FAIL = 2; //测试失败
    public static final int TEST_RESULT_SUCC = 1; //测试成功

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_data();
    }

    private void init_data() {
        Resources res = getResources();
        names = res.getStringArray(R.array.listview_item_name);

        sharedPreferencesHelper = new SharedPreferencesHelper(this);
        //在这里模拟测试结果，camera测试成功，mic测试失败
        sharedPreferencesHelper.put("MIC测试", TEST_RESULT_FAIL);
        sharedPreferencesHelper.put("Camera输入测试", TEST_RESULT_SUCC);

    }


    @Override
    protected void onResume() {
        super.onResume();

        init_xml();//初始化xml数据
        get_testresult();//动态获取测试项和测试结果
        get_xml();
//        get_xml2();
        start_thread();//在onResume中获取到数据之后再开启线程上传数据
    }


    /**
     * 初始化xml数据
     */
    private void init_xml() {
        //[1]初始化我们要备份的数据
        smsLists = new ArrayList<ryd>();
        sms = new ryd();
        //在这里可以获取你想设置的数据，这里举例设置sn
        String serial = Build.SERIAL;//获取SN
        sms.setPro_name("8802");
        sms.setPro_SN(serial);
        sms.setPro_Mac("0c:12:05:01:25:25");
        sms.setTest_mode("pqc");
    }


    /**
     * 获取测试结果，补充test部分
     */
    private void get_testresult() {
        for (String str : names) {
            Log.i("yyy", "str------------: " + str);
            int result = (int) sharedPreferencesHelper.get(str, TEST_RESULT_NONE);

            //在这里可以将测试的中文名称改成英文名称，因为接收中文很可能导致报错，因为数据不匹配，最好用英文
            switch (str.trim()) {
                case "MIC测试":
                    str = "EarphoneTest";
                    break;
                case "Camera输入测试":
                    str = "Camera Test";
                    break;
            }

            if (result == TEST_RESULT_SUCC) {
                map.put(str, "Success");
            } else if (result == TEST_RESULT_NONE) {
                map.put(str, "Fail");
            } else if (result == TEST_RESULT_FAIL) {
                map.put(str, "Fail");
            }
        }

        //放入测试项
        smsLists.add(sms);
    }


    /**
     * 通过StringBuffer直接以追加字符串的方式生成XML文件
     */
    private void get_xml() {
        //[1]创建sb对象
        StringBuffer sb = new StringBuffer();

        //[2]开始组拼xml文件头
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        //[3]开始组拼xml根节点
        sb.append("<ryd>");
        //[4]开始组拼sms节点
        for (ryd sms : smsLists) {
            //sb.append("<sms>");

            //[5]开始组拼pro_name节点
            sb.append("<pro_name>");
            sb.append(sms.getPro_name());
            sb.append("</pro_name>");

            //[6]开始组拼pro_SN节点
            sb.append("<pro_SN>");
            sb.append(sms.getPro_SN());
            sb.append("</pro_SN>");

            //[7]开始组拼pro_Mac节点
            sb.append("<pro_Mac>");
            sb.append(sms.getPro_Mac());
            sb.append("</pro_Mac>");


            //[7]开始组拼test_mode节点
            sb.append("<test_mode>");
            sb.append(sms.getTest_mode());
            sb.append("</test_mode>");

            //遍历出map的值
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                //[7]开始组拼<test>节点
                sb.append("<test>");

                sb.append("<name>");
                sb.append(key);
                sb.append("</name>");

                sb.append("<result>");
                sb.append(value);
                sb.append("</result>");

                sb.append("<errmsg>");
                if (value.contains("Fail")) {
                    sms.setErrmsg("Fail");
                } else {
                    sms.setErrmsg("");
                }
                sb.append(sms.getErrmsg());
                sb.append("</errmsg>");

                sb.append("</test>");
            }
        }

        sb.append("</ryd>");
        //[8]把数据保存到sd卡中
        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath(), "MesXml.xml");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.close();//关闭流
            Log.i("yyy", "文件已经保存到sd卡: ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 利用XmlSerializer类来生成XML文件
     */
    private void get_xml2() {
        File file = new File(Environment.getExternalStorageDirectory(),
                "ryd.xml");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            // 获取xml序列化器
            XmlSerializer xs = Xml.newSerializer();
            xs.setOutput(fos, "utf-8");
            //生成xml头
            xs.startDocument("utf-8", true);
            xs.startTag(null, "ryd");
            for (ryd sms : smsLists) {
                //设置pro_name
                xs.startTag(null, "pro_name");
                xs.text(sms.getPro_name());
                xs.endTag(null, "pro_name");

                //设置sn
                xs.startTag(null, "pro_SN");
                xs.text(sms.getPro_SN());
                xs.endTag(null, "pro_SN");

                //设置mac
                xs.startTag(null, "pro_Mac");
                xs.text(sms.getPro_Mac());
                xs.endTag(null, "pro_Mac");

                //设置test_mode
                xs.startTag(null, "test_mode");
                xs.text(sms.getTest_mode());
                xs.endTag(null, "test_mode");
            }

            //遍历出map的值
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                xs.startTag(null, "test");

                //设置key
                xs.startTag(null, "name");
                xs.text(key);
                xs.endTag(null, "name");

                //设置result
                xs.startTag(null, "result");
                xs.text(value);
                xs.endTag(null, "result");

                //设置errmsg:这里失败的测试项errmsg就写fail，成功的为空
                xs.startTag(null, "errmsg");
                if (value.contains("Fail")) {
                    xs.text("Fail");
                } else {
                    xs.text("");
                }
                xs.endTag(null, "errmsg");

                xs.endTag(null, "test");
            }
            xs.endTag(null, "ryd");
            //生成xml头
            xs.endDocument();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void start_thread() {
        //开始线程
        exit0 = false;
        msocketThread = new socketThread();
        msocketThread.start();
    }


    /***
     * 线程
     */
    public class socketThread extends Thread {
        public void run() {
            while (!exit0) {
                try {
                    Log.i("yyy", "进入socketThread----------: ");
                    // 开启与服务器的socket连接
                    socket = new Socket(HOST, PORT);
                    // in:读入  out:写出
                    in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));

                    //获取输出流
                    outStream = socket.getOutputStream();
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outStream)), true);


                    try {
                        //File file = new File("F:/1/MesXml.xml");//java程序：创建了一个对象 ==> 描述了一个路径
                        //System.out.println(file);//D:\1\1.txt
                        File file = new File(Environment.getExternalStorageDirectory().getPath(), "MesXml.xml");
                        //创建SAXReader,读取指定文件
                        System.out.println(file);//D:\1\1.txt
                        Log.i("yyy", "file:-----------: " + file);
                        if (file.exists()) {
                            SAXReader reader = new SAXReader();
                            Document doc = null;
                            doc = reader.read(file);
                            //创建XML输出流
                            XMLWriter writer = new XMLWriter();
                            writer.setOutputStream(outStream);
                            writer.write(doc);
                        } else {
                            Log.i("yyy", "MesXml文件还没有生成: ");
                        }


                    } catch (DocumentException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (socket != null) {
                            msocketThread.stopDownload();
                            try {
                                out.flush();
                                out.close();
                                System.out.println("socket已关闭---");
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }
        }

        public void stopDownload() {
            Log.i("yyy", "线程已经停止------------: ");
            exit0 = true;
        }
    }


}
