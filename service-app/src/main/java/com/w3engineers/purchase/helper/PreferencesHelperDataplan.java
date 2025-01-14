package com.w3engineers.purchase.helper;

import com.w3engineers.purchase.constants.DataPlanConstants;
import com.w3engineers.purchase.db.SharedPref;
import com.w3engineers.purchase.manager.PurchaseConstants;

public class PreferencesHelperDataplan {

    private static final String DATA_SHARE_MODE = "data_share_mode"; //Mesh User, Buyer, Seller
    private static final String DATA_AMOUNT_MODE = "data_amount_mode"; // Unlimited, Limited
    private static final String SELL_DATA_AMOUNT = "sell_data_amount";
    private static final String SELL_FROM_DATE = "sell_from_date";
    private static final String CHANNEL_CREATED_LATEST_BLOCK = "channel_create_latest_block";
    private static final String CHANNEL_CLOSED_LATEST_BLOCK = "channel_closed_latest_block";
    private static final String CHANNEL_TOPUP_LATEST_BLOCK = "channel_topup_latest_block";
    private static final String BALANCE_APPROVED_BLOCK = "balance_approved_block";
    private static final String CHANNEL_WITHDRAWN_BLOCK = "channel_withdrawn_block";
    private static final String TOKEN_MINTED_BLOCK = "token_minted_block";
    private static final String REQUESTED_FOR_ETHER_STATUS = "requested_for_ether";
    private static final String GIFT_ETHER_TRANX_HASH = "requested_ether_hash";
    private static final String GIFT_TOKEN_TRANX_HASH = "requested_token_hash";
    private static final String GIFT_ETHER_VALUE = "gift_ether_value";
    private static final String GIFT_TOKEN_VALUE = "gift_token_value";


    private static final String ETHER_REQUEST_TIME = "ether_request_time";
    private static final String CONFIG_VERSION = "CONFIG_VERSION";
    private static final String TOKEN_GUIDE_VERSION = "TOKEN_GUIDE_VERSION";
    private static final String PER_MB_TKN_VALUE = "PER_MB_TKN_VALUE";
    private static final String MAX_POINT_FOR_RMESH = "MAX_POINT_FOR_RMESH";
    private static final String RMESH_PER_POINT = "RMESH_PER_POINT";

    private static final String WALLET_RMESH_AVAILABLE = "WALLET_RMESH_AVAILABLE";
    private static final String RMESH_INFO_TEXT = "RMESH_INFO_TEXT";
    private static final String RMESH_OWNER_ADDRESS = "RMESH_OWNER_ADDRESS";
    private static final String MAINNET_NETWORK_TYPE = "MAINNET_NETWORK_TYPE";
    private static final String CONFIG_DATA = "CONFIG_DATA";
//    private static final String CURRENCY_MODE = "currency_mode";


    private PreferencesHelperDataplan() {

    }

    private static PreferencesHelperDataplan sInstance;

    synchronized public static PreferencesHelperDataplan on() {
        if (sInstance == null) {
            sInstance = new PreferencesHelperDataplan();
        }
        return sInstance;
    }

    public void setDataPlanRole(int value) {
        SharedPref.write(DATA_SHARE_MODE, value);
    }
    public int getDataPlanRole() {
      return SharedPref.readInt(DATA_SHARE_MODE, DataPlanConstants.USER_ROLE.MESH_USER);
    }

    public void setDataAmountMode(int value) {
        SharedPref.write(DATA_AMOUNT_MODE, value);
    }
    public int getDataAmountMode() {
        return SharedPref.readInt(DATA_AMOUNT_MODE, DataPlanConstants.DATA_MODE.UNLIMITED);
    }

    public void setSellDataAmount(long value) {
        SharedPref.write(SELL_DATA_AMOUNT, value);
    }
    public long getSellDataAmount() {
        long value = SharedPref.readLong(SELL_DATA_AMOUNT);
        return value;
    }

    public void setSellFromDate(long value) {
        SharedPref.write(SELL_FROM_DATE, value);
    }
    public long getSellFromDate() {
        return SharedPref.readLong(SELL_FROM_DATE);
    }

//    public void setSellToDate(long value) {
//        SharedPref.write(SELL_TO_DATE, value);
//    }
//    public long getSellToDate() {
//        return SharedPref.readLong(SELL_TO_DATE);
//    }

    public void setChannelCreatedBlock(long value){
        SharedPref.write(CHANNEL_CREATED_LATEST_BLOCK, value);
    }

    public long getChannelCreatedBlock(){
        return SharedPref.readLong(CHANNEL_CREATED_LATEST_BLOCK);
    }

    public void setChannelClosedBlock(long value){
        SharedPref.write(CHANNEL_CLOSED_LATEST_BLOCK, value);
    }

    public long getChannelClosedBlock(){
        return SharedPref.readLong(CHANNEL_CLOSED_LATEST_BLOCK);
    }

    public void setChannelTopupBlock(long value){
        SharedPref.write(CHANNEL_TOPUP_LATEST_BLOCK, value);
    }

    public long getChannelTopupBlock(){
        return SharedPref.readLong(CHANNEL_TOPUP_LATEST_BLOCK);
    }

    public void setBalanceApprovedBlock(long value){
        SharedPref.write(BALANCE_APPROVED_BLOCK, value);
    }

