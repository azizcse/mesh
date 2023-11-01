package com.w3engineers.purchase.dataplan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import com.w3engineers.eth.data.remote.EthereumService;
import com.w3engineers.mesh.MeshApp;
import com.w3engineers.mesh.TransportManagerX;
import com.w3engineers.mesh.TransportState;
import com.w3engineers.mesh.db.peers.PeersEntity;
import com.w3engineers.mesh.util.JsonBuilder;
import com.w3engineers.mesh.util.MeshLog;
import com.w3engineers.models.UserInfo;
import com.w3engineers.purchase.constants.DataPlanConstants;
import com.w3engineers.purchase.db.DatabaseService;
import com.w3engineers.purchase.db.purchase.Purchase;
import com.w3engineers.purchase.helper.PreferencesHelperDataplan;
import com.w3engineers.purchase.manager.PayController;
import com.w3engineers.purchase.manager.PurchaseConstants;
import com.w3engineers.purchase.manager.PurchaseManagerBuyer;
import com.w3engineers.purchase.manager.PurchaseManagerSeller;
import com.w3engineers.purchase.model.Seller;
import com.w3engineers.purchase.ui.dataplan.TestDataPlanActivity;
import com.w3engineers.purchase.util.EthereumServiceUtil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;


public class DataPlanManager implements PayController.PayControllerListenerForDataplanManager {

    private static DataPlanManager dataPlanManager;

    private PreferencesHelperDataplan preferencesHelperDataplan;
    private DataPlanListener dataPlanListener;
    private PayController payController;

    private String currentSellerId;
    private String currentSellerStatus;
    private List<Seller> finalSeller;
    private static String appTokenName;

    private BehaviorSubject<List<Seller>> sellers = BehaviorSubject.create();

    private DataPlanManager() {
        preferencesHelperDataplan = PreferencesHelperDataplan.on();
        payController = PayController.getInstance();
        payController.setPayControllerListenerForDataplanManager(this);
        finalSeller = new ArrayList<>();
    }

    public static DataPlanManager getInstance() {
        if (dataPlanManager == null) {
            dataPlanManager = new DataPlanManager();
        }
        return dataPlanManager;
    }

    public static void openActivity(Context context, String appToken) {
        appTokenName = appToken;
        Intent intent = new Intent(context, TestDataPlanActivity.class);
        intent.putExtra(TestDataPlanActivity.class.getSimpleName(), appToken);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        /*if(imageValue != 0) {
            byte[] image = getPicture(context, imageValue);
            intent.putExtra("picture", image);
        }*/
        context.startActivity(intent);
    }

    private static byte[] getPicture(Context context, int value) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), value);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        return b;
    }

    public static void resumeMessaging() {
        PurchaseManagerSeller.getInstance().resumeMessaging();
    }

    public void setDataPlanListener(DataPlanListener dataPlanListener) {
        this.dataPlanListener = dataPlanListener;

        if (getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_BUYER) {
            PurchaseManagerBuyer.getInstance().setDataPlanListener(dataPlanListener);
        } else if (getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_SELLER) {
            PurchaseManagerSeller.getInstance().setDataPlanListener(dataPlanListener);
        }
    }

