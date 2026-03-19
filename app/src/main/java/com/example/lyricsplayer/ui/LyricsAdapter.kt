package com.example.lyricsplayer.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lyricsplayer.data.model.LyricsLine

class LyricsAdapter(
    private val onLineClick: ((LyricsLine) -> Unit)? = null
) : RecyclerView.Adapter<LyricsAdapter.LyricsViewHolder>() {

    private var lines: List<LyricsLine> = emptyList()
    private var currentLineIndex: Int = -1

    fun updateLines(newLines: List<LyricsLine>) {
        lines = newLines
        notifyDataSetChanged()
    }

    fun highlightLine(index: Int) {
        if (index == currentLineIndex) return
        val oldIndex = currentLineIndex
        currentLineIndex = index
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (index >= 0) notifyItemChanged(index)
    }

    fun highlightByTime(currentTimeMs: Long) {
        val newIndex = lines.indexOfLast { it.timeMs <= currentTimeMs }
        if (newIndex != currentLineIndex) {
            highlightLine(newIndex)
        }
    }

    fun getCurrentHighlightedIndex(): Int = currentLineIndex

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricsViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(24, 16, 24, 16)
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = android.view.Gravity.CENTER
            isClickable = true
            isFocusable = true
        }
        return LyricsViewHolder(textView)
    }

    override fun onBindViewHolder(holder: LyricsViewHolder, position: Int) {
        holder.bind(lines[position], position == currentLineIndex)
    }

    override fun getItemCount(): Int = lines.size

    inner class LyricsViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(line: LyricsLine, isHighlighted: Boolean) {
            textView.text = line.text
            if (isHighlighted) {
                textView.setTextColor(Color.WHITE)
                textView.textSize = 20f
                textView.setTypeface(null, Typeface.BOLD)
                textView.alpha = 1.0f
            } else {
                textView.setTextColor(Color.GRAY)
                textView.textSize = 16f
                textView.setTypeface(null, Typeface.NORMAL)
                textView.alpha = 0.6f
            }
            textView.setOnClickListener {
                onLineClick?.invoke(line)
            }
        }
    }
}
