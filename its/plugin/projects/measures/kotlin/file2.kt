data class C(val a: Int?) {
    fun function_1(): Int { // Add no sonar comment here and update tests when available
        val b = 0
        return (a ?: b) + 1
    }

    fun function_2() {
        /* multiline comment
        */
    }
}
