data class C(val a: Int?) {
    fun function_1(): Int { // Add no sonar comment here and update tests when available
        val b = 0
        return if (((a ?: b) + 1) > 3) 0 else if (true) 1 else -1
    }

    fun function_2() {
        /* multiline comment
        */
    }
}
