package com.w3engineers.mesh.httpservices;

import com.w3engineers.ext.strom.util.Text;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.IHTTPSession;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.NanoHTTPD;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.request.Method;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Response;
import com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Status;
import com.w3engineers.mesh.util.MeshLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

import static com.w3engineers.mesh.httpservices.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

/**
 * NANO http server manager
 */
public class NanoHTTPServer extends NanoHTTPD {
    private HttpDataListener httpDataListener;
    private static HttpDataListenerFromInternet mHttpDataListenerFromInternet;
    public int myPort;
    private boolean isTcpSocketRunning;

    public interface HttpDataListener {
        void receivedData(String userId, String ipAddress, String data);
    }

    public interface HttpDataListenerFromInternet {
        void receivedInternetData(String ipAddress, String data, String immediateSender);
    }

    public void setHttpDataListener(HttpDataListener httpDataListener) {
        this.httpDataListener = httpDataListener;
    }

    public static void setHttpDataListenerForInternet(HttpDataListenerFromInternet httpDataListenerFromInternet) {
        mHttpDataListenerFromInternet = httpDataListenerFromInternet;
    }

    public NanoHTTPServer(String host, int port) {
        super(host, port);
        myPort = port;
    }

    public void start() throws IOException {
        super.start();
        isTcpSocketRunning = true;
        startTcpServerSocket(myPort);
        MeshLog.e("Http server started with port on : " + myPort);
        //  mServerStartedListener.serverStarted();
        //System.out.println("\nRunning! Point your browers to http://localhost:8080/ \n");

    }

    @Override
    public void stop() {
        super.stop();
        isTcpSocketRunning = false;
        if(serverSocket != null){
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        final String html = "<html><head><body><h1>Hello, Telemesh</h1></body></head></html>";
        Method method = session.getMethod();
        if (method == Method.POST) {
            try {
                session.parseBody(new HashMap<String, String>());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ResponseException e) {
                e.printStackTrace();
            }
            MeshLog.e("Data Received" + session.getParameters().toString());

            //wifi-direct and AdHoc data storage
            List<String> localData = session.getParameters().get("data");


            //internet data storage
            List<String> internetData = session.getParameters().get("internet_data");

            MeshLog.e("MeshTesst   inside nano  " + session.getMethod().toString() + "   " + internetData);
            if (localData != null && httpDataListener != null) {
                String uri = session.getUri();
                if (Text.isNotEmpty(uri)) {
                    switch (uri) {
                        case URI_TEXT:
                            String usrId = session.getHeaders().get("id");
                            httpDataListener.receivedData(usrId, session.getRemoteIpAddress(), localData.get(0));
                            return newFixedLengthResponse(Status.OK, MIME_HTML, html);
                    }
                }
            } else if (internetData != null && mHttpDataListenerFromInternet != null) {
                String usrId = session.getHeaders().get("id");
                mHttpDataListenerFromInternet.receivedInternetData(session.getRemoteIpAddress(), internetData.get(0), usrId);
                //Log.d()
                return newFixedLengthResponse(Status.OK, MIME_HTML, html);
            }
            return null;
        } else {
            return newFixedLengthResponse(Status.NOT_FOUND, MIME_HTML, "Request not supported");
        }
    }

    private ServerSocket serverSocket;

    private void startTcpServerSocket(int port) {
        final int newTcpPort = port - 2;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(newTcpPort);
                    serverSocket.setReuseAddress(true);
                    if (!serverSocket.isBound()) {
                        serverSocket.bind(new InetSocketAddress(newTcpPort));
                    }
                    while (isTcpSocketRunning) {
                        MeshLog.v("[Raw-socket] Raw socket is running");
                        Socket socket = serverSocket.accept();
                        byte[] data = getInputStreamByteArray(socket.getInputStream());
                        String ipAddress = socket.getInetAddress().getHostAddress();
                        MeshLog.v("[Raw-socket] Raw socket received: " + new String(data));
                        httpDataListener.receivedData("", ipAddress, new String(data));
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (serverSocket != null) {
                            serverSocket.close();
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        }).start();

    }

    public byte[] getInputStreamByteArray(InputStream input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;

        try {
            while ((len = input.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return (baos.toByteArray());
    }
}
