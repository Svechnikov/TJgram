package io.svechnikov.tjgram.features.addpost.selectimage

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.svechnikov.tjgram.databinding.SelectImageItemBinding
import javax.inject.Inject

class ImagesAdapter constructor(private val picasso: Picasso,
                                private val selectedListener: (SelectImageView) -> Unit
): PagedListAdapter<SelectImageView, ImagesAdapter.ImageViewHolder>(
    DIFF_CALLBACK
) {

    private var columnSize = 0

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ImageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = SelectImageItemBinding.inflate(inflater, parent, false)

        binding.root.setOnClickListener {
            binding.localImage?.let {
                selectedListener(it)
            }
        }

        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = getItem(position)

        item?.let {
            val path = when(item.thumbPath) {
                null -> {
                    item.path
                }
                else -> {
                    item.thumbPath
                }
            }

            val block = {
                picasso.load(path)
                    .resize(columnSize, columnSize)
                    .centerCrop()
                    .into(holder.binding.imageView)
            }
            if (columnSize == 0) {
                val globalLayoutListener = object: ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        holder.binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        columnSize = holder.binding.imageView.width
                        block.invoke()
                    }
                }
                holder.binding.root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
            }
            else {
                block.invoke()
            }
            holder.binding.localImage = item
        }
    }

    override fun onViewRecycled(holder: ImageViewHolder) {
        super.onViewRecycled(holder)

        holder.binding.imageView.setImageBitmap(null)
    }

    companion object {
        private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<SelectImageView>() {
            override fun areItemsTheSame(oldItem: SelectImageView,
                                         newItem: SelectImageView
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SelectImageView,
                                            newItem: SelectImageView
            ): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }

    class Factory @Inject constructor(private val picasso: Picasso) {
        fun create(selectedListener: (SelectImageView) -> Unit): ImagesAdapter {
            return ImagesAdapter(picasso, selectedListener)
        }
    }

    class ImageViewHolder(val binding: SelectImageItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}

