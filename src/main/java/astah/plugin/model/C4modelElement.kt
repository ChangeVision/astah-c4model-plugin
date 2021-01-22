package astah.plugin.model

import astah.plugin.black
import astah.plugin.white

sealed class C4modelElement(val className: String, val description: String) {
    abstract val offset: Double
    abstract val textColor: String
}

class Person(className: String, description: String): C4modelElement(className, description) {
    override val offset = 0.4
    override val textColor = white
}
class SoftwareSystem(className: String, description: String): C4modelElement(className, description) {
    override val offset = 0.2
    override val textColor = white
}

class Container(className: String, description: String, val containerTechnology: String):
    C4modelElement(className, description) {
    override val offset = 0.2
    override val textColor = white
}

class Component(className: String, description: String, val componentTechnology: String):
        C4modelElement(className, description) {
    override val offset = 0.2
    override val textColor = black
}