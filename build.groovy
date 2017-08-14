import groovy.text.*
import groovy.json.*
import java.util.stream.Collectors

///////////////////////////
// Hjelpemetoder
///////////////////////////
def debug(Object o) {
    if (Boolean.getBoolean("debug")) {
        println o
    }
    return o
}

String shell(String cmd, File dir, boolean ikkePipeOutput) {
    def absoluteDir = (dir ?: new File(".")).getAbsoluteFile()
    def process = cmd.execute(null, absoluteDir)
    println "${cmd} [${absoluteDir}]"

    def out = ikkePipeOutput ? new ByteArrayOutputStream() : System.out
    def err = ikkePipeOutput ? new ByteArrayOutputStream() : System.err
    process.waitForProcessOutput(out, err)
    def result = process.waitFor()
    if (result != 0) {
        if (ikkePipeOutput) {
            println out.toString()
            println err.toString()
        }
        throw new RuntimeException("[${cmd}] in [${absoluteDir}] failed!")
    }
    if (ikkePipeOutput) {
        return out.toString()
    } else {
        return null
    }
}

def shell(String cmd, File dir) {
    shell(cmd, dir, false)
}

def shell(String cmd) {
    shell(cmd, null, false)
}

def mvnExecutable() {
    try {
        "mvn -v".execute()
        return "mvn"
    } catch (Exception e) {
        return "mvn.cmd" // windows
    }
}

mvn = mvnExecutable()

String evaluateMavenExpression(String expression, File directory) {
    def output = shell "${mvn} org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=${expression}", directory, true
    def resultat = Arrays.stream(output.split("\n"))
            .map({ line -> debug(line) })
            .map({ line -> line.trim() })
            .filter({ line -> !line.startsWith("[") }) // fjern log-linjer
            .reduce({ a, b -> b })  // ta siste linje
            .get()
    println "${expression} = ${resultat}"
    return resultat
}


def steg(String stegNavn, Runnable runnable) {
    println "\n\n===============================\n${stegNavn}\n==============================="
    runnable.run()
    println "\n\n"
}

///////////////////////////
// Start script
///////////////////////////
releaseRepositoryName = "release"
morPomRepositoryName = "mor-pom"

def mvn = mvnExecutable()
def version = new Date().format("yyyy.MM.dd.kk.mm")
def snapshotVersjon = "1-SNAPSHOT"
def arvetVersjonForCommonBibliotek = "\${project.parent.version}"
def modules = ""

// clone og analyser mor-pom
def morPomScmUrl = "ssh://git@stash.devillo.no:7999/common/${morPomRepositoryName}.git"

def morPomDirectory = new File(morPomRepositoryName)
if (!morPomDirectory.exists() || !new File(morPomDirectory, ".git").exists()) {
    shell "git clone ${morPomScmUrl} ${morPomDirectory.absolutePath}"
}
def morPomVersjon = evaluateMavenExpression "project.version", morPomDirectory
def morPomArtifactId = evaluateMavenExpression "project.artifactId", morPomDirectory
def morPomGroupId = evaluateMavenExpression "project.groupId", morPomDirectory
def morPomId = "${morPomGroupId}:${morPomArtifactId}:${morPomVersjon}"
println "mor-pom: ${morPomId}"
if (morPomVersjon != snapshotVersjon) {
    throw new IllegalStateException("mor-pom har versjon [${morPomVersjon}] - forventet [${snapshotVersjon}]")
}

def findLocalRepoDirectory(String repoName) {
    switch (repoName) {
        case releaseRepositoryName:
            return new File(".")
        case morPomRepositoryName:
            return new File(morPomRepositoryName)
        default:
            return new File(morPomRepositoryName, repoName)
    }
}

// analyser andre repoer
def fellesRepositories = new JsonSlurper().parse(new URL("http://stash.devillo.no/rest/api/1.0/projects/common/repos?limit=100"))
def repos = fellesRepositories.values.stream().map({ repo ->
    return [
            name: repo.name,
            localRepoDirectory: findLocalRepoDirectory(repo.name),
            links: repo.links
    ]
}).collect(Collectors.toList())
repos.forEach { repo ->
    String repoName = repo.name
    if (repoName == releaseRepositoryName || repoName == morPomRepositoryName) {
        return
    }

    steg "setup - ${repoName}", {
        def localRepoDirectory = repo.localRepoDirectory

        if (!localRepoDirectory.exists()) {
            repo.links.clone.forEach { cloneLink ->
                if (cloneLink.name == "ssh") {
                    def href = cloneLink.href
                    shell "git clone ${href} ${localRepoDirectory.absolutePath}"
                }
            }
        } else {
            shell"git pull --ff-only origin master", localRepoDirectory
        }

        println "sjekker at ${repoName} er koblet til riktig mor-pom (${morPomId})"
        def pomXml = new XmlSlurper().parse(new File(localRepoDirectory, "pom.xml"))
        def repoVersion = pomXml.version
        def parentGroupId = pomXml.parent.groupId
        def parentArtifactId = pomXml.parent.artifactId
        def parentVersion = pomXml.parent.version

        if (repoVersion != arvetVersjonForCommonBibliotek) {
            throw new IllegalStateException("${repoName} arver ikke versjon fra mor-pom på riktig måte: [${repoVersion}] - forventet: [${arvetVersjonForCommonBibliotek}]")
        }

        def parentId = "${parentGroupId}:${parentArtifactId}:${parentVersion}"
        if (parentId != morPomId) {
            throw new IllegalStateException("${repoName} er ikke koblet riktig til mor-pom: [${parentId}] - forventet: [${morPomId}]")
        }

        // align alle versjoner til mor-pommen sin versjon slik at vi kan oppdatere alt samlet senere
        shell "${mvn} versions:set -DnewVersion=${morPomVersjon} -U", localRepoDirectory

        // brukes i generert pom.xml
        modules += "\t\t<module>${morPomRepositoryName}/${repoName}</module>\n"
    }
}

// lag release-pom som knytter alt sammen
steg "lag pom.xml", {
    def template = new SimpleTemplateEngine().createTemplate(new File("pom.template.xml"))
    def writable = template.make([
            modules: modules,
            version: version
    ])

    def pomXml = new File("pom.xml")
    pomXml.delete()
    pomXml << writable
}


// alle repoer er nå ok, da kan vi oppdatere versjonen til alle prosjektene:
steg "oppdater versjon", {

    // oppdaterer alt til ny versjon
    shell "${mvn} versions:set -DnewVersion=${version}", morPomDirectory

    // oppdater common.version i bommen slik at denne er låst
    def bomDirectory = new File(morPomDirectory, "bom")
    def pomXmlFile = new File(bomDirectory, "pom.xml")
    def pomXml = new XmlSlurper().parse(pomXmlFile)
    pomXmlFile.text = pomXmlFile.text.replaceAll('<common.version>.*</common.version>', "<common.version>${pomXml.parent.version}</common.version>")
}

// prosjektet er nå klart for bygg, test og deploy!
steg "mvn verify", {
    shell "${mvn} verify -DskipTests"
}

// TODO test når disse er stabilisert!
//steg "mvn test", {
//    shell "${mvn} test"
//}

steg "mvn deploy", {
    shell "${mvn} deploy -DskipTests"
}

steg "git tag", {
    repos.forEach { repo ->
        def localRepoDirectory = repo.localRepoDirectory
        shell "git tag -a ${version} -m ${version}", localRepoDirectory
        shell "git push --tags", localRepoDirectory
    }
}

// TODO stash notification
