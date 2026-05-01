package com.martinjm.buynote.data.db.converter

import androidx.room.TypeConverter
import com.martinjm.buynote.domain.model.ListStatus
import com.martinjm.buynote.domain.model.QuantityUnit

class Converters {
    @TypeConverter fun fromListStatus(value: ListStatus): String = value.name
    @TypeConverter fun toListStatus(value: String): ListStatus = ListStatus.valueOf(value)

    @TypeConverter fun fromQuantityUnit(value: QuantityUnit): String = value.name
    @TypeConverter fun toQuantityUnit(value: String): QuantityUnit = QuantityUnit.valueOf(value)
}
