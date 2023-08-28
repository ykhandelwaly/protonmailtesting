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
package ch.protonmail.android.api.models

import ch.protonmail.android.testAndroidInstrumented.HamcrestMismatchBuilder
import ch.protonmail.android.testAndroidInstrumented.build
import org.hamcrest.Description
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.TypeSafeDiagnosingMatcher

/**
 * Created by Kamil Rajtar on 07.09.18.  */
class ContactEncryptedDataMatcher(contactEncryptedData:ContactEncryptedData):TypeSafeDiagnosingMatcher<ContactEncryptedData>() {
	private val type=`is`(equalTo(contactEncryptedData.type))
	private val data=`is`(equalTo(contactEncryptedData.data))
	private val signature=`is`(equalTo(contactEncryptedData.signature))

	override fun describeTo(description:Description) {
		description.build("type" to type,"data" to data,"signature" to signature)
	}

	override fun matchesSafely(item:ContactEncryptedData,
							   mismatchDescription:Description):Boolean {
		return HamcrestMismatchBuilder(mismatchDescription).match("type",type,item.type)
				.match("data",data,item.data)
				.match("signature",signature,item.signature)
				.build()
	}
}

