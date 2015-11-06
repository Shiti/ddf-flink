import sbt._
import sbt.Keys._


object Common {

  lazy val rootOrganization = "io"

  lazy val rootProjectName = "ddf"

  lazy val ddfVersion = "1.4.0-SNAPSHOT"

  lazy val theScalaVersion = "2.10.4"

  lazy val flinkVersion = "1.0-SNAPSHOT"

  lazy val submodulePom = (
    <!--
      **************************************************************************************************
      IMPORTANT: This file is generated by "sbt make-pom" (bin/make-poms.sh). Edits will be overwritten!
      **************************************************************************************************
      -->
      <parent>
        <groupId>{rootOrganization}</groupId>
        <artifactId>{rootProjectName}</artifactId>
        <version>{ddfVersion}</version>
      </parent>
      <build>
        <plugins>
          <plugin>
            <groupId>net.alchim31.maven</groupId>
            <artifactId>scala-maven-plugin</artifactId>
            <version>3.1.5</version>
            <configuration>
              <recompileMode>incremental</recompileMode>
            </configuration>
          </plugin>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>2.7</version>
            <configuration>
              <skipTests>true</skipTests>
            </configuration>
          </plugin>
          <plugin>
            <groupId>org.scalatest</groupId>
            <artifactId>scalatest-maven-plugin</artifactId>
            <version>1.0</version>
            <configuration>
              <reportsDirectory>${{basedir}}/surefire-reports</reportsDirectory>
              <junitxml>.</junitxml>
              <filereports>scalatest_reports.txt</filereports>
            </configuration>
            <executions>
              <execution>
                <id>test</id>
                <goals>
                  <goal>test</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    )

  lazy val commonSettings = Seq(
    organization := "io.ddf",
    version := ddfVersion,
    retrieveManaged := true, // Do create a lib_managed, so we have one place for all the dependency jars to copy to slaves, if needed
    scalaVersion := theScalaVersion,
    scalacOptions := Seq("-unchecked", "-optimize", "-deprecation"),
    fork in Test := true,
    parallelExecution in ThisBuild := false,
    javaOptions in Test ++= Seq("-Xmx2g"),
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    resolvers ++= Seq(Resolver.mavenLocal,
      "Adatao Mvnrepos Snapshots" at "https://raw.github.com/adatao/mvnrepos/master/snapshots",
      "Adatao Mvnrepos Releases" at "https://raw.github.com/adatao/mvnrepos/master/releases")
  )

}

