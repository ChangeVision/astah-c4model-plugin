package astah.plugin

import astah.plugin.model.*
import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.presentation.PresentationPropertyConstants.Key.NOTATION_TYPE
import com.change_vision.jude.api.inf.presentation.PresentationPropertyConstants.Value.NOTATION_TYPE_CUSTOMIZED
import java.awt.Point
import java.awt.geom.Point2D
import javax.swing.JButton

enum class ReferenceSource {
  TEXT_NODE, MODEL
}

class TextId(val label: String, val iTextId: String)

class C4modelIcon(val id: String, val textIds: List<TextId>)
class C4modelIconData(val presentations: List<C4modelIcon>)

class C4modelRectangle(val classId: String,
                       val boundaryNameTextId: TextId, val stereotypeTextId: TextId, val iRectId: String)
class C4modelRectangleData(val rectangles: List<C4modelRectangle>)

const val C4MODEL_TAG_KEY = "c4model"

class ButtonRefresh(label: String): JButton(label) {
    fun push(diagram: IDiagram?, referenceSource: ReferenceSource) {
        if (diagram == null || diagram !is IClassDiagram)
            return
        val allNodePresentation = diagram.presentations.filterIsInstance<INodePresentation>()
        val allLinkPresentation = diagram.presentations.filterIsInstance<ILinkPresentation>()
        val allClasses = allNodePresentation.filter { it.type == "Class" }
        val allTexts = allNodePresentation.filter { it.type == "Text" }
        val allRectangles = allNodePresentation.filter { it.type == "Rectangle" }
        val allRelationPresentations =
            allLinkPresentation.filter { it.type == "Dependency" || it.type == "Association" }
        val allC4modelClassPresentations = allClasses.filter { clazz ->
            val model = clazz.model
            if (model is IClass) {
                model.stereotypes.any { stereotype ->
                    C4modelType.values().map { it.stereotypeName }.contains(stereotype)
                }
            } else {
                false
            }
        }
        allC4modelClassPresentations.forEach { presentation ->
            refreshNode(presentation, diagram, allTexts, referenceSource)
        }
        allRelationPresentations.forEach { presentation ->
            refreshRelation(presentation, diagram, allTexts, referenceSource)
        }
        refreshBoundary(diagram, allRectangles, allTexts)
    }

