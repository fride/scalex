package ophir.db

import ophir.model.Def
import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._

object DefRepo extends SalatDAO[Def, ObjectId](collection = MongoConnection()("ophir")("def")) {

  def batchInsert(objs: List[Def]) { collection insert (objs map _grater.asDBObject) }

  def findByTokens(tokens: List[String]): Iterator[Def] =
    find(MongoDBObject("$and" -> tokensToEqualities(tokens)))

  def findBySig(sig: String): Iterator[Def] =
    find(MongoDBObject("normalizedTypeSig" -> sig))

  def findByTokensAndSig(tokens: List[String], sig: String): Iterator[Def] =
    find(MongoDBObject("$and" -> tokensToEqualities(tokens), "normalizedTypeSig" -> sig))

  private def tokensToEqualities(tokens: List[String]) =
    tokens map (token => MongoDBObject("tokens" -> token.toLowerCase))

  def findAll: Iterator[Def] = find(MongoDBObject())

  def index() {
    collection.ensureIndex(MongoDBObject("tokens" -> 1))
    collection.ensureIndex(MongoDBObject("normalizedTypeSig" -> 1))
  }

  def drop() {
    collection remove MongoDBObject()
    collection.dropIndexes
  }
}