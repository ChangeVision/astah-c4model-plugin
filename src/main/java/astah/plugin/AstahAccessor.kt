package astah.plugin

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.editor.TransactionManager
import com.change_vision.jude.api.inf.exception.InvalidEditingException
import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.presentation.PresentationPropertyConstants.Key.*
import com.change_vision.jude.api.inf.presentation.PresentationPropertyConstants.Value.*
import java.awt.geom.Point2D

const val white = "#FFFFFF"
const val black = "#000000"

object AstahAccessor {
    private val projectAccessor = AstahAPI.getAstahAPI().projectAccessor

    fun setNodeLocation(node: INodePresentation, location: Point2D) {
        try {
            TransactionManager.beginTransaction()
            node.location = location
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }

    fun setText(node: INodePresentation, text: String) {
        try {
            TransactionManager.beginTransaction()
            node.label = text
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }

    fun setClassName(model: IClass, name: String) {
        try {
            TransactionManager.beginTransaction()
            model.name = name
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }

    fun setInitialValue(model: IClass, valueName: String, initialValue: String) {
        try {
            TransactionManager.beginTransaction()
            model.attributes.filter { it.association == null }
                    .firstOrNull { it.name == valueName }?.initialValue = initialValue
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }

    fun setDefinition(element: INamedElement, definition: String) {
        try {
            TransactionManager.beginTransaction()
            element.definition = definition
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }


    fun setNodeSize(node: INodePresentation, width: Double, height: Double) {
        try {
            TransactionManager.beginTransaction()
            node.width = maxOf(node.width, width)
            node.height = maxOf(node.height, height)
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }

    fun setNotationType(node: INodePresentation, notationType: String) {
        if (node.model is IClass && (notationType == NOTATION_TYPE_CUSTOMIZED) ||
            (notationType == NOTATION_TYPE_ICON) || (notationType == NOTATION_TYPE_NORMAL)) {
            try {
                TransactionManager.beginTransaction()
                node.setProperty(NOTATION_TYPE, notationType)
                TransactionManager.endTransaction()
            } catch (e: Exception) {
                e.printStackTrace()
                if (TransactionManager.isInTransaction()) {
                    TransactionManager.abortTransaction()
                }
            }
        }
    }

    fun writeTaggedValue(element: IElement, tagKey : String , json: String) {
        try {
            TransactionManager.beginTransaction()
            val tag = getTaggedValue(element, tagKey, true)
            tag!!.value = json
            TransactionManager.endTransaction()
        } catch (e : Exception) {
            e.printStackTrace()
            if (TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
    }

    fun readTaggedValue(element: IElement, tagKey : String) : String? {
        try {
            getTaggedValue(element, tagKey, false)?.let { tag ->
                return tag.value
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getTaggedValue(element: IElement, tagKey: String, isCreateIfNotExist: Boolean) : ITaggedValue? {
        var ret: ITaggedValue? = null
        val isInTransaction = TransactionManager.isInTransaction()
        try {
            ret = element.taggedValues.firstOrNull { it.key.equals(tagKey, ignoreCase = true) }
            if (ret == null && isCreateIfNotExist) {
                if (!isInTransaction)
                    TransactionManager.beginTransaction()
                ret = if (projectAccessor.astahEdition == "SysML")
                    projectAccessor.modelEditorFactory.sysmlModelEditor.createTaggedValue(element, tagKey, "")
                else
                    projectAccessor.modelEditorFactory.basicModelEditor.createTaggedValue(element, tagKey, "")
                if (!isInTransaction)
                    TransactionManager.endTransaction()
            }
        } catch (exp: Exception) {
            exp.printStackTrace()
            if (!isInTransaction && TransactionManager.isInTransaction()) {
                TransactionManager.abortTransaction()
            }
        }
        return ret
    }

    fun createText(location: Point2D, width: Double, height: Double, text: String, fontColor: String,
                   diagram: IDiagram, backgroundColor: String? = null): INodePresentation? {
        val editor = projectAccessor.diagramEditorFactory.classDiagramEditor
        editor.diagram = diagram
        var textPresentation: INodePresentation? = null
        try {
            TransactionManager.beginTransaction()
            textPresentation = editor.createText(text, location)
            textPresentation.height = maxOf(textPresentation.height, height)
            textPresentation.width = maxOf(textPresentation.width, width)
            textPresentation.setProperty(FONT_COLOR, fontColor)
            if (backgroundColor !== null)
                textPresentation.setProperty(FILL_COLOR, backgroundColor)
            TransactionManager.endTransaction()
        } catch (e: InvalidEditingException) {
            e.printStackTrace()
            TransactionManager.abortTransaction()
        }
        return textPresentation
    }

    fun updateText(location: Point2D, width: Double, height: Double, text: String, fontColor: String,
                   textPresentation: INodePresentation, backgroundColor: String? = null) {
        try {
            TransactionManager.beginTransaction()
            textPresentation.label = text
            textPresentation.location = location
            textPresentation.height = maxOf(textPresentation.height, height)
            textPresentation.width = maxOf(textPresentation.width, width)
            textPresentation.setProperty(FONT_COLOR, fontColor)
            if (backgroundColor !== null)
                textPresentation.setProperty(FILL_COLOR, backgroundColor)
            TransactionManager.endTransaction()
        } catch (e: InvalidEditingException) {
            e.printStackTrace()
            TransactionManager.abortTransaction()
        }
    }

    fun createRectangle(location: Point2D, width: Double, height: Double, diagram: IDiagram): INodePresentation? {
        val editor = projectAccessor.diagramEditorFactory.classDiagramEditor
        editor.diagram = diagram
        var rectanglePresentation: INodePresentation? = null
        try {
            TransactionManager.beginTransaction()
            rectanglePresentation = editor.createRect(location, width, height)
            TransactionManager.endTransaction()
        } catch (e: InvalidEditingException) {
            e.printStackTrace()
            TransactionManager.abortTransaction()
        }
        return rectanglePresentation
    }

    fun updateRectangle(location: Point2D, width: Double, height: Double, iRect: INodePresentation) {
        try {
            TransactionManager.beginTransaction()
            iRect.location = location
            iRect.width = width
            iRect.height = height
            TransactionManager.endTransaction()
        } catch (e: InvalidEditingException) {
            e.printStackTrace()
            TransactionManager.abortTransaction()
        }
    }

    fun getAllPackages(): List<IPackage> {
        fun getAllPackages1(p: IPackage, packages: MutableList<IPackage>) {
            p.ownedElements.filterIsInstance<IPackage>().forEach { e ->
                packages.add(e)
                getAllPackages1(e, packages)
            }
        }
        val allPackages = mutableListOf<IPackage>(projectAccessor.project)
        getAllPackages1(projectAccessor.project, allPackages)
        return allPackages
    }

    fun getSetsOfClasses(): List<Pair<IClass, List<IClass>>> {
        val ret = mutableListOf<Pair<IClass, List<IClass>>>()
        getAllPackages().forEach { p ->
            p.ownedElements.filterIsInstance<IClass>().forEach { clazz ->
                val nestedClasses = clazz.nestedClasses
                if (nestedClasses.isNotEmpty()) {
                    val classes = mutableListOf<IClass>()
                    classes.addAll(nestedClasses)
                    ret.add(Pair(clazz, classes))
                }
            }
        }
        return ret
    }

    fun getSetsOfClassesUnderCommonParent(diagram: IDiagram):
            List<Pair<IClass,List<Pair<INodePresentation,IClass>>>> {
        val classPresentations = diagram.presentations.filter { presentation ->
            presentation is INodePresentation && presentation.model is IClass }.map { presentation ->
            Pair(presentation as INodePresentation, presentation.model as IClass) }
        val ret = mutableListOf<Pair<IClass, List<Pair<INodePresentation, IClass>>>>()
        getSetsOfClasses().filter { classes ->
            classPresentations.any { classes.second.contains(it.second) } }.forEach { classes ->
            val brotherOrSisterPresentations = mutableListOf<Pair<INodePresentation, IClass>>()
            brotherOrSisterPresentations.addAll(classPresentations.filter { classes.second.contains(it.second) })
            ret.add(Pair(classes.first, brotherOrSisterPresentations))
        }
        return ret
    }

    fun getCurrentDiagram(): IDiagram? =
            projectAccessor.viewManager.diagramViewManager.currentDiagram

    fun getInitialValue(model: IClass, valueName: String): String? =
            model.attributes.filter { it.association == null }.firstOrNull { it.name == valueName }?.initialValue
    fun getOrCreateInitialValue(model: IClass, valueName: String, undefinedValue: String): String {
        return getInitialValue(model, valueName) ?: run {
            try {
                TransactionManager.beginTransaction()
                val attribute = if (projectAccessor.astahEdition == "SysML")
                    projectAccessor.modelEditorFactory.sysmlModelEditor.createAttribute(model, valueName, "String")
                else
                    projectAccessor.modelEditorFactory.basicModelEditor.createAttribute(model, valueName, "Object")
                attribute.initialValue = undefinedValue
                TransactionManager.endTransaction()
            } catch (exp: Exception) {
                exp.printStackTrace()
                TransactionManager.abortTransaction()
            }
            undefinedValue
        }
    }
}