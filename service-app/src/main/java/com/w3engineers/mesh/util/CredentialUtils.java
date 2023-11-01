package com.w3engineers.mesh.util;
 
/*
============================================================================
Copyright (C) 2020 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

public class CredentialUtils {

    private static CredentialUtils credentialUtils = null;

    public static final int APP_TYPE = CredentialConstant.APP_TYPE_LIVE;

    public static CredentialUtils getInstance() {
        if (credentialUtils == null) {
            credentialUtils = new CredentialUtils();
        }
        return credentialUtils;
    }

    private interface CredentialConstant {
        int APP_TYPE_LIVE = 1;
        int APP_TYPE_STAGING = 2;
    }

    private interface LiveCredentials {
        String FILE_REPO_LINK = "https://config.telemesh.net/";
        String SIGNAL_SERVER_URL = "https://signal.telemesh.net";
        String CONFIGURATION_FILE_NAME = "Configuration.json";
        String CLIENT_INFO_FILE_NAME = "MeshControlConfig.json";

        String AUTH_USER_NAME = "superadminformeshlib";
        String AUTH_USER_PASS = "y6gr0ioz3mg9ford9dr";

        String PARSE_URL = "https://api.telemesh.net/mbaas";
        String PARSE_APP_ID = "csYNSQZMQgt3RJe4pge5ta1iW8atH9htMPTrENru8KqXQ9NK1c";
    }

    private interface StagingCredentials {
        String FILE_REPO_LINK = "https://dev-config.telemesh.net/";
        String SIGNAL_SERVER_URL = "https://dev-signal.telemesh.net";
        String CONFIGURATION_FILE_NAME = "Configuration-Staging.json";

        String AUTH_USER_NAME = "majorarif";
        String AUTH_USER_PASS = "major1234admin";

        String PARSE_URL = "https://dev-api.telemesh.net/mbaas";
        String PARSE_APP_ID = "PbsR8Pc8Un7f6ALQ8Q2LClIX7SQfpGDv3Qfml4O8sNnwaxh953";
    }

    public static String getFileRepoLink() {
        String value;
        if (APP_TYPE == CredentialConstant.APP_TYPE_LIVE) {
            value = LiveCredentials.FILE_REPO_LINK;
        } else {
            value = StagingCredentials.FILE_REPO_LINK;
        }
        return value;
    }

    public static String getSignalServerLink() {
        String value;
        if (APP_TYPE == CredentialConstant.APP_TYPE_LIVE) {
            value = LiveCredentials.SIGNAL_SERVER_URL;
        } else {
            value = StagingCredentials.SIGNAL_SERVER_URL;
        }
        return value;
    }

    public static String getConfigurationFile() {
        String value;
        if (APP_TYPE == CredentialConstant.APP_TYPE_LIVE) {
            value = LiveCredentials.CONFIGURATION_FILE_NAME;
        } else {
            value = StagingCredentials.CONFIGURATION_FILE_NAME;
        }
        return value;
    }

    public static String getClientInfoFile() {
        String value;
        if (APP_TYPE == CredentialConstant.APP_TYPE_LIVE) {
            value = LiveCredentials.CLIENT_INFO_FILE_NAME;
        } else {
            value = "";
        }
        return value;
    }

    public static String getAuthUserName() {
        String value;
        if (APP_TYPE == CredentialConstant.APP_TYPE_LIVE) {
            value = LiveCredentials.AUTH_USER_NAME;
        } else {
            value = StagingCredentials.AUTH_USER_NAME;
        }
        return value;
    }

    public static String getAuthPassword() {
        String value;
        if (APP_TYPE == CredentialConstant.APP_TYPE_LIVE) {
            value = LiveCredentials.AUTH_USER_PASS;
        } else {
            value = StagingCredentials.AUTH_USER_PASS;
        }
        return value;
    }

    public static String getParseUrl() {
        String value;
        if (APP_TYPE == CredentialConstant.APP_TYPE_LIVE) {
            value = LiveCredentials.PARSE_URL;
        } else {
            value = StagingCredentials.PARSE_URL;
        }
        return value;
    }

    public static String getParseAppId() {
        String value;
        if (APP_TYPE == CredentialConstant.APP_TYPE_LIVE) {
            value = LiveCredentials.PARSE_APP_ID;
        } else {
            value = StagingCredentials.PARSE_APP_ID;
        }
        return value;
    }
}
