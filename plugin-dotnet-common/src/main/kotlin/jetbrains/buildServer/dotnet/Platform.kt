package jetbrains.buildServer.dotnet

enum class Platform(val id: String, val description: String) {
    x86("x86", "x86"),
    x64("x64", "x64"),
    ARM("ARM", "ARM");

    companion object {
        fun tryParse(id: String): Platform? {
            return Platform.values().singleOrNull()
        }
    }
}