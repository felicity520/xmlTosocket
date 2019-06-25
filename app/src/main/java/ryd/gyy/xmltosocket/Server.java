package ryd.gyy.xmltosocket;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * 服务端
 */
public class Server {
    private ServerSocket server;

    public Server() throws IOException {
        server = new ServerSocket(12333);
    }

    public void start() {
        try {
            //监听客户端,建立连接
            System.out.println("等待连接..........");
            Socket socket = server.accept();
            System.out.println("服务端连接成功!");
            //获取输入流   写出  读入
            InputStream in = socket.getInputStream();
            //创建SAXReader,读取指定文件
            SAXReader reader = new SAXReader();
            Document doc = reader.read(in);
            //获取根节点
            Element root = doc.getRootElement();
            getNodes(root);//遍历根节点下的所有节点，也就是整个xml文件的所有节点
            System.out.println("解析完毕!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                    System.out.println("服务端已关闭!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 从指定节点开始,递归遍历所有子节点
     */
    public void getNodes(Element node) {
        System.out.println("--------------------");

        //当前节点的名称、文本内容和属性
        System.out.println("当前节点名称：" + node.getName());//当前节点名称
        System.out.println("当前节点的内容：" + node.getTextTrim());//当前节点内容
        //递归遍历当前节点所有的子节点
        List<Element> listElement = node.elements();//所有一级子节点的list
        for (Element e : listElement) {//遍历所有一级子节点
            this.getNodes(e);//递归
        }
    }


    public static void main(String[] args) {
        try {
            Server s = new Server();
            s.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("初始化服务端失败!");
        }
    }
}