    private fun refreshNode(
        presentation: INodePresentation, diagram: IClassDiagram,
        allTexts: List<INodePresentation>, referenceSource: ReferenceSource
    ) {
        fun createPresentation(
            locationX: Double, locationY: Double, width: Double, height: Double,
            element: C4modelElement, description: String, presentation: INodePresentation,
            type: C4modelType, c4ModelIconData: C4modelIconData
        ) {
            val model = presentation.model as IClass
            val namePresentation = AstahAccessor.createText(
                Point(
                    (locationX + ValueType.NAME.indent!!).toInt(),
                    (locationY + height * (element.offset + ValueType.NAME.yPosition!!)).toInt()
                ),
                width - ValueType.NAME.indent * 2, 24.0, model.name, element.textColor, diagram
            )
            val stereotypePresentation = AstahAccessor.createText(
                Point(
                    (locationX + ValueType.STEREOTYPE.indent!!).toInt(),
                    (locationY + height * (element.offset + ValueType.STEREOTYPE.yPosition!!)).toInt()
                ),
                width - ValueType.STEREOTYPE.indent * 2, 13.0, "[${type.stereotypeName}" +
                        if (element is Container && element.containerTechnology != "")
                            ": " + element.containerTechnology + "]"
                        else if (element is Component && element.componentTechnology != "")
                            ": " + element.componentTechnology + "]"
                        else "" + "]", element.textColor, diagram
            )
            val descriptionPresentation =
                AstahAccessor.createText(
                    Point(
                        (locationX + ValueType.DESCRIPTION.indent!!).toInt(),
                        (locationY + height * (element.offset + ValueType.DESCRIPTION.yPosition!!)).toInt()
                    ),
                    width - ValueType.DESCRIPTION.indent * 2, 40.0, description, element.textColor, diagram
                )
            if (namePresentation != null && descriptionPresentation != null && stereotypePresentation != null) {
                val textIds = mutableListOf<TextId>()
                textIds.add(TextId(ValueType.NAME.valueName, namePresentation.id))
                textIds.add(TextId(ValueType.STEREOTYPE.valueName, stereotypePresentation.id))
                textIds.add(TextId(ValueType.DESCRIPTION.valueName, descriptionPresentation.id))
                val c4modelIPresentations = mutableListOf(C4modelIcon(presentation.id, textIds))
                c4modelIPresentations.addAll(c4ModelIconData.presentations)
                AstahAccessor.writeTaggedValue(
                    model, C4MODEL_TAG_KEY,
                    JsonSaveDataConverter.convertFromC4modelIconToJSON(C4modelIconData(c4modelIPresentations))
                )
            }
        }

        val model = presentation.model
        if (model !is IClass)
            return
        C4modelType.obtainType(model.stereotypes.first())?.let { type ->
            AstahAccessor.setNotationType(presentation, NOTATION_TYPE_CUSTOMIZED)
            val name = model.name
            val description = AstahAccessor.getOrCreateInitialValue(
                model,
                ValueType.DESCRIPTION.valueName, "(undefined)"
            )
            val element = when (type) {
                C4modelType.PERSON -> Person(name, description)
                C4modelType.SOFTWARE_SYSTEM -> SoftwareSystem(name, description)
                C4modelType.COMPONENT ->
                    Component(
                        name, description, AstahAccessor.getOrCreateInitialValue(
                            model, ValueType.COMPONENT_TECHNOLOGY.valueName, "(technology)"
                        )
                    )
                C4modelType.CONTAINER ->
                    Container(
                        name, description, AstahAccessor.getOrCreateInitialValue(
                            model, ValueType.CONTAINER_TECHNOLOGY.valueName, "(technology)"
                        )
                    )
            }
            val locationX = presentation.location.x
            val locationY = presentation.location.y
            val width = presentation.width
            val height = presentation.height
            val taggedValue = AstahAccessor.readTaggedValue(model, C4MODEL_TAG_KEY)
            if (taggedValue != null) {
                val c4modelPresentationData = JsonSaveDataConverter.convertFromJsonToC4modelIcon(taggedValue)
                val c4modelIPresentation = c4modelPresentationData.presentations.firstOrNull {
                    it.id == presentation.id
                }
                if (c4modelIPresentation != null) {
                    c4modelIPresentation.textIds.forEach { textId ->
                        allTexts.firstOrNull { it.id == textId.iTextId }?.let { textNode ->
                            when (textId.label) {
                                ValueType.NAME.valueName -> {
                                    when (referenceSource) {
                                        ReferenceSource.MODEL -> AstahAccessor.setText(textNode, name)
                                        ReferenceSource.TEXT_NODE ->
                                            AstahAccessor.setClassName(model, textNode.label)
                                    }
                                    val x = locationX + (width / 2) - (textNode.width / 2)
                                    val y = locationY + height * (element.offset + ValueType.NAME.yPosition!!)
                                    AstahAccessor.setNodeLocation(textNode, Point(x.toInt(), y.toInt()))
                                }
                                ValueType.STEREOTYPE.valueName -> {
                                    when (referenceSource) {
                                        ReferenceSource.MODEL ->
                                            AstahAccessor.setText(
                                                textNode, "[${type.stereotypeName}" +
                                                        if (element is Container &&
                                                            element.containerTechnology != ""
                                                        )
                                                            ": " + element.containerTechnology + "]"
                                                        else if (element is Component &&
                                                            element.componentTechnology != ""
                                                        )
                                                            ": " + element.componentTechnology + "]"
                                                        else "" + "]"
                                            )
                                        ReferenceSource.TEXT_NODE ->
                                            if (element is Container || element is Component) {
                                                val stereotypeAndTechnology = textNode.label.split(":")
                                                if (stereotypeAndTechnology.isEmpty()) {
                                                    AstahAccessor.setInitialValue(
                                                        model,
                                                        "technology", " "
                                                    )
                                                } else {
                                                    AstahAccessor.setInitialValue(
                                                        model,
                                                        "technology",
                                                        stereotypeAndTechnology[1].split("]")[0].trim()
                                                    )
                                                }
                                            }
                                    }
                                    val x = locationX + (width / 2) - (textNode.width / 2)
                                    val y = locationY + height * (element.offset + ValueType.STEREOTYPE.yPosition!!)
                                    AstahAccessor.setNodeLocation(textNode, Point(x.toInt(), y.toInt()))
                                }
                                ValueType.DESCRIPTION.valueName -> {
                                    when (referenceSource) {
                                        ReferenceSource.MODEL -> AstahAccessor.setText(textNode, description)
                                        ReferenceSource.TEXT_NODE -> {
                                            AstahAccessor.setInitialValue(
                                                model, ValueType.DESCRIPTION.valueName, textNode.label
                                            )
                                        }
                                    }
                                    val x = locationX + ValueType.DESCRIPTION.indent!!
                                    val y =
                                        locationY + height * (element.offset + ValueType.DESCRIPTION.yPosition!!)
                                    AstahAccessor.setNodeLocation(textNode, Point(x.toInt(), y.toInt()))
                                }
                            }
                        }
                    }
                } else {
                    createPresentation(
                        locationX, locationY, width, height,
                        element, description, presentation, type, c4modelPresentationData
                    )
                }
            } else {
                createPresentation(
                    locationX, locationY, width, height,
                    element, description, presentation, type, C4modelIconData(listOf())
                )
            }
        }
    }

