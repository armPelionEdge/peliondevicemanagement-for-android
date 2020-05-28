/*
 * Copyright 2020 ARM Ltd.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.peliondevicemanagement.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.components.viewholders.AccountViewHolder
import com.arm.peliondevicemanagement.components.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.components.models.user.Account
import java.util.*

class AccountAdapter(private val accountsList: ArrayList<Account>,
                     private val itemClickListener: RecyclerItemClickListener):
    RecyclerView.Adapter<AccountViewHolder>(),
    RecyclerItemClickListener,
    Filterable {

    companion object {
        private val TAG: String = AccountAdapter::class.java.simpleName
    }

    private var accountsListFiltered: ArrayList<Account> = accountsList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return AccountViewHolder(itemView = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.layout_item_account,
                parent,
                false),
            itemClickListener = itemClickListener)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) =
        holder.bind(model = accountsListFiltered[position])

    override fun getItemCount(): Int = accountsListFiltered.size

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val searchedText = charSequence.trim().toString().toLowerCase()
            accountsListFiltered = when {
                searchedText.isBlank() -> accountsList
                else -> {
                    val filteredList = arrayListOf<Account>()
                    accountsList
                        .filterTo(filteredList)
                        {
                            it.accountName.toLowerCase(Locale.getDefault()).contains(searchedText)
                        }
                    filteredList
                }
            }
            val filterResults = FilterResults()
            filterResults.values = accountsListFiltered
            return filterResults
        }

        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            accountsListFiltered = filterResults.values as ArrayList<Account>
            notifyDataSetChanged()
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as Account
        itemClickListener.onItemClick(model)
    }

}