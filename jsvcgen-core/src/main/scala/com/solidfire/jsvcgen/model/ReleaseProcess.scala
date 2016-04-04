/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/
package com.solidfire.jsvcgen.model

import com.sun.tracing.dtrace.StabilityLevel

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

  case object INTERNAL extends StabilityLevel( 3, "Internal" )

  case object INCUBATE extends StabilityLevel( 2, "Incubate" )

  case object PUBLIC extends StabilityLevel( 1, "Public" )

  case object ALL extends StabilityLevel( 0, "All" )

  val levels: Seq[StabilityLevel] = Seq(INTERNAL, INCUBATE, PUBLIC, ALL)

  def fromName( name: String ): Option[StabilityLevel] = {
    levels.find( level => level.name.equalsIgnoreCase( name ) )
  }

  def fromNames( names: Seq[String] ): Option[Seq[StabilityLevel]] = {
    val found = levels.filter( level => names.exists(n => n.toLowerCase == level.name.toLowerCase))
    if (found.nonEmpty)
      Some(found)
    else
      None
  }

  def highestOrdinal(levels: Seq[StabilityLevel]): StabilityLevel =  levels.maxBy(_.ordinal)
}
