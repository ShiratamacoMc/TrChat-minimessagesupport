repositories {
    maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:module-adventure"))
    compileOnly(project(":project:module-chat"))
    compileOnly(project(":project:module-compat"))
    compileOnly(project(":project:module-nms"))
    compileOnly("ink.ptms.core:v12105:12105:universal")
    compileOnly("net.md-5:bungeecord-chat:1.21-R0.3")
    compileOnly(fileTree(rootDir.resolve("libs")))

    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }
    compileOnly("com.discordsrv:discordsrv:1.26.0") { isTransitive = false }
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.24.0")
}

taboolib { subproject = true }