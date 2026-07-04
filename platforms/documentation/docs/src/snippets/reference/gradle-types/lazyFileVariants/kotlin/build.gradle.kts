plugins {
    java
}

// tag::lazy-javadoc-destination[]
tasks.named<Javadoc>("javadoc") {
    // Lazy variant: wire from another provider without forcing eager evaluation.
    destinationDirectory = layout.buildDirectory.dir("docs/javadoc")
}
// end::lazy-javadoc-destination[]

tasks.register("verifyJavadocDestination") {
    val destination = tasks.named<Javadoc>("javadoc").flatMap { it.destinationDirectory }
    doLast {
        println("javadoc destination: ${destination.get().asFile.toRelativeString(projectDir)}")
    }
}
