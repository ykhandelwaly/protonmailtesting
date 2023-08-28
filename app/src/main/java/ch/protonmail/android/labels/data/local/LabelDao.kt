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

package ch.protonmail.android.labels.data.local

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.LABEL_TYPE_ID_CONTACT_GROUP
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
internal abstract class LabelDao : BaseDao<LabelEntity>() {

    @Query("SELECT * FROM LabelEntity WHERE userId = :userId ORDER BY labelOrder")
    abstract fun observeAllLabels(userId: UserId): Flow<List<LabelEntity>>

    @Query(
        """
        SELECT * FROM LabelEntity 
        WHERE id IN (:labelIds) 
        ORDER BY labelOrder
        """
    )
    abstract fun observeLabelsById(labelIds: List<LabelId>): Flow<List<LabelEntity>>

    @Query(
        """
        SELECT * FROM LabelEntity 
        WHERE id = :labelId 
        ORDER BY labelOrder
        """
    )
    abstract suspend fun findLabelById(labelId: LabelId): LabelEntity?

    @Query(
        """
        SELECT * FROM LabelEntity 
        WHERE id=:labelId 
        ORDER BY labelOrder
        """
    )
    abstract fun observeLabelById(labelId: LabelId): Flow<LabelEntity?>

    @Query(
        """
        SELECT * FROM LabelEntity 
        WHERE userId = :userId
        AND type in (:labelTypes)
        ORDER BY labelOrder
        """
    )
    abstract fun observeLabelsByType(userId: UserId, vararg labelTypes: LabelType): Flow<List<LabelEntity>>

    @Query(
        """
        SELECT * FROM LabelEntity 
        WHERE userId = :userId
        AND type in (:labelTypes)
        """
    )
    abstract suspend fun findLabelsByTypes(userId: UserId, vararg labelTypes: LabelType): List<LabelEntity>

    @Query(
        """
        SELECT *
        FROM LabelEntity 
        WHERE userId = :userId
        AND type = :labelType
        AND name LIKE '%' || :labelName || '%'
        ORDER BY name
    """
    )
    abstract fun observeSearchLabelsByNameAndType(
        userId: UserId,
        labelName: String,
        labelType: LabelType
    ): Flow<List<LabelEntity>>

    @Query(
        """
        SELECT * FROM LabelEntity 
        WHERE userId = :userId
        AND name = :labelName
        """
    )
    abstract suspend fun findLabelByName(userId: UserId, labelName: String): LabelEntity?

    @Query(
        """
        SELECT * FROM LabelEntity 
        WHERE userId = :userId
        AND type = :labelType
        ORDER BY labelOrder
        """
    )
    abstract fun findAllLabelsPaged(userId: UserId, labelType: LabelType): DataSource.Factory<Int, LabelEntity>

    @Query("DELETE FROM LabelEntity")
    abstract fun deleteLabelsTableData()

    @Query("DELETE FROM LabelEntity WHERE id IN (:labelIds)")
    abstract suspend fun deleteLabelsById(labelIds: List<LabelId>)

    @Query("DELETE FROM LabelEntity WHERE userId = :userId ")
    abstract suspend fun deleteAllLabels(userId: UserId)

    @Query(
        """
        DELETE FROM LabelEntity 
        WHERE userId = :userId
        AND type = $LABEL_TYPE_ID_CONTACT_GROUP
        """
    )
    abstract suspend fun deleteContactGroups(userId: UserId)

}
