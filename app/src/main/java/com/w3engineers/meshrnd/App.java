package com.w3engineers.meshrnd;

/*
 *  ****************************************************************************
 *  * Created by : Md. Azizul Islam on 1/14/2019 at 5:06 PM.
 *  * Email : azizul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md. Azizul Islam on 1/14/2019.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 */



import com.w3engineers.mesh.MeshApp;
import com.w3engineers.meshrnd.util.ObjectBox;

public class App extends MeshApp {

    @Override
    public void onCreate() {
        super.onCreate();
        ObjectBox.init(this);
    }
}
