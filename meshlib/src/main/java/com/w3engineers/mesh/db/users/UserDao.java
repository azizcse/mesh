package com.w3engineers.mesh.db.users;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.w3engineers.mesh.db.meta.TableMeta;

import java.util.List;

/**
 * User DAO
 */
@Dao
public interface UserDao {
    @Query("SELECT * FROM " + TableMeta.TableNames.USERS)
    List<UserEntity> getAll();

    @Query("SELECT * FROM users  WHERE  address != :id AND is_online = :status")
    List<UserEntity> getAllOnlineUser(String id, boolean status);


    @Query("SELECT * FROM "+ TableMeta.TableNames.USERS +" WHERE "+ TableMeta.ColumnNames.ADDRESS +"= :id")
    UserEntity getById(String id);

    @Update
    void update(UserEntity userEntity);

    @Query("UPDATE users SET is_online = :status WHERE address = :id")
    void updateOnlineStatusById(String id, boolean status);


    @Query("UPDATE users SET is_online = :status")
    void updateAllUsersOnlineStatus(boolean status);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(UserEntity... userEntities);

    @Query("SELECT COUNT(*) FROM users where address != :id")
    LiveData<Integer> getCount(String id);

    @Delete
    void delete(UserEntity userEntity);

    @Query("SELECT " + TableMeta.ColumnNames.PUBLIC_KEY +" FROM "+ TableMeta.TableNames.USERS +" WHERE "+ TableMeta.ColumnNames.ADDRESS +"= :id")
    String getPublicKeyById(String id);
}