//    public void closeMesh(int role) {
//        preferencesHelperDataplan.setDataPlanRole(role);
//        payController.getTransportManagerX().sendNewRoleToLocalConnectedUser(role);
//    }

    @Override
    public void onTransportInit(String nodeId, String publicKey, TransportState transportState, String msg) {
        roleSwitchCompleted();
    }

    @Override
    public void onUserChooseNewRole(String userId, int newRole, int previousRole) {
        if (newRole == DataPlanConstants.USER_ROLE.DATA_SELLER && getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_BUYER){
            payController.syncMyBuyer(userId);
        }

        if (previousRole == DataPlanConstants.USER_ROLE.DATA_SELLER && getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_BUYER){
            TransportManagerX.getInstance().makeInternetUsersOffline(userId);
        }

       /* if (newRole == DataPlanConstants.USER_ROLE.DATA_BUYER){
           //Nothing to do for now
        }*/

        //NOTE task moved to PayController class
       /* if (previousRole == DataPlanConstants.USER_ROLE.DATA_BUYER && getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_SELLER){
            payController.getTransportManagerX().getRemoteTransport().onBuyerDisconnected(userId);
        }*/

    }

    public interface DataPlanListener {

        void onConnectingWithSeller(String sellerAddress);

        void onPurchaseFailed(String sellerAddress, String msg);

        void onPurchaseSuccess(String sellerAddress, double purchasedData, long blockNumber);

        void onPurchaseClosing(String sellerAddress);

        void onPurchaseCloseFailed(String sellerAddress, String msg);

        void onPurchaseCloseSuccess(String sellerAddress);

        void showToastMessage(String msg);

        void onBalancedFinished(String sellerAddress, int remain);

        void onTopUpFailed(String sellerAddress, String msg);

        void onRoleSwitchCompleted();

        void onLimitFinished(boolean isFullyFinished, String message);
    }

    public void roleSwitch(int newRole, int previousRole) {

        try {
            MeshLog.v("UserMode dpm " + newRole);
            preferencesHelperDataplan.setDataPlanRole(newRole);
            //payController.getTransportManagerX().restart(newRole);
            payController.getTransportManagerX().sendNewRoleToLocalConnectedUser(newRole);

            if (newRole == DataPlanConstants.USER_ROLE.DATA_BUYER){
                payController.syncMyBuyer(null);
            }
            if (previousRole == DataPlanConstants.USER_ROLE.DATA_BUYER) {
                TransportManagerX.getInstance().makeInternetUsersOffline("all_sellers");
            }

            /*if (newRole == DataPlanConstants.USER_ROLE.DATA_SELLER){
                // Nothing to do here for now.
            }*/

            if (previousRole == DataPlanConstants.USER_ROLE.DATA_SELLER) {
                //disconnect all buyers from internet
                TransportManagerX.getInstance().getRemoteTransport().sendBuyersOffline();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void roleSwitchCompleted() {

        if (getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_BUYER) {

            PurchaseManagerBuyer.getInstance().setDataPlanListener(dataPlanListener);

            PurchaseManagerSeller.getInstance().setDataPlanListener(null);
            PurchaseManagerSeller.getInstance().destroyObject();


            PurchaseManagerBuyer.getInstance().setPayControllerListener();

        } else if (getDataPlanRole() == DataPlanConstants.USER_ROLE.DATA_SELLER) {
            PurchaseManagerSeller.getInstance().setDataPlanListener(dataPlanListener);

            PurchaseManagerBuyer.getInstance().setDataPlanListener(null);
            PurchaseManagerBuyer.getInstance().destroyObject();

            PurchaseManagerSeller.getInstance().setPayControllerListener();
        }
        if (dataPlanListener != null) {
            dataPlanListener.onRoleSwitchCompleted();
        }
    }


    private void setSellers(List<Seller> sellerList) {
        sellers.onNext(sellerList);
    }

    public Flowable<List<Seller>> getAllSellers() {
        return sellers.toFlowable(BackpressureStrategy.LATEST);
    }


    public int getDataPlanRole() {
        return preferencesHelperDataplan.getDataPlanRole();
    }

    public long getSellAmountData() {
        return preferencesHelperDataplan.getSellDataAmount();
    }

    public int getDataAmountMode() {
        return preferencesHelperDataplan.getDataAmountMode();
    }

    public long getSellFromDate() {
        return preferencesHelperDataplan.getSellFromDate();
    }

//    public long getSellToDate() {
//        return preferencesHelperDataplan.getSellToDate();
//    }

    public long getSellDataAmount() {
        return preferencesHelperDataplan.getSellDataAmount();
    }

    public void setSellFromDate(long fromDate) {
        preferencesHelperDataplan.setSellFromDate(fromDate);
    }

    public void setDataAmountMode(int mode) {
        preferencesHelperDataplan.setDataAmountMode(mode);
    }

    public void setSellDataAmount(Long sharedData) {
        preferencesHelperDataplan.setSellDataAmount(sharedData);
    }


    public long getRemainingData() {
        if (preferencesHelperDataplan.getDataAmountMode() == DataPlanConstants.DATA_MODE.LIMITED) {
            try {
                long sharedData = preferencesHelperDataplan.getSellDataAmount();
                long usedData = getUsedData(MeshApp.getContext(), preferencesHelperDataplan.getSellFromDate());
                return sharedData - usedData;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }

        return 0;
    }

//    public void setSellToDate(long toDate) {
//        preferencesHelperDataplan.setSellToDate(toDate);
//    }

    public long getUsedData(Context context, long fromDate) throws ExecutionException, InterruptedException {
        return DatabaseService.getInstance(context).getDataUsageByDate(fromDate);
    }

    public LiveData<Long> getDataUsage(Context context, long fromDate) {
        return DatabaseService.getInstance(context).getDatausageDao().getDataUsage(fromDate);
    }

    public void closeAllActiveChannel() {
        PurchaseManagerSeller.getInstance().closeAllActiveBuyerChannel();
    }

    public void initPurchase(double amount, String sellerId) {
        PurchaseManagerBuyer.getInstance().buyData(amount, sellerId);
    }

    public void closePurchase(String sellerId) {
        PurchaseManagerBuyer.getInstance().closePurchase(sellerId);
    }

    public void processAllSeller(Context context) {
        List<String> connectedSellers = payController.getTransportManagerX().getInternetSellers();
        if (connectedSellers != null)
            processAllSeller(context, connectedSellers);
    }

    public void setCurrentSeller(Context context, String sellerId, String currentSellerStatus) {
        this.currentSellerId = sellerId;
        this.currentSellerStatus = currentSellerStatus;

//        if (TextUtils.isEmpty(currentSellerId)) {
//            processAllSeller(context);
//        }
    }

    private void processAllSeller(Context context, List<String> connectedSellers) {

        try {

            DatabaseService databaseService = DatabaseService.getInstance(context);
            EthereumService ethService = EthereumServiceUtil.getInstance(context).getEthereumService();

            List<Purchase> purchaseSellers = databaseService.getMyActivePurchases(ethService.getAddress());

            List<Seller> connectedWithPurchasesClose = new ArrayList<>();
            List<Seller> connectedWithPurchasesOpen = new ArrayList<>();

            for (int i = purchaseSellers.size() - 1; i >= 0; i--) {
                Purchase purchase = purchaseSellers.get(i);

                if (!TextUtils.isEmpty(purchase.sellerAddress) && connectedSellers.contains(purchase.sellerAddress)) {

                    String sellerName = getSellersNameFromPeersEntity(payController
                            .getTransportManagerX().getUserNameByAddress(purchase.sellerAddress,
                                    appTokenName));

                    if (purchase.balance < purchase.deposit) {
                        connectedWithPurchasesClose.add(purchase.toSeller(DataPlanConstants.SELLER_LABEL.ONLINE_PURCHASED, sellerName));
                    } else {
                        connectedWithPurchasesOpen.add(purchase.toSeller(DataPlanConstants.SELLER_LABEL.ONLINE_NOT_PURCHASED, sellerName));
                    }

                    connectedSellers.remove(purchase.sellerAddress);
                    purchaseSellers.remove(purchase);
                }
            }

            finalSeller.clear();

            if (connectedSellers.size() > 0) {

                for (String sellerId : connectedSellers) {

                    String sellerName = getSellersNameFromPeersEntity(payController.getTransportManagerX().getUserNameByAddress(sellerId, appTokenName));

                    finalSeller.add(getSellerById(sellerId, DataPlanConstants.SELLER_LABEL.ONLINE_NOT_PURCHASED, sellerName));
                }

                // Add all top up seller in connected seller list
                finalSeller.addAll(connectedWithPurchasesOpen);
            }

            if (connectedWithPurchasesClose.size() > 0) {
                // Add all close action seller
                // in connected with existed seller list
                finalSeller.addAll(connectedWithPurchasesClose);
            }

            if (purchaseSellers.size() > 0) {

                for (Purchase purchase : purchaseSellers) {

                    // Ony added seller for close action seller
                    // Because those are not connected
                    if (purchase.balance < purchase.deposit) {

                        String sellerName = getSellersNameFromPeersEntity(payController
                                .getTransportManagerX().getUserNameByAddress(purchase.sellerAddress,
                                        appTokenName));

                        finalSeller.add(purchase.toSeller(DataPlanConstants.SELLER_LABEL.OFFLINE_PURCHASED, sellerName));
                    }
                }
            }

            if (currentSellerId != null) {
                for (int i = 0; i < finalSeller.size(); i++) {
                    Seller seller = finalSeller.get(i);

                    if (seller.getId().equals(currentSellerId)) {

//                        seller.setBtnEnabled(!currentSellerStatus.equals(PurchaseConstants.SELLERS_BTN_TEXT.PURCHASING) && !currentSellerStatus.equals(PurchaseConstants.SELLERS_BTN_TEXT.CLOSING));
                        seller.setBtnEnabled(!currentSellerStatus.equals(PurchaseConstants.SELLERS_BTN_TEXT.PURCHASING));

                        seller.setBtnText(currentSellerStatus);

                        finalSeller.set(i, seller);
                        break;
                    }
                }
            }

            setSellers(finalSeller);

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void precessDisconnectedSeller(Context context, String sellerId) {
        if (finalSeller == null || finalSeller.size() == 0)
            return;

        for (Seller seller : finalSeller) {
            if (seller.getId().equals(sellerId)) {
                List<String> connectedSellers = payController.getTransportManagerX().getInternetSellers();
                if (connectedSellers != null) {
                    connectedSellers.remove(sellerId);
                    processAllSeller(context, connectedSellers);
                }
                return;
            }
        }
    }

    private Seller getSellerById(String sellerId, int label, String name) {
        return new Seller()
                .setId(sellerId)
                .setName(name)
                .setPurchasedData(0)
                .setUsedData(0)
                .setBtnEnabled(true)
                .setLabel(label)
                .setBtnText(PurchaseConstants.SELLERS_BTN_TEXT.PURCHASE);
    }

    private String getSellersNameFromPeersEntity(PeersEntity peersEntity) {
        if (peersEntity != null) {
            UserInfo userInfo = JsonBuilder.on().formJson(peersEntity.getAppUserInfo());
            if (userInfo != null) {
                return userInfo.getUserName();
            }
        }
        return "Nearby Seller";
    }
}
