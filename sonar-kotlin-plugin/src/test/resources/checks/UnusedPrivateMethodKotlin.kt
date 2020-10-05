class UnusedPrivateMethodKotlinCheckTest() {
    private fun unused() {} // Noncompliant

    // Serializable method should not raise any issue in Kotlin.
    private fun writeObject() {}
    private fun readObject() {}
    private fun writeReplace() {}
    private fun readResolve() {}
    private fun readObjectNoData() {}
}
