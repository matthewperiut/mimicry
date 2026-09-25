import java.io.File

/** Compares Minecraft versions like "1.20.1" and "26.3" numerically, part by part. */
fun compareVersions(a: String, b: String): Int {
    val x = a.split('.', '-').map { it.toIntOrNull() ?: 0 }
    val y = b.split('.', '-').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(x.size, y.size)) {
        val c = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
        if (c != 0) return c
    }
    return 0
}

/**
 * Overlay directories under src/main/overlays are named for the newest version they apply to. A version gets every overlay
 * named at or above it, newest first, so the one closest to it is copied last and wins.
 */
fun overlaysFor(root: File, version: String): List<File> = root.resolve("src/main/overlays").listFiles().orEmpty()
    .filter { it.isDirectory && compareVersions(version, it.name) <= 0 }
    .sortedWith { a, b -> compareVersions(b.name, a.name) }