    private fun refreshRelation(
        presentation: ILinkPresentation, diagram: IClassDiagram,
        allTexts: List<INodePresentation>, referenceSource: ReferenceSource
    ) {
        fun createPresentation(
            midPoint: Point2D, definition: String,
            presentation: ILinkPresentation, c4ModelIconData: C4modelIconData
        ) {
            val definitionPresentation = AstahAccessor.createText(
                midPoint, 100.0, 24.0, definition, black, diagram, white
            )
            if (definitionPresentation != null) {
                val textIds = mutableListOf<TextId>()
                textIds.add(TextId(ValueType.DEFINITION.valueName, definitionPresentation.id))
                val c4modelIPresentations = mutableListOf(C4modelIcon(presentation.id, textIds))
                c4modelIPresentations.addAll(c4ModelIconData.presentations)
                AstahAccessor.writeTaggedValue(
                    presentation.model, C4MODEL_TAG_KEY,
                    JsonSaveDataConverter.convertFromC4modelIconToJSON(C4modelIconData(c4modelIPresentations))
                )
            }
        }

        val model = presentation.model
        if ((model is IAssociation || model is IDependency) && model is INamedElement) {
            val definition = if (model.definition != "") model.definition else "(undefined)"
            val startPoint = presentation.points.firstOrNull()
            val endPoint = presentation.points.lastOrNull()
            if (startPoint != null && endPoint != null) {
                val midPoint = Point(
                    ((startPoint.x + endPoint.x) / 2).toInt(),
                    ((startPoint.y + endPoint.y) / 2).toInt()
                )
                val taggedValue = AstahAccessor.readTaggedValue(model, C4MODEL_TAG_KEY)
                if (taggedValue != null) {
                    val c4modelPresentationData = JsonSaveDataConverter.convertFromJsonToC4modelIcon(taggedValue)
                    val c4modelIPresentation = c4modelPresentationData.presentations.firstOrNull {
                        it.id == presentation.id
                    }
                    if (c4modelIPresentation != null) {
                        c4modelIPresentation.textIds.forEach { textId ->
                            allTexts.firstOrNull { it.id == textId.iTextId }?.let { textNode ->
                                when (referenceSource) {
                                    ReferenceSource.MODEL -> AstahAccessor.setText(textNode, definition)
                                    ReferenceSource.TEXT_NODE -> AstahAccessor.setDefinition(model, textNode.label)
                                }
                                val x = midPoint.x - (textNode.width / 2)
                                val y = midPoint.y - (textNode.height / 2)
                                AstahAccessor.setNodeLocation(textNode, Point(x.toInt(), y.toInt()))
                            }
                        }
                    } else {
                        createPresentation(midPoint, definition, presentation, c4modelPresentationData)
                    }
                } else {
                    createPresentation(midPoint, definition, presentation, C4modelIconData(listOf()))
                }
            }
        }
    }

