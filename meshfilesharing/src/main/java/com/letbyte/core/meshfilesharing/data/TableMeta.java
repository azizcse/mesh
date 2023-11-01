package com.letbyte.core.meshfilesharing.data;

/**
 * ============================================================================
 * Copyright (C) 2020 W3 Engineers Ltd - All Rights Reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * <br>----------------------------------------------------------------------------
 * <br>Created by: Ahmed Mohmmad Ullah (Azim) on [2020-02-28 at 4:14 PM].
 * <br>----------------------------------------------------------------------------
 * <br>Project: meshsdk.
 * <br>Code Responsibility: <Purpose of code>
 * <br>----------------------------------------------------------------------------
 * <br>Edited by :
 * <br>1. <First Editor> on [2020-02-28 at 4:14 PM].
 * <br>2. <Second Editor>
 * <br>----------------------------------------------------------------------------
 * <br>Reviewed by :
 * <br>1. <First Reviewer> on [2020-02-28 at 4:14 PM].
 * <br>2. <Second Reviewer>
 * <br>============================================================================
 **/
public class TableMeta {

    public static final String DB_NAME = "meshfile.db";

    public interface TableNames {
        String FILE_PACKET = "File_Packet";
    }

    public interface ColNames {
        String FILE_TRANSFER_ID = "trans_id";
        String PEER_ADDRESS = "peer_address";
        String SOURCE_ADDRESS = "source_address";
        String FILE_SIZE = "size";
        String FILE_PATH = "path";
        String FILE_NAME = "name";
        String LAST_MODIFIED = "last_modified";
        String FILE_STATUS = "file_status";
        String RELATIVE_OFFSET = "r_offset";
        String APP_TOKEN = "app_token";
        String TRANSFERRED_BYTES = "transferred_bytes";
        String META_DATA = "meta_data";
    }

    public interface Serialization {
        String FILE_PATH = "fp";
        String APP_TOKEN = "at";
        String SENDER_ADDRESS = "sd";
        String IS_LAST_PACKET = "lp";
    }

}
