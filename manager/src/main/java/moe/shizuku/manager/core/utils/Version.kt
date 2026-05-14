package moe.shizuku.manager.core.utils

data class Version(
    private val major: Int,
    private val minor: Int,
    private val patch: Int = 0
) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(
            this,
            other,
            { it.major },
            { it.minor },
            { it.patch }
        )

    override fun toString(): String =
        "$major.$minor.$patch"

    companion object {
        fun parse(tag: String): Version? {
            val regex = Regex("""v?(\d+)\.(\d+)\.(\d+)""")
            val match = regex.find(tag) ?: return null
            val (major, minor, patch) = match.destructured
            return Version(
                major.toInt(),
                minor.toInt(),
                patch.toInt()
            )
        }
    }
}