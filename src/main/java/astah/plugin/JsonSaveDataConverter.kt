package astah.plugin

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonSaveDataConverter {
    private val moshiForC4modelIcon = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val jsonAdapterForC4modelIcon = moshiForC4modelIcon.adapter(C4modelIconData::class.java)
    fun convertFromC4modelIconToJSON(models: C4modelIconData): String = jsonAdapterForC4modelIcon.toJson(models)
    fun convertFromJsonToC4modelIcon(json: String): C4modelIconData = jsonAdapterForC4modelIcon.fromJson(json)!!

    private val moshiForC4modelRectangle = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val jsonAdapterForC4modelRectangle= moshiForC4modelRectangle.adapter(C4modelRectangleData::class.java)
    fun convertFromC4modelRectangleToJSON(models: C4modelRectangleData): String =
        jsonAdapterForC4modelRectangle.toJson(models)
    fun convertFromJsonToC4modelRectangle(json: String): C4modelRectangleData =
        jsonAdapterForC4modelRectangle.fromJson(json)!!
}