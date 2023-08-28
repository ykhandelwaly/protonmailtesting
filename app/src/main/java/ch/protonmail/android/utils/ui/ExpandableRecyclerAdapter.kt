/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.utils.ui

import android.content.Context
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

const val TYPE_HEADER = 1000
const val TYPE_ITEM = 1001

abstract class ExpandableRecyclerAdapter<T : ExpandableRecyclerAdapter.ListItem>(
    private var mContext: Context
) :
    RecyclerView.Adapter<ExpandableRecyclerAdapter<T>.ViewHolder>() {

    protected var allItems: MutableList<T> = ArrayList()
    public var visibleItems: MutableList<T> = ArrayList()
        protected set
    private var indexList: MutableList<Int> = ArrayList()
    private var expandMap = SparseIntArray()

    open class ListItem(var itemType: Int)

    override fun getItemId(i: Int) = i.toLong()

    override fun getItemCount() = visibleItems.size

    fun getItem(i: Int): T? {
        return if (i < 0 || i >= visibleItems.size) {
            null
        } else {
            visibleItems[i]
        }
    }

    protected fun inflate(resourceID: Int, viewGroup: ViewGroup): View =
        LayoutInflater.from(mContext).inflate(resourceID, viewGroup, true)

    open inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    open inner class HeaderViewHolder(view: View) : ViewHolder(view) {

        init {
            view.setOnClickListener {
                if (allItems.size > 2) {
                    handleClick()
                }
            }
        }

        private fun handleClick() {
            toggleExpandedItems(layoutPosition, false)
        }
    }

    fun toggleExpandedItems(position: Int, notify: Boolean): Boolean {
        return if (isExpanded(position)) {
            collapseItems(position, notify)
            false
        } else {
            expandItems(position, notify)
            true
        }
    }

    private fun expandItems(position: Int, notify: Boolean) {
        var count = 0
        val index = indexList[position]
        var insert = position

        var i = index + 1
        while (i < allItems.size && allItems[i].itemType != TYPE_HEADER) {
            insert++
            count++
            visibleItems.add(insert, allItems[i])
            indexList.add(insert, i)
            i++
        }

        notifyItemRangeInserted(position + 1, count)

        val allItemsPosition = indexList[position]
        expandMap.put(allItemsPosition, 1)

        if (notify) {
            notifyItemChanged(position)
        }
    }

    private fun collapseItems(position: Int, notify: Boolean) {
        var count = 0
        val index = indexList[position]

        var i = index + 1
        while (i < allItems.size && allItems[i].itemType != TYPE_HEADER) {
            count++
            visibleItems.removeAt(position + 1)
            indexList.removeAt(position + 1)
            i++
        }

        notifyItemRangeRemoved(position + 1, count)

        val allItemsPosition = indexList[position]
        expandMap.delete(allItemsPosition)

        if (notify) {
            notifyItemChanged(position)
        }
    }

    fun isExpanded(position: Int): Boolean {
        val allItemsPosition = indexList[position]
        return expandMap.get(allItemsPosition, -1) >= 0
    }

    override fun getItemViewType(position: Int): Int {
        return visibleItems[position].itemType
    }

    fun setItems(items: MutableList<T>) {
        allItems = items
        val visibleItems = ArrayList<T>()
        expandMap.clear()
        indexList.clear()

        for (i in items.indices) {
            if (items[i].itemType == TYPE_HEADER) {
                indexList.add(i)
                visibleItems.add(items[i])
            }
        }

        this.visibleItems = visibleItems

        notifyDataSetChanged()
    }

    protected fun removeItemAt(visiblePosition: Int) {
        val allItemsPosition = indexList[visiblePosition]

        allItems.removeAt(allItemsPosition)
        visibleItems.removeAt(visiblePosition)

        incrementIndexList(allItemsPosition, visiblePosition, -1)
        incrementExpandMapAfter(allItemsPosition, -1)

        notifyItemRemoved(visiblePosition)
    }

    private fun incrementExpandMapAfter(position: Int, direction: Int) {
        val newExpandMap = SparseIntArray()

        for (i in 0 until expandMap.size()) {
            val index = expandMap.keyAt(i)
            newExpandMap.put(if (index < position) index else index + direction, 1)
        }

        expandMap = newExpandMap
    }

    private fun incrementIndexList(allItemsPosition: Int, visiblePosition: Int, direction: Int) {
        val newIndexList = ArrayList<Int>()

        for (i in indexList.indices) {
            if (i == visiblePosition) {
                if (direction > 0) {
                    newIndexList.add(allItemsPosition)
                }
            }

            val `val` = indexList[i]
            newIndexList.add(if (`val` < allItemsPosition) `val` else `val` + direction)
        }

        indexList = newIndexList
    }
}
