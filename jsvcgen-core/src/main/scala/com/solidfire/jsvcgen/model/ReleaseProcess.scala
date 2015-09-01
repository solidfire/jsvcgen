package com.solidfire.jsvcgen.model

/**
 * Created by Jason Ryan Womack on 8/27/15.
 */
object ReleaseProcess {

  sealed abstract class StabilityLevel(
                                             val ordinal: Int,
                                             val name: String
                                             ) extends Ordered[StabilityLevel] {

    def compare( that: StabilityLevel ) = this.ordinal - that.ordinal

    override def toString = name
  }

  case object ALL extends StabilityLevel( 4, "All" )

  case object INTERNAL extends StabilityLevel( 3, "Internal" )

  case object INCUBATE extends StabilityLevel( 2, "Incubate" )

  case object PUBLIC extends StabilityLevel( 1, "Public" )

  val levels: Seq[StabilityLevel] = Seq(ALL, INTERNAL, INCUBATE, PUBLIC)

  def fromName( name: String ): Option[StabilityLevel] = {
    levels.find( level => level.name.equalsIgnoreCase( name ) )
  }

  def fromNames( names: Seq[String] ): Option[Seq[StabilityLevel]] = {
    Option( levels.filter( level => names.contains( level.name ) ) )
  }

}
