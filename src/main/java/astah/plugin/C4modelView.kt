package astah.plugin

import com.change_vision.jude.api.inf.project.ProjectEvent
import com.change_vision.jude.api.inf.project.ProjectEventListener
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView
import com.change_vision.jude.api.inf.ui.ISelectionListener
import java.awt.Component
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JPanel


class C4modelView : JPanel(), IPluginExtraTabView, ProjectEventListener, ActionListener {
    val buttonRefreshBasedOnModel = ButtonRefresh("Refresh (based on model)")
    val buttonRefreshBasedOnText = ButtonRefresh("Reflect c4model texts to the model")

    init {
        val panelButtons = JPanel()
        panelButtons.layout = GridLayout(1,2)
        panelButtons.add(buttonRefreshBasedOnModel)
        panelButtons.add(buttonRefreshBasedOnText)
        buttonRefreshBasedOnModel.addActionListener(this)
        buttonRefreshBasedOnText.addActionListener(this)
        add(panelButtons)
    }

    override fun actionPerformed(e: ActionEvent) {
        when (e.source) {
            buttonRefreshBasedOnModel -> {
                buttonRefreshBasedOnModel.push(AstahAccessor.getCurrentDiagram(), ReferenceSource.MODEL)
            }
            buttonRefreshBasedOnText -> {
                buttonRefreshBasedOnModel.push(AstahAccessor.getCurrentDiagram(), ReferenceSource.TEXT_NODE)
            }
        }
    }

    override fun getTitle(): String = "C4model"
    override fun getDescription(): String = "C4model View"
    override fun getComponent(): Component = this
    override fun addSelectionListener(p0: ISelectionListener?) {}
    override fun activated() {}
    override fun deactivated() {}
    override fun projectOpened(p0: ProjectEvent?) {}
    override fun projectClosed(p0: ProjectEvent?) {}
    override fun projectChanged(p0: ProjectEvent?) {}
}