    private fun refreshBoundary(
        diagram: IClassDiagram,
        allRectangles: List<INodePresentation>, allTexts: List<INodePresentation>
    ) {
        class Boundary(
            val boundaryNameText: INodePresentation?, val stereotypeText: INodePresentation?,
            val rectangle: INodePresentation?
        )

        fun updateOrCreateBoundary(
            brotherOrSisterPresentations: List<Pair<INodePresentation, IClass>>,
            boundaryName: String, stereotype: String,
            iBoundaryNameText: INodePresentation? = null,
            iStereotypeText: INodePresentation? = null,
            iRectangle: INodePresentation? = null
        ): Boundary {
            val minXPresentation = brotherOrSisterPresentations.minBy { it.first.location.x }
            val minYPresentation = brotherOrSisterPresentations.minBy { it.first.location.y }
            if (minXPresentation != null && minYPresentation != null) {
                val x = minXPresentation.first.location.x - 5
                val y = minYPresentation.first.location.y - 5
                val widthPresentation =
                    brotherOrSisterPresentations.maxBy { it.first.location.x + it.first.width }
                val heightPresentation =
                    brotherOrSisterPresentations.maxBy { it.first.location.y + it.first.height }
                if (widthPresentation != null && heightPresentation != null) {
                    val width = widthPresentation.first.location.x + widthPresentation.first.width - x + 10
                    val height = heightPresentation.first.location.y + heightPresentation.first.height - y + 40 + 5
                    val newRectangle = if (iRectangle != null) {
                        AstahAccessor.updateRectangle(Point(x.toInt(), y.toInt()), width, height, iRectangle)
                        iRectangle
                    } else {
                        AstahAccessor.createRectangle(Point(x.toInt(), y.toInt()), width, height, diagram)
                    }
                    val newBoundaryNameText = if (iBoundaryNameText != null) {
                        AstahAccessor.updateText(
                            Point(x.toInt(), (y + height - 40).toInt()),
                            boundaryName.length * 16.0, 20.0, boundaryName, black, iBoundaryNameText
                        )
                        iBoundaryNameText
                    } else {
                        AstahAccessor.createText(
                            Point(x.toInt(), (y + height - 40).toInt()),
                            boundaryName.length * 16.0, 20.0, boundaryName, black, diagram
                        )
                    }
                    val newStereotypeText = if (iStereotypeText != null) {
                        AstahAccessor.updateText(
                            Point(x.toInt(), (y + height - 20).toInt()),
                            stereotype.length * 12.0, 10.0,
                            "[$stereotype]", black, iStereotypeText
                        )
                        iStereotypeText
                    } else {
                        AstahAccessor.createText(
                            Point(x.toInt(), (y + height - 20).toInt()),
                            stereotype.length * 12.0, 20.0, "[$stereotype]", black, diagram
                        )
                    }
                    return Boundary(newBoundaryNameText, newStereotypeText, newRectangle)
                }
            }
            return Boundary(null, null, null)
        }

        val taggedValue = AstahAccessor.readTaggedValue(diagram, C4MODEL_TAG_KEY)
        val newGroups = mutableListOf<C4modelRectangle>()
        if (taggedValue != null) {
            val storedGroups = JsonSaveDataConverter.convertFromJsonToC4modelRectangle(taggedValue)
            AstahAccessor.getSetsOfClassesUnderCommonParent(diagram).forEach { group ->
                val clazz = group.first
                val boundaryName = clazz.name
                val stereotypeName = clazz.stereotypes.firstOrNull() ?: ""
                val brotherOrSisterPresentations = group.second
                val correspondingRectangle = storedGroups.rectangles.firstOrNull { it.classId == clazz.id }
                if (correspondingRectangle == null) {
                    val newBoundary = updateOrCreateBoundary(brotherOrSisterPresentations, boundaryName, stereotypeName)
                    val newBoundaryNameText = newBoundary.boundaryNameText
                    val newStereotypeText = newBoundary.stereotypeText
                    val newRectangle = newBoundary.rectangle
                    if (newBoundaryNameText != null && newStereotypeText != null && newRectangle != null) {
                        newGroups.add(
                            C4modelRectangle(
                                clazz.id, TextId(boundaryName, newBoundaryNameText.id),
                                TextId(stereotypeName, newStereotypeText.id), newRectangle.id
                            )
                        )
                    }
                } else {
                    val correspondingBoundaryNameText = allTexts.firstOrNull {
                        it.id == correspondingRectangle.boundaryNameTextId.iTextId
                    }
                    val correspondingStereotypeText = allTexts.firstOrNull {
                        it.id == correspondingRectangle.stereotypeTextId.iTextId
                    }
                    val correspondingIRect = allRectangles.firstOrNull { it.id == correspondingRectangle.iRectId }
                    if (correspondingBoundaryNameText != null && correspondingStereotypeText != null &&
                        correspondingIRect != null
                    ) {
                        updateOrCreateBoundary(
                            brotherOrSisterPresentations, boundaryName, stereotypeName,
                            correspondingBoundaryNameText, correspondingStereotypeText, correspondingIRect
                        )
                        newGroups.add(
                            C4modelRectangle(
                                clazz.id,
                                TextId(boundaryName, correspondingBoundaryNameText.id),
                                TextId(stereotypeName, correspondingStereotypeText.id), correspondingIRect.id
                            )
                        )
                    }
                }
            }
        } else {
            AstahAccessor.getSetsOfClassesUnderCommonParent(diagram).forEach { group ->
                val clazz = group.first
                val boundaryName = clazz.name
                val stereotype = clazz.stereotypes.firstOrNull() ?: ""
                val boundary = updateOrCreateBoundary(group.second, boundaryName, stereotype)
                if (boundary.boundaryNameText != null && boundary.stereotypeText != null
                    && boundary.rectangle != null
                ) {
                    newGroups.add(
                        C4modelRectangle(
                            clazz.id, TextId(boundaryName, boundary.boundaryNameText.id),
                            TextId(stereotype, boundary.stereotypeText.id), boundary.rectangle.id
                        )
                    )
                }
            }
        }
        AstahAccessor.writeTaggedValue(
            diagram, C4MODEL_TAG_KEY,
            JsonSaveDataConverter.convertFromC4modelRectangleToJSON(C4modelRectangleData(newGroups))
        )
    }
}