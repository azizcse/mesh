package com.w3engineers.meshrnd.ui.chat;

/*
 *  ****************************************************************************
 *  * Created by : Md. Azizul Islam on 11/30/2018 at 6:43 PM.
 *  * Email : azizul@w3engineers.com
 *  *
 *  * Purpose:
 *  *
 *  * Last edited by : Md. Azizul Islam on 11/30/2018.
 *  *
 *  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>
 *  ****************************************************************************
 */

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.FileProvider;
import androidx.databinding.ViewDataBinding;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jarvanmo.exoplayerview.media.SimpleMediaSource;
import com.w3engineers.mesh.util.Constant;
import com.w3engineers.meshrnd.R;
import com.w3engineers.meshrnd.databinding.ItemFileInBinding;
import com.w3engineers.meshrnd.databinding.ItemFileOutBinding;
import com.w3engineers.meshrnd.databinding.ItemTextMessageInBinding;
import com.w3engineers.meshrnd.databinding.ItemTextMessageOutBinding;
import com.w3engineers.meshrnd.databinding.ItemVideoInBinding;
import com.w3engineers.meshrnd.databinding.ItemVideoOutBinding;
import com.w3engineers.meshrnd.model.MessageModel;
import com.w3engineers.meshrnd.ui.base.BaseAdapter;
import com.w3engineers.meshrnd.ui.base.BaseViewHolder;
import com.w3engineers.meshrnd.util.TimeUtil;

import java.io.File;

import timber.log.Timber;


public class ChatAdapter extends BaseAdapter<MessageModel> {
    private final int TEXT_IN = 1;
    private final int TEXT_OUT = 2;
    private final int IMAGE_IN = 3;
    private final int IMAGE_OUT = 4;
    private final int VIDEO_IN = 5;
    private final int VIDEO_OUT = 6;

    private Context context;

    public ChatAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel item = getItem(position);
        if (item == null)
            return TEXT_OUT;