    public long getBalanceApprovedBlock(){
        return SharedPref.readLong(BALANCE_APPROVED_BLOCK);
    }

    public void setChannelWithdrawnBlock(long value){
        SharedPref.write(CHANNEL_WITHDRAWN_BLOCK, value);
    }

    public long getChannelWithdrawnBlock(){
        return SharedPref.readLong(CHANNEL_WITHDRAWN_BLOCK);
    }

    public void setTokenMintedBlock(long value){
        SharedPref.write(TOKEN_MINTED_BLOCK, value);
    }

    public long getTokenMintedBlock(){
        return SharedPref.readLong(TOKEN_MINTED_BLOCK);
    }

    public void setRequestedForEther(int requestState, int endpoint) {
        SharedPref.write(REQUESTED_FOR_ETHER_STATUS+endpoint, requestState);
    }

    public int getEtherRequestStatus(int endpoint) {
        return SharedPref.readInt(REQUESTED_FOR_ETHER_STATUS+endpoint, PurchaseConstants.GIFT_REQUEST_STATE.NOT_REQUESTED_YET);
    }


    public void setGiftEtherValue(double ethValue, int endpoint) {
        SharedPref.write(GIFT_ETHER_VALUE + endpoint, ethValue + "");
    }

    public double getGiftEtherValue(int endpoint) {
        return Double.parseDouble(SharedPref.read(GIFT_ETHER_VALUE + endpoint));
    }

    public void setGiftTokenValue(double tknValue, int endpoint) {
        SharedPref.write(GIFT_TOKEN_VALUE + endpoint, tknValue + "");
    }

    public double getGiftTokenValue(int endpoint) {
        return Double.parseDouble(SharedPref.read(GIFT_TOKEN_VALUE + endpoint));
    }


    public void setGiftEtherHash(String tranxHash, int endpoint) {
        SharedPref.write(GIFT_ETHER_TRANX_HASH+endpoint, tranxHash);
    }

    public String getGiftEtherHash(int endpoint) {
        return SharedPref.read(GIFT_ETHER_TRANX_HASH+endpoint);
    }

    public void setGiftTokenHash(String tranxHash, int endpoint) {
        SharedPref.write(GIFT_TOKEN_TRANX_HASH+endpoint, tranxHash);
    }

    public String getGiftTokenHash( int endpoint) {
        return SharedPref.read(GIFT_TOKEN_TRANX_HASH+endpoint);
    }

    public long getEtherRequestTimeStamp( int endpoint) {
        return SharedPref.readLong(ETHER_REQUEST_TIME+endpoint);
    }

    public void setEtherRequestTimeStamp(long time, int endpoint) {
        SharedPref.write(ETHER_REQUEST_TIME+endpoint, time);
    }

    public void setPerMbTokenValue(float value) {
        SharedPref.write(PER_MB_TKN_VALUE, "" + value);
    }

    public float getPerMbTokenValue() {
        return Float.valueOf(SharedPref.read(PER_MB_TKN_VALUE, "" + PurchaseConstants.PRICE_PER_MB));
    }

    public void setConfigVersion(int configVersion) {
        SharedPref.write(CONFIG_VERSION, configVersion);
    }

    public int getConfigVersion() {
        return SharedPref.readInt(CONFIG_VERSION, -1);
    }

    public void setConfigData(String configData) {
        SharedPref.write(CONFIG_DATA, configData);
    }

    public String getConfigData() {
        return SharedPref.read(CONFIG_DATA, null);
    }

    public void setTokenGuideVersion(int configVersion) {
        SharedPref.write(TOKEN_GUIDE_VERSION, configVersion);
    }

    public int getTokenGuideVersion() {
        return SharedPref.readInt(TOKEN_GUIDE_VERSION, -1);
    }

    public void setMaxPointForRmesh(long points) {
        SharedPref.write(MAX_POINT_FOR_RMESH, points);
    }

    public long getMaxPointForRmesh() {
        return SharedPref.readLong(MAX_POINT_FOR_RMESH);
    }

    public void setRmeshPerPoint(float value) {
        SharedPref.write(RMESH_PER_POINT, "" + value);
    }

    public float getRmeshPerPoint() {
        return Float.valueOf(SharedPref.read(RMESH_PER_POINT, "" + PurchaseConstants.RMESH_PER_POINT));
    }

    public void setWalletRmeshAvailable(boolean isEnable) {
        SharedPref.write(WALLET_RMESH_AVAILABLE, isEnable);
    }

    public boolean getWalletRmeshAvailable() {
        return SharedPref.readBoolean(WALLET_RMESH_AVAILABLE, false);
    }

    public void setRmeshInfoText(String infoText) {
        SharedPref.write(RMESH_INFO_TEXT, infoText);
    }

    public String getRmeshInfoText() {
        return SharedPref.read(RMESH_INFO_TEXT);
    }

    public void setRmeshOwnerAddress(String ownerAddress) {
        SharedPref.write(RMESH_OWNER_ADDRESS, ownerAddress);
    }

    public String getRmeshOwnerAddress() {
        return SharedPref.read(RMESH_OWNER_ADDRESS);
    }

    public void setMainnetNetworkType(int value) {
        SharedPref.write(MAINNET_NETWORK_TYPE, value);
    }

    public int getMainnetNetworkType() {
        return SharedPref.readInt(MAINNET_NETWORK_TYPE);
    }
}
