package com.letbyte.core.meshfilesharing.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.google.gson.annotations.SerializedName;
import com.w3engineers.mesh.util.AddressUtil;
import com.w3engineers.mesh.util.gson.Exclude;

/**
 * To store file info and maintain file data flexibly
 */
@Entity(tableName = TableMeta.TableNames.FILE_PACKET, primaryKeys =
        {TableMeta.ColNames.SOURCE_ADDRESS, TableMeta.ColNames.FILE_TRANSFER_ID})
public class FilePacket extends BaseMessage {

    /**
     * Generated identity of a file transaction from sender/source
     */
    @SerializedName("tid")
    @ColumnInfo(name = TableMeta.ColNames.FILE_TRANSFER_ID)
    public long mFileId;

    @NonNull
    @Exclude
    @ColumnInfo(name = TableMeta.ColNames.PEER_ADDRESS)
    public String mPeerAddress;

    @NonNull
    // @Exclude
    @SerializedName("sc")
    @ColumnInfo(name = TableMeta.ColNames.SOURCE_ADDRESS)
    public String mSourceAddress;

    //todo FilePath will maintain only director part
    /**
     * Local storing path. Source path for sender
     */
    @SerializedName("fp")
    @ColumnInfo(name = TableMeta.ColNames.FILE_PATH)
    public String mSelfFullFilePath;

    /**
     * Sender's URI so that we can request file server with specific uri
     */
    @Exclude
    @Ignore
    public String mPeerFullFilePath;

    @SerializedName("fn")
    @ColumnInfo(name = TableMeta.ColNames.FILE_NAME)
    public String mFileName;

    /**
     * Size of original file. This is necessary so that receiver can track packet count and file
     * receiving finished or not. Only transacted for initial {@link FileMessage}
     */
    @Exclude
    @ColumnInfo(name = TableMeta.ColNames.FILE_SIZE)
    public long mFileSize;
    /**
     * packet data content
     */
    @SerializedName("d")
    @Ignore
    public byte[] mData;

    /**
     * packet data content
     */
    @Exclude
    @ColumnInfo(name = TableMeta.ColNames.LAST_MODIFIED)
    public long mLastModified = System.currentTimeMillis();

    @Exclude
    @ColumnInfo(name = TableMeta.ColNames.FILE_STATUS)
    public int fileStatus;

    /**
     * Is current packet last packet or not
     */
    @SerializedName(TableMeta.Serialization.IS_LAST_PACKET)
    public boolean mIsLastPacket;

    /**
     * If any intermediate node start forwarding data from another source to a different destination
     * and it has not the starting of that file. In that case this offset adjust the packet number
     * with source and destination.
     * For source and destination and in best case it maintains reset value.
     * This value set when the intermediate node receives a file request and it founds that it had
     * no earlier entry for this particular file id.
     */
    @Exclude
    @ColumnInfo(name = TableMeta.ColNames.RELATIVE_OFFSET)
    public long mRelativeOffset;

    /**
     * The app token indicating the the actual app identity
     * And it helps to get which app is sending file packet or will
     * receive file packet.
     * App token is used to support multiple app.
     */
    @SerializedName(TableMeta.Serialization.APP_TOKEN)
    @ColumnInfo(name = TableMeta.ColNames.APP_TOKEN)
    public String appToken;

    //@Exclude
    @ColumnInfo(name = TableMeta.ColNames.TRANSFERRED_BYTES)
    public long mTransferredBytes;

    @ColumnInfo(name = TableMeta.ColNames.META_DATA)
    public String metaData;


    public FilePacket(long fileId) {
        this.mFileId = fileId;
    }

    protected FilePacket(Parcel in) {
        mFileId = in.readLong();
        mSelfFullFilePath = in.readString();
        mPeerAddress = in.readString();
        mSourceAddress = in.readString();
        mFileName = in.readString();
        mFileSize = in.readLong();
        fileStatus = in.readInt();
        mRelativeOffset = in.readInt();
        appToken = in.readString();
        mIsLastPacket = in.readInt() == 1;
    }

    public long getEffectiveTransferredBytes() {
        return mTransferredBytes;
    }

    /**
     * Calculate total transferred bytes by the actual transferred bytes and relative offset if any
     *
     * @return
     */
    public long getTransferredBytes() {
        return mTransferredBytes + mRelativeOffset;
    }

    public boolean isTransferFinished() {
        return mTransferredBytes == mFileSize;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mFileId);
        dest.writeString(mSelfFullFilePath);
        dest.writeString(mPeerAddress);
        dest.writeString(mSourceAddress);
        dest.writeString(mFileName);
        dest.writeLong(mFileSize);
        dest.writeInt(fileStatus);
        dest.writeLong(mRelativeOffset);
        dest.writeString(appToken);
        dest.writeInt(mIsLastPacket ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public FilePacket copy() {

        FilePacket filePacket = new FilePacket(mFileId);

        filePacket.mSelfFullFilePath = mSelfFullFilePath;
        filePacket.mFileName = mFileName;
        filePacket.mFileSize = mFileSize;
        filePacket.mPeerAddress = mPeerAddress;
        filePacket.mSourceAddress = mSourceAddress;
        filePacket.fileStatus = fileStatus;
        filePacket.mRelativeOffset = mRelativeOffset;
        filePacket.appToken = appToken;
        filePacket.mTransferredBytes = mTransferredBytes;
        filePacket.mData = mData;
        filePacket.mLastModified = mLastModified;
        filePacket.mIsLastPacket = mIsLastPacket;
        filePacket.mPeerFullFilePath = mPeerFullFilePath;
        filePacket.metaData = metaData;
        return filePacket;
    }

    @NonNull
    @Override
    public String toString() {
        return "Read: " + mTransferredBytes + "bytes of " + mFileSize + "-to: " +
                AddressUtil.makeShortAddress(mPeerAddress) + "-path: " + mSelfFullFilePath + "-id: "
                + mFileId + "-status: " + fileStatus;
    }
}
