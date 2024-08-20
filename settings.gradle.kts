rootProject.name = "ObjectSign"
include("annotation")
include("ksp")
buildscript{
    dependencies{
        classpath("io.github.cloak-box.plugin:maven-api-plugin:1.0.0.2")
    }
    repositories{
        mavenLocal()
        mavenCentral()
        google()
    }
}