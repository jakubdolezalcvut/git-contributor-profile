package cz.dolezal.gitcontributorprofile.domain

@JvmInline
internal value class LastDays(
    val value: Int,
) {
    init {
        require(value in AnalysisConfig.Days.MIN..AnalysisConfig.Days.MAX) {
            "LastDays value must be between ${AnalysisConfig.Days.MIN} and ${AnalysisConfig.Days.MAX}, was $value"
        }
    }
}
