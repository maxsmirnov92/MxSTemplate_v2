package net.maxsmr.core.network.retrofit.serializers

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer


//Сериалайзер на случай, когда вместо пустового массива сервер присылает пустую строку
internal object StringListSerializer : JsonTransformingSerializer<List<String>>(ListSerializer(String.serializer())) {

    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf()) else element
}

internal object IntListSerializer : JsonTransformingSerializer<List<Int>>(ListSerializer(Int.serializer())) {

    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf()) else element
}