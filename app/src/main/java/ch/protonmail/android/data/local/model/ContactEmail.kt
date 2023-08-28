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
package ch.protonmail.android.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.io.Serializable

const val TABLE_CONTACT_EMAILS = "contact_emailsv3"
const val COLUMN_CONTACT_EMAILS_ID = "ID"
const val COLUMN_CONTACT_EMAILS_NAME = "Name"
const val COLUMN_CONTACT_EMAILS_EMAIL = "Email"
const val COLUMN_CONTACT_EMAILS_EMAIL_TYPE = "Type"
const val COLUMN_CONTACT_EMAILS_ORDER = "Order"
const val COLUMN_CONTACT_EMAILS_CONTACT_ID = "ContactID"
const val COLUMN_CONTACT_EMAILS_LABEL_IDS = "LabelIDs"
const val COLUMN_CONTACT_EMAILS_DEFAULTS = "Defaults"
const val COLUMN_CONTACT_EMAILS_LAST_TIME_USED = "LastUsedTime"

@Entity(
    tableName = TABLE_CONTACT_EMAILS,
    indices = [
        Index(COLUMN_CONTACT_EMAILS_ID, unique = true),
        Index(COLUMN_CONTACT_EMAILS_EMAIL, unique = false)
    ]
)
@TypeConverters(value = [ContactEmailConverter::class])
@kotlinx.serialization.Serializable
data class ContactEmail @JvmOverloads constructor(

    @SerializedName(COLUMN_CONTACT_EMAILS_ID)
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_ID)
    @PrimaryKey
    var contactEmailId: String,

    @SerializedName(COLUMN_CONTACT_EMAILS_EMAIL)
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_EMAIL)
    var email: String,

    @SerializedName(COLUMN_CONTACT_EMAILS_NAME)
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_NAME)
    var name: String?,

    @SerializedName(COLUMN_CONTACT_EMAILS_DEFAULTS)
    @Ignore
    private var defaults: Int = 0,

    @SerializedName(COLUMN_CONTACT_EMAILS_EMAIL_TYPE)
    @Ignore
    private var type: MutableList<String>? = null,

    @SerializedName(COLUMN_CONTACT_EMAILS_ORDER)
    @Ignore
    private var order: Int = 0,

    @SerializedName(COLUMN_CONTACT_EMAILS_CONTACT_ID)
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_CONTACT_ID, index = true)
    var contactId: String? = null,

    @SerializedName(COLUMN_CONTACT_EMAILS_LABEL_IDS)
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_LABEL_IDS)
    var labelIds: List<String>? = null,

    @SerializedName(COLUMN_CONTACT_EMAILS_LAST_TIME_USED)
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_LAST_TIME_USED)
    var lastUsedTime: Int = 0

) : Serializable {

    var selected: Boolean = false
    var pgpIcon: Int = 0 // for pgp
    var pgpIconColor: Int = 0 // for pgp
    var pgpDescription: Int = 0 // for clicking description
    var isPGP: Boolean = false
}

class ContactEmailConverter {

    private val gson by lazy {
        GsonBuilder().disableHtmlEscaping().create()
    }

    @TypeConverter
    fun contactEmailLabelsToString(contactEmailLabels: List<String>?): String? {
        if (contactEmailLabels.orEmpty().isEmpty()) {
            return ""
        }
        return gson.toJson(contactEmailLabels)
    }

    @TypeConverter
    fun stringToContactEmailLabels(value: String?): List<String>? {
        if (value.isNullOrEmpty()) {
            return ArrayList()
        }
        return gson.fromJson(value, Array<String>::class.java).asList()
    }
}
