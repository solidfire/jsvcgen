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

import de.johoop.jacoco4sbt._

import JacocoPlugin._

jacoco.settings

Keys.fork              in jacoco.Config := false

parallelExecution      in jacoco.Config := false

jacoco.outputDirectory in jacoco.Config := file("target/jacoco")

jacoco.reportFormats   in jacoco.Config := Seq( XMLReport(encoding = "utf-8"),
                                                ScalaHTMLReport(withBranchCoverage = true))

jacoco.includes        in jacoco.Config := Seq("com/solidfire/**")

