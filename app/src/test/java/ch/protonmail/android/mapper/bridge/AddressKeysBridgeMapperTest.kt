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
package ch.protonmail.android.mapper.bridge

import assert4k.assert
import assert4k.equals
import assert4k.that
import assert4k.times
import assert4k.unaryPlus
import me.proton.core.util.kotlin.invoke
import kotlin.test.Test
import assert4k.invoke as fix
import ch.protonmail.android.api.models.Keys as OldKey

/**
 * Test suite for [AddressKeysBridgeMapper] and [AddressKeyBridgeMapper]
 */
internal class AddressKeysBridgeMapperTest {

    private val singleMapper = AddressKeyBridgeMapper()
    private val multiMapper = AddressKeysBridgeMapper(singleMapper)

    @Test
    fun `can map correctly single Key`() {
        val oldKey = OldKey(
            id = "id",
            flags = 3,
            privateKey = "-----BEGIN PGP PRIVATE KEY BLOCK-----private_key-----END PGP PRIVATE KEY BLOCK-----",
            token = "-----BEGIN PGP MESSAGE-----token-----END PGP MESSAGE-----",
            signature = "-----BEGIN PGP SIGNATURE KEY BLOCK-----signature-----END PGP SIGNATURE KEY BLOCK-----",
            activation = "-----BEGIN PGP MESSAGE-----activation-----END PGP MESSAGE-----"
        )

        val newKey = singleMapper { oldKey.toNewModel() }

        assert that newKey * {
            +id.id equals "id"
            +canEncrypt equals true
            +canVerifySignature equals true
            +privateKey.content.s equals "private_key"
            +token?.content?.s equals "token"
            +signature?.content?.s equals "signature"
            +activation?.content?.s equals "activation"
        }
    }

    @Test
    fun `can map correctly multiple keys`() {
        val oldKeys = (1..10).map { OldKey("$it", primary = it == 4) }

        val newKeys = multiMapper { oldKeys.toNewModel() }

        assert that newKeys * {
            +primaryKey?.id?.id equals "4"
            +keys.size.fix() equals 10
        }
    }

    @Test
    fun `does pick first Key as primary, if none is defined`() {
        val oldKeys = (1..10).map { OldKey("$it") }

        val newKeys = multiMapper { oldKeys.toNewModel() }

        assert that newKeys * {
            +primaryKey?.id?.id equals "1"
            +keys.size.fix() equals 10
        }
    }

    private fun OldKey(
        id: String = "none",
        primary: Boolean = false,
        flags: Int = 0,
        privateKey: String = "none",
        token: String = "none",
        signature: String = "none",
        activation: String = "none"
    ) = OldKey(id, privateKey, flags, if (primary) 1 else 0, token, signature, activation, 0)
}
