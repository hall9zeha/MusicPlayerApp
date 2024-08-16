package com.barryzeha.ktmusicplayer.view.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.barryzeha.ktmusicplayer.databinding.OrderByDialogLayoutBinding
import com.barryzeha.ktmusicplayer.view.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint


/**
 * Project KTMusicPlayer
 * Created by Barry Zea H. on 16/8/24.
 * Copyright (c)  All rights reserved.
 **/

@AndroidEntryPoint
class OrderByDialog:DialogFragment() {
  private var _bind:OrderByDialogLayoutBinding?=null
    private val mainViewModel:MainViewModel by activityViewModels()
    private var dialogView:View?=null
    private var rbSelectedPosition =0
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
            dialogView = onCreateView(LayoutInflater.from(requireContext()),null,savedInstanceState)
            setTitle("Order list by")
            setPositiveButton("Accept"){_,dialog->
                saveOrderOption()
            }
            setNegativeButton("Cancel",null)
            setView(dialogView)
        }.create()

        return dialog
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.let{
            _bind = OrderByDialogLayoutBinding.inflate(inflater, container, false)
            _bind?.let{bind->
                return  bind.root
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getView(): View? {
        return dialogView
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListeners()

    }
    private fun setUpListeners(){
        dialogView?.let {
            _bind = OrderByDialogLayoutBinding.bind(dialogView!!)
            _bind?.radioGroup?.setOnCheckedChangeListener { group, checkedId ->
                val selectedRadioButton = view?.findViewById<RadioButton>(checkedId)
                val selectedText = selectedRadioButton?.text.toString()
                rbSelectedPosition = selectedRadioButton?.tag.toString().toInt()
                /*Toast.makeText(requireContext(), "Selected: $rbSelectedPosition", Toast.LENGTH_SHORT)
                    .show()*/
            }
        }
    }
    private fun saveOrderOption(){
        mainViewModel.setOrderListBy(rbSelectedPosition)

    }
}