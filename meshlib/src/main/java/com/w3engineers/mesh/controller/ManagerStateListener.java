package com.w3engineers.mesh.controller;

import com.w3engineers.mesh.libmeshx.wifid.APCredential;

public interface ManagerStateListener {
    void  onSoftApCreated(APCredential apCredential);
}
