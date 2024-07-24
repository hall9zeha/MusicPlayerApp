package com.barryzeha.ktmusicplayer.view.ui.adapters

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 24/7/24.
 * Copyright (c)  All rights reserved.
 **/

class StickyHeaderItemDecoration: RecyclerView.ItemDecoration() {

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        val header = createHeaderView(topChildPosition,parent)
            ?: createPreviousHeaderView(topChildPosition,parent)
            ?: return
        makeViewFullLayout(header,parent)
        val nextHeader = findNextHeaderView(parent)
        val dx = if(nextHeader != null){
            val result = nextHeader.top.toFloat() - header.height
            if(result <=0){
                result
            }else{
                0F
            }
        }else{
            0f
        }
        c.save()
        c.translate(0f,dx)
        header.draw(c)
        c.restore()

    }

    private fun createHeaderView(position:Int, parent:RecyclerView): View?{
        val adapter = parent.adapter?: return null
        val viewType = adapter.getItemViewType(position)
        val holder = adapter.onCreateViewHolder(parent, viewType) as? StickyViewHolder
            ?:return null
        adapter.onBindViewHolder(holder,position)
        return holder.itemView?:null
    }

    private fun createPreviousHeaderView(position:Int, parent:RecyclerView):View?{
        if(position <=0){
            return null
        }
        for (i in position downTo 0){
            val header = createHeaderView(i,parent)
            if(header != null){
                return header
            }
        }
        return null
    }
    private fun findNextHeaderView(parent:RecyclerView):View?{
        val itemCount = parent.adapter?.itemCount ?:0
        if(itemCount<=1){
            return null
        }
        val visibleCount = parent.layoutManager?.childCount?:return null
        for(i in 1..visibleCount){
            val header = parent.getChildAt(i)
            if(header != null && parent.getChildViewHolder(header) as? StickyViewHolder != null) {
                return header
            }
            }
        return null
        }
    private fun makeViewFullLayout(view:View, parent:RecyclerView){
        val parentWidth = View.MeasureSpec.makeMeasureSpec(parent.width,View.MeasureSpec.EXACTLY)
        val parentHeight = View.MeasureSpec.makeMeasureSpec(parent.height,View.MeasureSpec.EXACTLY)

        val childLayoutParam = view.layoutParams?: return

        val childWidth = ViewGroup.getChildMeasureSpec(parentWidth,0,childLayoutParam.width)
        val childHeight = ViewGroup.getChildMeasureSpec(parentHeight,0,childLayoutParam.height)

        view.measure(childWidth,childHeight)
        view.layout(0,0,view.measuredWidth,view.measuredHeight)
    }

}