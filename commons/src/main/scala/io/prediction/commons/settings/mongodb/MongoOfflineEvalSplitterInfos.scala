package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings.{OfflineEvalSplitterInfo, OfflineEvalSplitterInfos}

import com.mongodb.casbah.Imports._

/** MongoDB implementation of OfflineEvalSplitterInfos. */
class MongoOfflineEvalSplitterInfos(db: MongoDB) extends OfflineEvalSplitterInfos {
  private val coll = db("offlineEvalSplitterInfos")

  private def dbObjToOfflineEvalSplitterInfo(dbObj: DBObject) = {
    val params = dbObj.as[MongoDBList]("params")
    val paramorder = params map { p => p.asInstanceOf[DBObject].as[String]("param") }
    val paramdefaults = params map { p => p.asInstanceOf[DBObject].as[Any]("default") }
    val paramnames = params map { p => p.asInstanceOf[DBObject].as[String]("name") }
    val paramdescription = params map { p => p.asInstanceOf[DBObject].as[String]("description") }
    OfflineEvalSplitterInfo(
      id               = dbObj.as[String]("_id"),
      name             = dbObj.as[String]("name"),
      description      = dbObj.getAs[String]("description"),
      engineinfoids    = MongoUtils.mongoDbListToListOfString(dbObj.as[MongoDBList]("engineinfoids")),
      commands         = dbObj.getAs[MongoDBList]("commands") map { MongoUtils.mongoDbListToListOfString(_) },
      paramdefaults    = Map() ++ (paramorder zip paramdefaults),
      paramnames       = Map() ++ (paramorder zip paramnames),
      paramdescription = Map() ++ (paramorder zip paramdescription),
      paramorder       = paramorder)
  }

  private def mergeParams(order: Seq[String], names: Map[String, String], defaults: Map[String, Any], description: Map[String, String]): Seq[Map[String, Any]] = {
    val listBuffer = collection.mutable.ListBuffer[Map[String, Any]]()

    order foreach { k =>
      listBuffer += Map("param" -> k, "default" -> defaults(k), "name" -> names(k), "description" -> description(k))
    }

    listBuffer.toSeq
  }

  def insert(offlineEvalSplitterInfo: OfflineEvalSplitterInfo) = {
    // required fields
    val obj = MongoDBObject(
      "_id"              -> offlineEvalSplitterInfo.id,
      "name"             -> offlineEvalSplitterInfo.name,
      "engineinfoids"    -> offlineEvalSplitterInfo.engineinfoids,
      "params"           -> mergeParams(offlineEvalSplitterInfo.paramorder, offlineEvalSplitterInfo.paramnames, offlineEvalSplitterInfo.paramdefaults, offlineEvalSplitterInfo.paramdescription))

    // optional fields
    val descriptionObj = offlineEvalSplitterInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = offlineEvalSplitterInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.insert(obj ++ descriptionObj ++ commandsObj)
  }

  def get(id: String) = coll.findOne(MongoDBObject("_id" -> id)) map { dbObjToOfflineEvalSplitterInfo(_) }

  def getAll() = coll.find().toSeq map { dbObjToOfflineEvalSplitterInfo(_) }

  def update(offlineEvalSplitterInfo: OfflineEvalSplitterInfo, upsert: Boolean = false) = {
    val idObj = MongoDBObject("_id" -> offlineEvalSplitterInfo.id)
    val requiredObj = MongoDBObject(
      "name"             -> offlineEvalSplitterInfo.name,
      "engineinfoids"    -> offlineEvalSplitterInfo.engineinfoids,
      "params"           -> mergeParams(offlineEvalSplitterInfo.paramorder, offlineEvalSplitterInfo.paramnames, offlineEvalSplitterInfo.paramdefaults, offlineEvalSplitterInfo.paramdescription))
    val descriptionObj = offlineEvalSplitterInfo.description.map { d => MongoDBObject("description" -> d) } getOrElse MongoUtils.emptyObj
    val commandsObj = offlineEvalSplitterInfo.commands.map { c => MongoDBObject("commands" -> c) } getOrElse MongoUtils.emptyObj

    coll.update(idObj, idObj ++ requiredObj ++ descriptionObj ++ commandsObj, upsert)
  }

  def delete(id: String) = coll.remove(MongoDBObject("_id" -> id))
}
