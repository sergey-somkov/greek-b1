package com.somkov.greek_b1.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/*
 * The bundle of the content, content/bundle.json of the assets, as tools/bundle.py writes it:
 * the catalog files in their format and the items with the columns of [ContentItem] and their
 * `body`. Decoding it gives the rows of the content tables.
 */

/** The rows of the content tables in a bundle. */
data class ContentRows(
    val items: List<ContentItem>,
    val taskTypes: List<CatalogTaskType>,
    val topics: List<CatalogTopic>,
    val situations: List<CatalogSituation>,
    val functions: List<CatalogFunction>,
    val criteria: List<CatalogCriterion>,
)

/** Decodes the JSON of a bundle; a bundle that is not in its format throws a `SerializationException`. */
fun decodeBundle(json: String): ContentRows {
    val bundle = format.decodeFromString<Bundle>(json)
    val catalog = bundle.catalog
    return ContentRows(
        items = bundle.items.map {
            ContentItem(
                id = it.id,
                part = it.part,
                taskType = it.taskType,
                situation = it.situation,
                topic = it.topic,
                title = it.title,
                retired = it.retired,
                body = it.body.toString(),
            )
        },
        taskTypes = catalog.taskTypes.taskTypes.map {
            CatalogTaskType(it.id, it.part, it.greek, answerFormat = it.answer, it.retired)
        },
        topics = catalog.topics.topics.map { CatalogTopic(it.id, it.name, it.greek, it.retired) },
        situations = catalog.topics.topics.flatMap { topic ->
            topic.situations.map { CatalogSituation(it.id, topic.id, it.name, it.greek, it.retired) }
        },
        functions = catalog.functions.functions.map { CatalogFunction(it.id, it.name, it.greek, it.retired) },
        criteria = catalog.criteria.flatMap { (part, criteria) ->
            criteria.map { CatalogCriterion(part, it.id, it.name, it.greek, it.retired) }
        },
    )
}

// The catalog has more fields than the tables (the examples, the points, the bands, ...).
private val format = Json { ignoreUnknownKeys = true }

@Serializable
private class Bundle(val catalog: Catalog, val items: List<Item>)

@Serializable
private class Catalog(
    @SerialName("task_types") val taskTypes: TaskTypes,
    val topics: Topics,
    val functions: Functions,
    /** The criteria by part: `writing` and `speaking`. */
    val criteria: Map<String, List<Entry>>,
)

@Serializable
private class TaskTypes(@SerialName("task_types") val taskTypes: List<TaskType>)

@Serializable
private class TaskType(
    val id: String,
    val part: String,
    val greek: String,
    /** The ID of the answer format. */
    val answer: String,
    val retired: Boolean,
)

@Serializable
private class Topics(val topics: List<Topic>)

@Serializable
private class Topic(
    val id: String,
    val name: String,
    val greek: String,
    val retired: Boolean,
    val situations: List<Entry>,
)

@Serializable
private class Functions(val functions: List<Entry>)

/** A situation, a function or a criterion. */
@Serializable
private class Entry(val id: String, val name: String, val greek: String, val retired: Boolean)

@Serializable
private class Item(
    val id: String,
    val part: String,
    @SerialName("task_type") val taskType: String,
    val situation: String,
    val topic: String,
    val title: String,
    val retired: Boolean,
    val body: JsonObject,
)
