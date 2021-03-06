package scalex.dump

import scala.tools.nsc.doc.Universe
import scala.collection.mutable
import scala.tools.nsc.doc.model.{ TypeEntity ⇒ NscTypeEntity, _ }
import scala.tools.nsc.doc.model.comment._
import com.roundeights.hasher.Hasher
import scalex.dump.model._

class Extractor(pack: String, config: Dumper.Config) {

  def passFunctions(universe: Universe, callback: List[scalex.model.Def] ⇒ Unit) {

    val done = mutable.HashSet.empty[Int]

    def gather(tpl: DocTemplateEntity): Unit = {

      val tplHashCode = tpl.hashCode
      if (!(done contains tplHashCode)) {
        done += tplHashCode

        val defs = (tpl.methods ++ tpl.values) filterNot (_.isAbstract) map makeDef

        println("%s => %d functions" format (tpl, defs.size))

        callback(defs)

        tpl.templates foreach gather
      }
    }

    gather(universe.rootPackage)
  }

  private[this] def makeDef(fun: NonTemplateMemberEntity): scalex.model.Def = {

    val comment = makeComment(fun.comment)
    val qualifiedName = makeQualifiedName(fun.qualifiedName)
    val parent = makeParent(fun.inTemplate)
    val resultType = makeTypeEntity(fun.resultType)
    val valueParams = makeValueParams(fun match {
      case fun: Def ⇒ fun.valueParams
      case fun: Val ⇒ Nil
    })
    val typeParams = makeTypeParams(fun match {
      case fun: Def ⇒ fun.typeParams
      case fun: Val ⇒ Nil
    })
    val showTypeParams = scalex.model.HigherKinded typeParamsToString typeParams
    val classSignature = parent.toString
    val paramSignature = valueParams map (_.toString) mkString ""
    val signature = List(classSignature, paramSignature, resultType) filter (_ != "") mkString " => "
    val declaration = qualifiedName + showTypeParams + ": " + signature
    val id = Hasher(declaration).md5

    val flatValueParams = valueParams.foldLeft(List[scalex.model.ValueParam]())((a, b) ⇒ a ::: b.params)
    val typeSigEnd = (flatValueParams filter (!_.isImplicit) map (_.resultType)) ::: List(resultType)
    val typeSig = if (!parent.isObject) parent.toTypeEntity :: typeSigEnd else typeSigEnd
    val normSig = scalex.model.RawTypeSig(typeSig).normalize.toString
    val aliasedSig = aliasSigToken(normSig, config.aliases.toList).toLowerCase

    fun match {
      case fun: Def ⇒ scalex.model.Def(
        id, fun.name, qualifiedName, signature, aliasedSig, declaration, parent, resultType, comment, valueParams, typeParams, pack, fun.deprecation map makeBlock
      )
      case fun: Val ⇒ scalex.model.Def(
        id, fun.name, qualifiedName, signature, aliasedSig, declaration, parent, resultType, comment, valueParams, typeParams, pack, fun.deprecation map makeBlock
      )
    }
  }

  def aliasSigToken(sig: String, aliases: List[(String, String)]): String =
    aliases.foldLeft(sig) {
      case (acc, (from, to)) ⇒ acc.replace(from, to)
    }

  private[this] def makeComment(comment: Option[Comment]) = comment map { com ⇒
    scalex.model.Comment(
      makeBlock(com.body), makeBlock(com.short), com.authors map makeBlock, com.see map makeBlock, com.result map makeBlock, com.throws.toMap map { case (a, b) ⇒ (a.replace(".", "_"), makeBlock(b)) }, com.valueParams.toMap map { case (a, b) ⇒ (a.replace(".", "_"), makeBlock(b)) }, com.typeParams.toMap map { case (a, b) ⇒ (a.replace(".", "_"), makeBlock(b)) }, com.version map makeBlock, com.since map makeBlock, com.todo map makeBlock, com.note map makeBlock, com.example map makeBlock, com.source, com.constructor map makeBlock
    )
  }

  private[this] def makeBlock(body: Body): scalex.model.Block =
    makeBlock(HtmlWriter.toHtml(body))

  private[this] def makeBlock(inline: Inline): scalex.model.Block =
    makeBlock(HtmlWriter.toHtml(inline))

  private[this] def makeBlock(html: String): scalex.model.Block =
    scalex.model.Block(html, HtmlWriter.htmlToText(html))

  private[this] def makeQualifiedName(name: String): String = name

  private[this] def makeTypeParams(tps: List[TypeParam]): List[scalex.model.TypeParam] = tps map { tp ⇒
    scalex.model.TypeParam(
      tp.name, makeQualifiedName(tp.qualifiedName), tp.variance, tp.lo map makeTypeEntity, tp.hi map makeTypeEntity, makeTypeParams(tp.typeParams)
    )
  }

  private[this] def makeParent(p: DocTemplateEntity): scalex.model.Parent = p match {
    case o: Object ⇒
      scalex.model.Parent.makeObject(
        o.name, makeQualifiedName(o.qualifiedName)
      )
    case t: Trait ⇒
      scalex.model.Parent.makeTrait(
        t.name, makeQualifiedName(t.qualifiedName), makeTypeParams(t.typeParams)
      )
  }

  private[this] def makeTypeEntity(t: NscTypeEntity): scalex.model.TypeEntity = {
    // convert to scalex TypeEntity, which is richer
    val te = t.asInstanceOf[TypeEntity]
    te.fullType
  }

  private[this] def makeValueParams(params: List[List[ValueParam]]) =
    params.map(vs ⇒ scalex.model.ValueParams(vs map makeValueParam))

  private[this] def makeValueParam(param: ValueParam): scalex.model.ValueParam =
    scalex.model.ValueParam(
      param.name, makeTypeEntity(param.resultType), param.defaultValue map (_.expression), param.isImplicit
    )
}
