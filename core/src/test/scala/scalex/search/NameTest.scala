package scalex
package search

import search._

import scalaz.{ Success, Failure }

class NameTest extends ScalexTest with WithSearch {

  "No duplicated functions" in {
    "One word" in {
      search("list", 200) must beSuccess.like {
        case defs ⇒ {
          duplicates(defs) must_== Nil
        }
      }
    }
    "Two words" in {
      search("list map", 200) must beSuccess.like {
        case defs ⇒ {
          duplicates(defs) must_== Nil
        }
      }
    }
  }
  "Find by qualified name exact matches" in {
    "Natural order" in {
      "collection list map" finds immutable + "List#map"
    }
    "Any order" in {
      "map collection list" finds immutable + "List#map"
    }
    "Only 2 words" in {
      "list map" finds immutable + "List#map"
    }
    "Only 1 word" in {
      "mapConserve" finds immutable + "List#mapConserve"
    }
  }
  "Find nothing" in {
    "One known word and one unknown word" in {
      "list rantanplan".findsNothing
    }
    "Two known words and one unknown word" in {
      "collection list rantanplan".findsNothing
    }
    "Only one unknown word" in {
      "rantanplan".findsNothing
    }
  }
  "Find by partial match" in {
    "One of two" in {
      "Start" in {
        "listset min" finds immutable + "ListSet#minBy"
      }
      "list map finds list#flatmap" in {
        "list map" finds immutable + "List#flatMap"
      }
      "list map finds ListBuffer#toMap" in {
        "list map" finds mutable + "ListBuffer#toMap"
      }
      "Contain" in {
        "listset adresol" finds immutable + "ListSet#readResolve"
      }
      "End" in {
        "listset index" finds immutable + "ListSet#zipWithIndex"
      }
      "Not existing def" in {
        "list map" notFinds mutable + "ListSet#slicePouetPouet"
      }
      "Not" in {
        "list map" notFinds mutable + "ListSet#slice"
      }
    }
    "Two of two" in {
      "Start" in {
        "bit hasDefinite" finds immutable + "BitSet.BitSetN#hasDefiniteSize"
      }
      "Contain" in {
        "itse efini" finds immutable + "BitSet.BitSetN#hasDefiniteSize"
      }
      "End" in {
        "setn size" finds immutable + "BitSet.BitSetN#hasDefiniteSize"
      }
    }
  }
  "Find exact match first" in {
    "Start" in {
      "list zip" finds Seq(
        immutable + "List#zip",
        immutable + "List#zipAll")
    }
    "End" in {
      "list map" finds Seq(
        immutable + "List#map",
        immutable + "List#flatMap",
        mutable + "ListBuffer#toMap")
    }
  }
  "Reverse tokens" in {
    "list map" in {
      "list map" finds Seq(
        immutable + "List#map",
        immutable + "Map#toList")
    }
    "map list" in {
      //"map list" finds Seq(
      //immutable + "Map#toList",
      //immutable + "List#map")
      pending
    }
  }
  "Simple signature first" in {
    "map list" in {
      "map list" findsDeclarations Seq(
        "scala.collection.immutable.List#map[B]: List[+A] => (f: (A => B)) => List[B]",
        "scala.collection.immutable.List#map[B, That]: List[+A] => (f: (A => B))(implicit bf: CanBuildFrom[List[A], B, That]) => That"
      )
    }
  }
}
