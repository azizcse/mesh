package com.letbyte.core.meshfilesharing.comm.bt;

public interface BTFileLinkListener {

    BTFileLink getBTFileLink();
    void onBTLink(BTFileLink btFileLink);

}
