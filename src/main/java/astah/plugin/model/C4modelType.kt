package astah.plugin.model

enum class C4modelType(val stereotypeName: String) {
    PERSON("Person"),
    SOFTWARE_SYSTEM("Software System"),
    CONTAINER("Container"),
    COMPONENT("Component");

    companion object {
        fun obtainType(s: String?): C4modelType? {
            return s?.let {
                when (it) {
                    PERSON.stereotypeName -> PERSON
                    SOFTWARE_SYSTEM.stereotypeName -> SOFTWARE_SYSTEM
                    CONTAINER.stereotypeName -> CONTAINER
                    COMPONENT.stereotypeName -> COMPONENT
                    else -> null
                }
            }
        }
    }
}

enum class ValueType(val valueName: String, val indent: Double? = null, val yPosition: Double? = null) {
    NAME("NAME", 24.0, 0.0),
    STEREOTYPE("stereotype", 30.0, 0.12),
    DESCRIPTION("description", 5.0, 0.25),
    CONTAINER_TECHNOLOGY("technology"),
    COMPONENT_TECHNOLOGY("technology"),
    DEFINITION("definition")
}