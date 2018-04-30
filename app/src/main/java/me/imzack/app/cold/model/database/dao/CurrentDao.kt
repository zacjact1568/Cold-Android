package me.imzack.app.cold.model.database.dao

import android.arch.persistence.room.*
import io.reactivex.Single
import me.imzack.app.cold.common.Constant
import me.imzack.app.cold.model.database.entity.CurrentEntity

@Dao
interface CurrentDao {

    @Insert
    fun insert(currentEntity: CurrentEntity)

    @Update
    fun update(currentEntity: CurrentEntity)

    @Query("DELETE FROM ${Constant.CURRENT} WHERE ${Constant.CITY_ID} = :cityId")
    fun delete(cityId: String)

    @Query("SELECT * FROM ${Constant.CURRENT}")
    fun loadAll(): Single<Array<CurrentEntity>>
}