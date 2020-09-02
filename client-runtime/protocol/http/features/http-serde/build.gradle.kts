description = "HTTP Serialization feature for Smithy services generated by smithy-kotlin"
extra["displayName"] = "Smithy :: Kotlin :: HTTP Serde"
extra["moduleName"] = "software.aws.clientrt.http.feature"

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                api(project(":client-runtime:protocol:http"))
                api(project(":client-runtime:serde"))
            }
        }

        commonTest {
            dependencies {
                implementation(project(":client-runtime:testing"))
                // for concrete provider
                implementation(project(":client-runtime:serde:serde-json"))
                implementation(project(":client-runtime:serde:serde-xml"))
            }
        }

    }
}