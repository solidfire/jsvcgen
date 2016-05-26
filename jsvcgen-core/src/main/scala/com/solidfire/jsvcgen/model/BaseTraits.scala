/*
 * Copyright &copy 2014-2016 NetApp, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.solidfire.jsvcgen.model

/**
  * Created by Jason Ryan Womack on 5/26/16.
  */

trait Doc {
  val documentation: Option[Documentation]
}

trait NamedWithDoc extends Doc {
  val name: String
}

trait ValuedWithDoc extends Doc {
  val value: String
}

trait Attribute extends NamedWithDoc {
  val since     : Option[String]
  val deprecated: Option[Deprecated]
}

trait Typed extends NamedWithDoc {
  val typeUse: TypeUse
}