        if (item.messageType == ChatActivity.IMAGE_MESSAGE) {
            if (item.incoming) {
                return IMAGE_IN;
            } else {
                return IMAGE_OUT;
            }
        } else if (item.messageType == ChatActivity.VIDEO_MESSAGE) {
            if (item.incoming) {
                return VIDEO_IN;
            } else {
                return VIDEO_OUT;
            }
        } else {
            if (item.incoming) {
                return TEXT_IN;
            } else {
                return TEXT_OUT;
            }
        }
    }

    @Override
    public boolean isEqual(MessageModel left, MessageModel right) {
        return false;
    }

    @Override
    public BaseViewHolder newViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder baseViewHolder = null;
        switch (viewType) {
            case TEXT_IN:
                baseViewHolder = new TextInHolder(inflate(parent, R.layout.item_text_message_in));
                break;
            case TEXT_OUT:
                baseViewHolder = new TextOutHolder(inflate(parent, R.layout.item_text_message_out));
                break;
            case IMAGE_IN:
                baseViewHolder = new FileInHolder(inflate(parent, R.layout.item_file_in));
                break;
            case IMAGE_OUT:
                baseViewHolder = new FileOutHolder(inflate(parent, R.layout.item_file_out));
                break;
            case VIDEO_IN:
                baseViewHolder = new VideoInHolder(inflate(parent, R.layout.item_video_in));
                break;
            case VIDEO_OUT:
                baseViewHolder = new VideoOutHolder(inflate(parent, R.layout.item_video_out));
                break;
        }
        return baseViewHolder;
    }

    public void updateProgress(String fileMessageId, int progress) {
        for (int i = getItemCount(); i > 0; i--) {
            MessageModel model = getItem(i);
            if (model != null && model.messageId != null && model.messageId.equals(fileMessageId)) {
                Log.d("FileMessageTest", "Progress: " + progress);
                model.progress = progress;
                model.messageStatus = model.incoming ? Constant.MessageStatus.RECEIVING :
                        Constant.MessageStatus.SENDING;
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void setFileError(String fileMessageId) {
        for (int i = getItemCount(); i > 0; i--) {
            MessageModel model = getItem(i);
            if (model != null && model.messageId != null && model.messageId.equals(fileMessageId)) {

                model.messageStatus = Constant.MessageStatus.FAILED;
                notifyItemChanged(i);
                break;
            }
        }
    }


    private class TextInHolder extends BaseViewHolder<MessageModel> {

        public TextInHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
        }

        @Override
        public void bind(MessageModel item, ViewDataBinding viewDataBinding) {
            ItemTextMessageInBinding binding = (ItemTextMessageInBinding) viewDataBinding;
            int padding = binding.textViewMessage.getPaddingTop();
            binding.textViewMessage.setPadding(padding, padding, padding, padding);
            binding.setMessage(item);
            binding.textViewDateTime.setText(TimeUtil.parseMillisToTime(item.receiveTime));
        }

        @Override
        public void onClick(View v) {

        }
    }

    private class TextOutHolder extends BaseViewHolder<MessageModel> {
        public TextOutHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
        }

        @Override
        public void bind(MessageModel item, ViewDataBinding viewDataBinding) {
            ItemTextMessageOutBinding binding = (ItemTextMessageOutBinding) viewDataBinding;
            binding.setMessage(item);
            int padding = binding.textViewMessage.getPaddingTop();
            binding.textViewMessage.setPadding(padding, padding, padding, padding);

            if (item.messageStatus == Constant.MessageStatus.FAILED) {
                binding.textViewDateTime.setText("Failed");
            } else if (item.messageStatus == Constant.MessageStatus.RECEIVED) {
                binding.textViewDateTime.setText("Received");
            } else if (item.messageStatus == Constant.MessageStatus.DELIVERED) {
                binding.textViewDateTime.setText("Delivered");
            } else if (item.messageStatus == Constant.MessageStatus.SEND) {
                binding.textViewDateTime.setText("Send");
            } else if (item.messageStatus == Constant.MessageStatus.RECEIVING) {
                binding.textViewDateTime.setText("Receiving...");
            } else {
                binding.textViewDateTime.setText("Sending...");
            }
        }

        @Override
        public void onClick(View v) {
        }
    }

    private class FileInHolder extends BaseViewHolder<MessageModel> {
        ItemFileInBinding binding;

        public FileInHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
        }

        @Override
        public void bind(MessageModel item, ViewDataBinding viewDataBinding) {
            binding = (ItemFileInBinding) viewDataBinding;
            Glide.with(context).load(item.message)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.imageViewMessageIn);
            //binding.imageViewMessageIn.setImageURI(Uri.parse(item.message));
            if (item.progress >= 100 || item.progress == 0 || item.messageStatus ==
                    Constant.MessageStatus.FAILED) {
                binding.progressBar.setVisibility(View.GONE);
                Log.d("FileMessageTest", "Progress gone to 0");
            } else {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.progressBar.setProgress(item.progress);
            }

            if (item.messageStatus == Constant.MessageStatus.FAILED) {
                binding.imageViewRetry.setVisibility(View.VISIBLE);
            } else {
                binding.imageViewRetry.setVisibility(View.GONE);
            }

            setClickListener(binding.imageViewRetry);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.image_view_retry) {
                binding.imageViewRetry.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.VISIBLE);
            }
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(view, getItem(getAdapterPosition()));
            }

        }
    }

    private class FileOutHolder extends BaseViewHolder<MessageModel> {
        ItemFileOutBinding binding;

        public FileOutHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
            binding = (ItemFileOutBinding) viewDataBinding;
        }

        @Override
        public void bind(MessageModel item, ViewDataBinding viewDataBinding) {

            Glide.with(context).load(item.message).into(binding.imageViewMessageOut);
            //binding.imageViewMessageOut.setImageURI(Uri.parse(item.message));

            if (item.progress >= 100 || item.progress == 0 || item.messageStatus == Constant.MessageStatus.FAILED) {
                binding.progressBar.setVisibility(View.GONE);
                Log.d("FileMessageTest", "Progress gone to 0");
            } else {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.progressBar.setProgress(item.progress);
            }

            if (item.messageStatus == Constant.MessageStatus.FAILED) {
                binding.imageViewRetry.setVisibility(View.VISIBLE);
            } else {
                binding.imageViewRetry.setVisibility(View.GONE);
            }

            setClickListener(binding.imageViewRetry);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.image_view_retry) {
                binding.imageViewRetry.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.VISIBLE);
            }
            mItemClickListener.onItemClick(v, getItem(getAdapterPosition()));
        }
    }

    private class VideoInHolder extends BaseViewHolder<MessageModel> {
        ItemVideoInBinding binding;

        public VideoInHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
            binding = (ItemVideoInBinding) viewDataBinding;
            setClickListener(binding.imageViewRetry);
        }

        @Override
        public void bind(MessageModel item, ViewDataBinding viewDataBinding) {

            //Glide.with(mContext).load(item.message).into(binding.imageViewMessageIn);
            // binding.imageViewMessageIn.setMediaController(new MediaController(context));
            //binding.imageViewMessageIn.setVideoPath(item.message);

            if (item.progress >= 100 || item.progress == 0) {

                binding.imageViewMessageIn.setVisibility(View.VISIBLE);
                binding.imageViewMessageInThumb.setVisibility(View.GONE);


                try {
                    String packageName = context.getPackageName() + ".provider";
                    Uri fileUri = FileProvider.getUriForFile(context, packageName, new File(item.message));
                    //Timber.d("Video: " + fileUri.getEncodedPath());
                    SimpleMediaSource mediaSource = new SimpleMediaSource(fileUri);
                    binding.imageViewMessageIn.play(mediaSource);
                    binding.imageViewMessageIn.pause();
                    //binding.imageViewMessageIn.setVideoURI(fileUri);

                    //binding.imageViewMessageIn.requestFocus();
                    // binding.imageViewMessageIn.seekTo(1);
                    //binding.imageViewMessageIn.pause();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            } else {
                binding.imageViewMessageIn.setVisibility(View.GONE);
                binding.imageViewMessageInThumb.setVisibility(View.VISIBLE);
            }

            if (item.messageStatus == Constant.MessageStatus.FAILED) {
                binding.imageViewRetry.setVisibility(View.VISIBLE);
            } else {
                binding.imageViewRetry.setVisibility(View.GONE);
            }

        }

        @Override
        public void onClick(View view) {

            if (view.getId() == R.id.image_view_retry) {
                binding.imageViewRetry.setVisibility(View.GONE);
                //binding.progressBar.setVisibility(View.VISIBLE);
            }
            mItemClickListener.onItemClick(view, getItem(getAdapterPosition()));
        }
    }

    private class VideoOutHolder extends BaseViewHolder<MessageModel> {
        ItemVideoOutBinding binding;

        public VideoOutHolder(ViewDataBinding viewDataBinding) {
            super(viewDataBinding);
            binding = (ItemVideoOutBinding) viewDataBinding;
            setClickListener(binding.imageViewRetry);
        }

        @Override
        public void bind(MessageModel item, ViewDataBinding viewDataBinding) {
            //binding.imageViewMessageOut.setMediaController(new MediaController(context));

            try {
                String packageName = context.getPackageName() + ".provider";
                Uri fileUri = FileProvider.getUriForFile(context, packageName, new File(item.message));
                SimpleMediaSource mediaSource = new SimpleMediaSource(fileUri);
                binding.imageViewMessageOut.play(mediaSource);
                binding.imageViewMessageOut.pause();

                //binding.imageViewMessageOut.requestFocus();
                //binding.imageViewMessageOut.setVideoPath(item.message);
                //binding.imageViewMessageOut.seekTo(1);
                //binding.imageViewMessageOut.pause();
            } catch (Exception e) {
                e.printStackTrace();
            }


            if (item.progress >= 100 || item.progress == 0 || item.messageStatus == Constant.MessageStatus.FAILED) {
                binding.progressBar.setVisibility(View.GONE);
                Log.d("FileMessageTest", "Progress gone to 0");
            } else {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.progressBar.setProgress(item.progress);
            }

            if (item.messageStatus == Constant.MessageStatus.FAILED) {
                binding.imageViewRetry.setVisibility(View.VISIBLE);
            } else {
                binding.imageViewRetry.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.image_view_retry) {
                binding.imageViewRetry.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.VISIBLE);
            }
            mItemClickListener.onItemClick(view, getItem(getAdapterPosition()));

        }
    }
}
