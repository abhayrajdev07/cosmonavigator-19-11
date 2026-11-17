
package com.cosmotrinity.cosmonavigator.ui.planner

data class UpscSubjectCategory(
    val title: String,
    val description: String,
    val subjects: List<String>
)

object UpscSubjectCatalog {

    val categories = listOf(

        UpscSubjectCategory(
            title = "GS Paper I",
            description = "Indian Heritage, History, Society and Geography",
            subjects = listOf(
                "Ancient History",
                "Medieval History",
                "Modern History",
                "Post-Independence",
                "World History",
                "Art & Culture",
                "Indian Society",
                "World Geography"
            )
        ),

        UpscSubjectCategory(
            title = "GS Paper II",
            description = "Polity, Governance and International Relations",
            subjects = listOf(
                "Indian Polity",
                "Governance",
                "International Relations",
                "Social Justice"
            )
        ),

        UpscSubjectCategory(
            title = "GS Paper III",
            description = "Economy, Science, Environment and Security",
            subjects = listOf(
                "General Science & Technology",
                "Indian Economy",
                "Environment & Ecology",
                "Internal Security",
                "Disaster Management"
            )
        ),

        UpscSubjectCategory(
            title = "GS Paper IV",
            description = "Ethics, Integrity and Aptitude",
            subjects = listOf(
                "Ethics, Integrity & Aptitude"
            )
        ),

        UpscSubjectCategory(
            title = "CSAT",
            description = "Civil Services Aptitude Test",
            subjects = listOf(
                "Comprehension",
                "Logical Reasoning",
                "Analytical Ability",
                "Basic Numeracy",
                "Data Interpretation",
                "Decision Making"
            )
        ),

        UpscSubjectCategory(
            title = "Essay",
            description = "Essay Paper",
            subjects = listOf(
                "Essay Preparation"
            )
        ),

        UpscSubjectCategory(
            title = "Optional Subject",
            description = "Two optional papers",
            subjects = listOf(
                "Optional Paper I",
                "Optional Paper II"
            )
        ),

        UpscSubjectCategory(
            title = "Current Affairs",
            description = "Integrated current affairs preparation",
            subjects = listOf(
                "Daily Current Affairs",
                "Monthly Current Affairs",
                "Government Schemes",
                "Reports & Indices"
            )
        ),

        UpscSubjectCategory(
            title = "Language Papers",
            description = "Qualifying language papers",
            subjects = listOf(
                "Indian Language Hindi (Paper A)",
                "English Language (Paper B)"
            )
        )
    )
}
