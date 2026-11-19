package no.nav.common.build

import java.io.File

internal fun File.fixtureCatalogVersion(alias: String): String {
    val entry = useLines { lines ->
        lines
            .dropWhile { it.trim() != "[versions]" }
            .drop(1)
            .takeWhile { !it.trim().startsWith("[") }
            .singleOrNull { it.substringBefore('=').trim() == alias }
    } ?: error("Mangler versjonen '$alias' i $path")
    val value = entry.substringAfter('=').trim()
    require(value.length >= 2 && value.first() == '"' && value.last() == '"') {
        "Forventet en strengversjon for '$alias' i $path"
    }
    return value.removeSurrounding("\"")
}
