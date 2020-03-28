package model

/**
 * The original FiTFactory encoded priorities as a plain integer. I chose against this because an integer carries
 * absolutely no usable information about the actual use case.
 * Representing priorities as an enum also eliminates meaningless values such as having a priority of 1000.
 */
enum class Priority {
    VeryLow,
    Low,
    Medium,
    High,
    VeryHigh
}