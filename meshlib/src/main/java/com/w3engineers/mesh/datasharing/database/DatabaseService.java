package com.w3engineers.mesh.datasharing.database;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room.Room;

import com.w3engineers.mesh.BuildConfig;
import com.w3engineers.mesh.datasharing.database.message.Message;
import com.w3engineers.mesh.datasharing.helper.CommonUtil;
import com.w3engineers.mesh.db.meta.TableMeta;
import com.w3engineers.mesh.db.peers.PeersEntity;
import com.w3engineers.mesh.db.users.UserEntity;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Database service
 */
public class DatabaseService {
    private static DatabaseService databaseService;
    private ExecutorService executor;
    private AppDatabase db;
    private Context context;


    DatabaseService(Context context) {
        this.context = context;
        executor = Executors.newFixedThreadPool(1);
        if (BuildConfig.DEBUG) {
            db = Room.databaseBuilder(this.context, AppDatabase.class, TableMeta.DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        } else {
            if (!CommonUtil.isEmulator()) {
                db = Room.databaseBuilder(this.context, AppDatabase.class, TableMeta.DB_NAME)
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
    }

    public static DatabaseService getInstance(Context context) {
        if (databaseService == null) {
            databaseService = new DatabaseService(context);
        }
        return databaseService;
    }

    public void insertMessage(Message message) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    db.messageDao().insertAll(message);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }

    public void deleteMessage(String messageId) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    db.messageDao().deleteByMessageById(messageId);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }

    public Message getMessageById(String msg_id) throws ExecutionException, InterruptedException {
        Future<Message> future = executor.submit(new Callable() {
            @Override
            public Message call() {
                Message message = null;
                try {
                    message = db.messageDao().getMessageById(msg_id);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return message;
            }
        });
        return future.get();
    }

    public List<Message> getPendingMessage(String receiverId) throws ExecutionException, InterruptedException {
        Future<List<Message>> future = executor.submit(new Callable() {
            @Override
            public List<Message> call() {
                List<Message> messageList = null;
                try {
                    messageList = db.messageDao().getPendingMessage(receiverId, false);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return messageList;
            }
        });
        return future.get();

    }

    public List<Message> getInComingPendingMessage(String appToken) throws ExecutionException, InterruptedException {
        Future<List<Message>> future = executor.submit(new Callable() {
            @Override
            public List<Message> call() {
                List<Message> messageList = null;
                try {
                    messageList = db.messageDao().getAllIncomingPendingMessage(true, appToken);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return messageList;
            }
        });
        return future.get();

    }

    /*public List<UserEntity> getAllOnlineUsers(String selfId) throws ExecutionException, InterruptedException {

        Future<List<UserEntity>> future = executor.submit(new Callable() {
            @Override
            public List<UserEntity> call() {
                List<UserEntity> userList = null;
                try {
                    userList = db.getUserDao().getAllOnlineUser(selfId, true);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return userList;
            }
        });
        return future.get();
    }*/

    /*public void updateOnlineStatus(String userId, boolean status) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    db.getUserDao().updateOnlineStatusById(userId, status);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }*/

    /*public void updateAllUsersOnlineStatus(boolean status) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    db.getUserDao().updateAllUsersOnlineStatus(status);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }*/

    public int insertUserEntity(UserEntity userEntity) throws ExecutionException, InterruptedException {

        Future<Integer> future = executor.submit(new Callable() {
            @Override
            public Integer call() {
                long[] rid = null;
                try {
                    long currentTime = System.currentTimeMillis();
                    userEntity.setTimeCreated(currentTime);
                    userEntity.setTimeModified(currentTime);

                    rid = db.getUserDao().insertAll(userEntity);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return (int) rid[0];
            }
        });
        return future.get().intValue();
    }

    /*public void updateUserEntity(UserEntity userEntity) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    long currentTime = System.currentTimeMillis();
                    userEntity.setTimeModified(currentTime);

                    db.getUserDao().update(userEntity);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }*/

    public UserEntity getUserById(String userId) throws ExecutionException, InterruptedException {
        Future<UserEntity> future = executor.submit(new Callable() {
            @Override
            public UserEntity call() {
                UserEntity userEntity = null;
                try {
                    userEntity = db.getUserDao().getById(userId);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return userEntity;
            }
        });
        return future.get();
    }

    public String getPublicKeyById(String userId) throws ExecutionException, InterruptedException {
        Future<String> future = executor.submit(new Callable() {
            @Override
            public String call() {
                String publicKey = null;
                try {
                    publicKey = db.getUserDao().getPublicKeyById(userId);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return publicKey;
            }
        });
        return future.get();
    }

    /*public LiveData<Integer> getCount(String userId) {
        return db.getUserDao().getCount(userId);
    }*/

    public int insertPeersEntity(PeersEntity peersEntity) throws ExecutionException, InterruptedException {

        Future<Integer> future = executor.submit(new Callable() {
            @Override
            public Integer call() {
                long[] rid = null;
                try {
                    long currentTime = System.currentTimeMillis();
                    peersEntity.setTimeModified(currentTime);

                    rid = db.getPeersDao().insertAll(peersEntity);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return (int) rid[0];
            }
        });
        return future.get().intValue();
    }

    public PeersEntity getPeersById(String userId, String token) throws ExecutionException, InterruptedException {
        Future<PeersEntity> future = executor.submit(new Callable() {
            @Override
            public PeersEntity call() {
                PeersEntity peersEntity = null;
                try {
                    peersEntity = db.getPeersDao().getPeersByIdAndToken(userId, token);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return peersEntity;
            }
        });
        return future.get();
    }

    public List<PeersEntity> getSelfPeers() throws ExecutionException, InterruptedException {
        Future<List<PeersEntity>> future = executor.submit(new Callable() {
            @Override
            public List<PeersEntity> call() {
                List<PeersEntity> peersEntites = null;
                try {
                    peersEntites = db.getPeersDao().getSelfPeers(true);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return peersEntites;
            }
        });
        return future.get();
    }

    public void deleteOldSelfInfo(String walletAddress, String appToken) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    db.getPeersDao().deleteOldSelfInfo(walletAddress, appToken);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }

    public List<PeersEntity> getAllOnlinePeers(String selfId, String appToken) throws ExecutionException, InterruptedException {

        Future<List<PeersEntity>> future = executor.submit(new Callable() {
            @Override
            public List<PeersEntity> call() {
                List<PeersEntity> peersEntities = null;
                try {
                    peersEntities = db.getPeersDao().getAllOnlinePeers(selfId, true, appToken);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return peersEntities;
            }
        });
        return future.get();
    }

    public void updatePeerStatus(String userId, boolean status) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    db.getPeersDao().updateOnlineStatusById(userId, status);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }

    public void updateAllPeersOnlineStatus(boolean status) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    db.getPeersDao().updateAllPeersOnlineStatus(status);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }

    public String getPeersPublicKey(String userId) throws ExecutionException, InterruptedException {
        Future<String> future = executor.submit(new Callable() {
            @Override
            public String call() {
                String publicKey = null;
                try {
                    publicKey = db.getPeersDao().getPeersPublicKey(userId);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return publicKey;
            }
        });
        return future.get();
    }

    public void updatePeerAppVersion(String userId, String appToken, int appVersion) {
        executor.submit(new Callable() {
            @Override
            public Integer call() {
                try {
                    db.getPeersDao().updatePeersAppVersion(userId, appToken, appVersion);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return 0;
            }
        });
    }

    public LiveData<Integer> getPeersCount(String userId) {
        return db.getPeersDao().getPeersCount(userId);
    }

    public PeersEntity getFirstPeersById(String userId) throws ExecutionException, InterruptedException {
        Future<PeersEntity> future = executor.submit(new Callable() {
            @Override
            public PeersEntity call() {
                PeersEntity peersEntity = null;
                try {
                    peersEntity = db.getPeersDao().getFirstPeerById(userId);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return peersEntity;
            }
        });
        return future.get();
    }

    public List<String> getOnlineUserIds(String userId, boolean status) throws ExecutionException, InterruptedException {
        Future<List<String>> future = executor.submit(new Callable() {
            @Override
            public List<String> call() {
                List<String> onlineUserIds = null;
                try {
                    onlineUserIds = db.getPeersDao().getOnlineUserAddress(userId, status);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return onlineUserIds;
            }
        });
        return future.get();
    }

    public List<String> getValidUserIds(List<String> userIds) throws ExecutionException, InterruptedException {
        Future<List<String>> future = executor.submit(new Callable() {
            @Override
            public List<String> call() {
                List<String> validUsers = null;
                try {
                    validUsers = db.getPeersDao().getValidUserAddress(userIds);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return validUsers;
            }
        });
        return future.get();
    }

    public List<PeersEntity> getAllPeerById(String userId) throws ExecutionException, InterruptedException {
        Future<List<PeersEntity>> future = executor.submit(new Callable() {
            @Override
            public List<PeersEntity> call() {
                List<PeersEntity> validUsers = null;
                try {
                    validUsers = db.getPeersDao().getAllPeerById(userId);
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                return validUsers;
            }
        });
        return future.get();
    }

}
